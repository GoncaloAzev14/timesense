package com.datacentric.utils.rest;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Collection of utility methods to simplify the implementation of requests
 * in the REST controllers.
 **/
public class RestUtils {

    private static final String COMMA_SEPARATOR = ",";

    /**
     * Converts a string of comma separated sort criteria to a Spring Sort object.
     * A descending sorting order is indicated by a '-' prefix to the field name.
     *
     * @param sortCriteria a string of comma separated sort criteria
     * @return a Spring Sort object
     */
    public static Sort getSortFromString(String sortCriteria) {
        String[] sortCriteriaArray = sortCriteria.split(COMMA_SEPARATOR);
        Sort.Order[] orders = new Sort.Order[sortCriteriaArray.length];

        int targetIdx = 0;
        for (int i = 0; i < sortCriteriaArray.length; i++) {
            String fieldName = sortCriteriaArray[i].trim();
            if (fieldName.length() == 0) {
                continue;
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (fieldName.charAt(0) == '-') {
                direction = Sort.Direction.DESC;
                fieldName = fieldName.substring(1);
            }

            orders[targetIdx] = new Sort.Order(direction, fieldName);
            targetIdx++;
        }
        return Sort.by(Arrays.copyOf(orders, targetIdx));
    }

    /**
     * Converts a string of comma separated expand options to a list of strings.
     *
     * @param expandOptions a string of comma separated expand options
     * @return a list of strings each with a field name
     */
    public static List<String> getExpandOptionsFromString(String expandOptions) {
        if (expandOptions == null) {
            return List.of();
        }
        return Arrays.asList(expandOptions.split(COMMA_SEPARATOR));
    }

    private RestUtils() {
    }

    /**
     * Converts a filter string to a JPA Specification object.
     *
     * @param filter a string with filter criteria
     * @return a JPA Specification object
     */
    public static <T> Specification<T> getSpecificationFromFilter(String filterFormat,
            String filter) {

        switch (filterFormat) {
            case "basic":
                return new BasicFilterSpecification<>(filter);
            default:
                throw new IllegalArgumentException("Unsupported filter format: " + filterFormat);
        }
    }
}
