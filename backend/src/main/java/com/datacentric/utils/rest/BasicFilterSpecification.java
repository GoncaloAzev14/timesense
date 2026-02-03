package com.datacentric.utils.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Specification that filters records based on the values of the specified
 * fields.
 * The filter is specified as a comma separated list of field=value pairs.
 * The filter is applied as an AND condition to all specified field=value pairs.
 * The filter is applied as an EQUAL condition to each field=value pair.
 * The filter is applied as a LIKE condition to each field~value pair.
 */
public class BasicFilterSpecification<T> implements Specification<T> {

    private static final Logger log = LoggerFactory.getLogger(BasicFilterSpecification.class);

    private static final Pattern FILTER_PARSER_REGEX = 
            Pattern.compile("[^=~!,]+(?:!=|!~|=|~)(?:\\([^)]*\\)|[^,]+)");

    private static final String ANY_CHAR_GLOB = "%";

    private static final String EQUAL = "=";
    private static final String DIFFERENT = "!=";
    private static final String LIKE = "~";
    private static final String NOT_LIKE = "!~";
    private static final String IN_DELIM_STR = ",";
    private static final String INITIAL_LIST_CHAR = "(";
    private static final String END_LIST_CHAR = ")";

    private String[] filterFields;
    private String[] filterOperators; // Now full string operators (=, ~, !=, !~)
    private String[] filterValues;

    public BasicFilterSpecification(String filter) {
        List<String> filterArray = splitFiltersRegex(filter);
        filterOperators = new String[filterArray.size()];
        filterFields = new String[filterArray.size()];
        filterValues = new String[filterArray.size()];

        for (int i = 0; i < filterArray.size(); i++) {
            String expression = filterArray.get(i);

            // Match operators: =, ~, !=, !~
            String operator = null;
            if (expression.contains(DIFFERENT)) {
                operator = DIFFERENT;
            } else if (expression.contains(NOT_LIKE)) {
                operator = NOT_LIKE;
            } else if (expression.contains(EQUAL)) {
                operator = EQUAL;
            } else if (expression.contains(LIKE)) {
                operator = LIKE;
            } else {
                throw new IllegalArgumentException("Invalid filter format: " + expression);
            }

            int opIndex = expression.indexOf(operator);
            filterFields[i] = expression.substring(0, opIndex);
            filterOperators[i] = operator;
            filterValues[i] = expression.substring(opIndex + operator.length());
        }
    }

    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Predicate[] allPredicates = new Predicate[filterFields.length];
        for (int i = 0; i < filterFields.length; i++) {
            String filterField = filterFields[i];
            String filterValue = filterValues[i];
            String operator = filterOperators[i];
            Expression<?> filterExpression;

            if (filterField.contains(".")) {
                String[] fieldParts = filterField.split("\\.");
                Path<?> path = root;
                for (String pathPart : fieldParts) {
                    path = path.get(pathPart);
                }
                filterExpression = path;
            } else {
                filterExpression = root.get(filterField);
            }

            switch (operator) {
                case EQUAL:
                    if (filterValue.startsWith(INITIAL_LIST_CHAR) 
                            && filterValue.endsWith(END_LIST_CHAR)) {
                        String inner = filterValue.substring(1, filterValue.length() - 1);
                        String[] values = inner.split(IN_DELIM_STR);
                        allPredicates[i] = createTypeSafeInPredicate(builder, values, 
                                filterExpression);
                    } else {
                        allPredicates[i] = createTypeSafeEqualityPredicate(builder, filterValue,
                                filterExpression);
                    }
                    break;
                case DIFFERENT:
                    if (filterValue.startsWith(INITIAL_LIST_CHAR) 
                            && filterValue.endsWith(END_LIST_CHAR)) {
                        String inner = filterValue.substring(1, filterValue.length() - 1);
                        String[] values = inner.split(IN_DELIM_STR);
                        allPredicates[i] = builder.not(createTypeSafeInPredicate(builder, values, 
                                filterExpression));
                    } else {
                        allPredicates[i] = builder.not(createTypeSafeEqualityPredicate(builder, 
                                filterValue, filterExpression));
                    }
                    break;
                case LIKE:
                    allPredicates[i] = builder.like((Expression<String>) filterExpression,
                            ANY_CHAR_GLOB + filterValue + ANY_CHAR_GLOB);
                    break;
                case NOT_LIKE:
                    allPredicates[i] = builder.not(builder.like((Expression<String>) 
                            filterExpression, ANY_CHAR_GLOB + filterValue + ANY_CHAR_GLOB));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid filter operator: " + operator);
            }
        }
        return builder.and(allPredicates);
    }

    private Predicate createTypeSafeEqualityPredicate(CriteriaBuilder builder,
                String filterValue, Expression<?> filterExpression) {
        Class<?> javaType = filterExpression.getJavaType();
        if (javaType == String.class) {
            return builder.equal(filterExpression, filterValue);
        }
        if (javaType == Long.class) {
            return builder.equal(filterExpression, Long.valueOf(filterValue));
        }
        if (javaType == Integer.class) {
            return builder.equal(filterExpression, Integer.valueOf(filterValue));
        }
        if (javaType == Boolean.class) {
            return builder.equal(filterExpression, Boolean.valueOf(filterValue));
        }
        if (javaType == Double.class) {
            return builder.equal(filterExpression, Double.valueOf(filterValue));
        }
        throw new IllegalArgumentException("Unsupported filter field type: " + javaType);
    }

    private Predicate createTypeSafeInPredicate(CriteriaBuilder builder, String[] filterValues,
        Expression<?> filterExpression) {

        Class<?> javaType = filterExpression.getJavaType();
        CriteriaBuilder.In<Object> inClause = builder.in(filterExpression);

        for (String rawValue : filterValues) {
            String value = rawValue.trim();
            if (javaType == String.class) {
                inClause.value(value);
            } else if (javaType == Long.class) {
                inClause.value(Long.valueOf(value));
            } else if (javaType == Integer.class) {
                inClause.value(Integer.valueOf(value));
            } else if (javaType == Boolean.class) {
                inClause.value(Boolean.valueOf(value));
            } else if (javaType == Double.class) {
                inClause.value(Double.valueOf(value));
            } else {
                throw new IllegalArgumentException("Unsupported filter field type fon IN" + 
                    " operation: " + javaType);
            }
        }
        return inClause;
    }

    public static <T> Specification<T> applyFilterSpec(List<Long> filterList, String columnName, 
            String foreignColumnName) {
        return (root, query, cb) -> {
            if (filterList == null || filterList.isEmpty()) {
                return null;
            }

            if (foreignColumnName != null && !foreignColumnName.isEmpty()) {
                return root.get(columnName).get(foreignColumnName).in(filterList);
            } else {
                return root.get(columnName).in(filterList);
            }
        };
    }

    private List<String> splitFiltersRegex(String filter) {
        /*
         * reg exp ->   [^=,]+(?:!=|=)(?:\\([^)]*\\)|[^,]+)
         *   1.  [^=,]+      -> match the field name stopping before "=" or "," 
         *   2.  (?:!=|=)    -> ensure we’re parsing field(= or !=)value format.
         *   3.  (?: ... )   -> non-capturing group, groups alternatives separeted by |
         *   A:  \([^)]*\)   -> the char "(" then captures any char thats not ")" then the char ")"
         *   B:  [^,]+       -> matches one or more characters that are not ","
         */
        
        Matcher matcher = FILTER_PARSER_REGEX.matcher(filter);

        List<String> filters = new ArrayList<>();
        while (matcher.find()) {
            filters.add(matcher.group());
        }
        return filters;
    }
}
