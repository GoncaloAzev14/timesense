package com.datacentric.timesense.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

public class TimeRecordCustomRepositoryImpl implements TimeRecordCustomRepository {

    private static final Logger logger =
        LoggerFactory.getLogger(TimeRecordCustomRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Object[]> getWeeklyProjectCostsWithUserFiltered(Long projectId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate) {

        int page = (firstRow != 0) ? (firstRow / numRows) : 0;

        // CHECKSTYLE.OFF: MultipleStringLiterals
        StringBuilder query = new StringBuilder();
        query.append("SELECT t.user_id, ")
             .append("DATE_TRUNC('week', t.start_date) AS week_start, ")
             .append("SUM(t.hours) AS total_hours, ")
             .append("SUM(t.hours * j.rate) AS total_cost ")
             .append("FROM time_records t ")
             .append("INNER JOIN users u ON u.id = t.user_id ")
             .append("INNER JOIN projects p ON p.id = t.project_id ")
             .append("INNER JOIN status s ON s.id = t.status_id ")
             .append("LEFT OUTER JOIN job_titles j ON j.id = u.job_title ")
             .append("WHERE p.id = :projectId ")
             .append("AND s.name <> 'DRAFT' ");

        if (userId != null && !userId.isEmpty()) {
            query.append("AND u.id in ( :userId ) ");
        }

        if (startDate != null) {
            query.append("AND t.start_date >= :startDate ");
        }

        if (endDate != null) {
            query.append("AND t.end_date <= :endDate ");
        }

        query.append("GROUP BY DATE_TRUNC('week', t.start_date), t.user_id ")
             .append("ORDER BY DATE_TRUNC('week', t.start_date) DESC ");

        String baseQueryStr = query.toString();
        String countQueryStr = "SELECT COUNT(*) FROM (" + baseQueryStr + ") AS count_query";

        Query dataQuery = entityManager.createNativeQuery(baseQueryStr);
        Query countQuery = entityManager.createNativeQuery(countQueryStr);

        dataQuery.setParameter("projectId", projectId);
        countQuery.setParameter("projectId", projectId);

        if (userId != null && !userId.isEmpty()) {
            dataQuery.setParameter("userId", userId);
            countQuery.setParameter("userId", userId);
        }

        if (startDate != null) {
            dataQuery.setParameter("startDate", startDate);
            countQuery.setParameter("startDate", startDate);
        }

        if (endDate != null) {
            dataQuery.setParameter("endDate", endDate);
            countQuery.setParameter("endDate", endDate);
        }

        dataQuery.setFirstResult(firstRow);
        dataQuery.setMaxResults(numRows);

        @SuppressWarnings("unchecked")
        List<Object[]> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, numRows, Sort.by("week_start")),
                totalElements);

    }

    @Override
    public Page<Object[]> getDailyProjectCosts(Long projectId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate, boolean export) {

        int page = (firstRow != 0) ? (firstRow / numRows) : 0;

        // Default sort field fallback
        String sortColumn = switch (sort) {
            case "name" -> "u.name";
            case "task" -> "pt.name";
            case "description" -> "t.description";
            case "hours" -> "t.hours";
            case "total_cost" -> "total_cost";
            default -> "t.start_date";
        };

        StringBuilder query = new StringBuilder();
        query.append("SELECT u.name, ")
             .append("pt.name, ")
             .append("t.description, ")
             .append("t.hours, ")
             .append("t.hours * j.rate AS total_cost, ")
             .append("t.start_date ")
             .append("FROM time_records t ")
             .append("INNER JOIN users u ON u.id = t.user_id ")
             .append("INNER JOIN projects p ON p.id = t.project_id ")
             .append("INNER JOIN status s ON s.id = t.status_id ")
             .append("INNER JOIN tasks pt ON pt.id = t.task_id ")
             .append("LEFT OUTER JOIN job_titles j ON j.id = u.job_title ")
             .append("WHERE p.id = :projectId ")
             .append("AND s.name <> 'DRAFT' ");

        if (userId != null && !userId.isEmpty()) {
            query.append("AND u.id in ( :userId ) ");
        }

        if (startDate != null) {
            query.append("AND t.start_date >= :startDate ");
        }

        if (endDate != null) {
            query.append("AND t.end_date <= :endDate ");
        }

        String baseQuery = query.toString();
        String dataQueryStr = baseQuery + " ORDER BY " + sortColumn + " DESC";
        String countQueryStr = "SELECT COUNT(*) FROM (" + baseQuery + ") AS count_query";

        Query dataQuery = entityManager.createNativeQuery(dataQueryStr);
        Query countQuery = entityManager.createNativeQuery(countQueryStr);

        dataQuery.setParameter("projectId", projectId);
        countQuery.setParameter("projectId", projectId);

        if (userId != null && !userId.isEmpty()) {
            dataQuery.setParameter("userId", userId);
            countQuery.setParameter("userId", userId);
        }

        if (startDate != null) {
            dataQuery.setParameter("startDate", startDate);
            countQuery.setParameter("startDate", startDate);
        }

        if (endDate != null) {
            dataQuery.setParameter("endDate", endDate);
            countQuery.setParameter("endDate", endDate);
        }

        if (!export) {
            // Apply pagination
            dataQuery.setFirstResult(firstRow);
            dataQuery.setMaxResults(numRows);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, numRows, Sort.by(sortColumn)),
                totalElements);
    }

    @Override
    public Page<Object[]> getFilteredTimeRecords(List<Long> projectId, Long approverId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate, boolean export) {

        int page = (firstRow != 0) ? (firstRow / numRows) : 0;

        if (sort == null || sort.isBlank()) {
            sort = "t.start_date";
        }

        String[] sortParts = sort.split(",");
        List<String> orderByClauses = new ArrayList<>();

        for (String s : sortParts) {
            s = s.trim();
            boolean descending = s.startsWith("-");
            String sortKey = descending ? s.substring(1) : s;

            String sortColumn = switch (sortKey) {
                case "id" -> "t.id";
                case "projectName" -> "p.name";
                case "userName" -> "u.name";
                case "taskName" -> "pt.name";
                case "description" -> "t.description";
                case "hours" -> "t.hours";
                case "startDate" -> "t.start_date";
                case "endDate" -> "t.end_date";
                default -> "t.start_date";
            };

            orderByClauses.add(sortColumn + (descending ? " DESC" : " ASC"));
        }

        if (orderByClauses.isEmpty()) {
            orderByClauses.add("t.start_date DESC");
        }
        String orderBy = String.join(", ", orderByClauses);

        StringBuilder query = new StringBuilder();
        query.append("SELECT u.name         AS userName, ")
             .append("p.name                AS projectCode, ")
             .append("p.description         AS projectName, ")
             .append("pt.name               AS taskName, ")
             .append("t.description         AS description, ")
             .append("t.hours               AS hours, ")
             .append("t.start_date          AS startDate, ")
             .append("t.end_date            AS endDate, ")
             .append("s.name                AS statusName, ")
             .append("t.id                  AS id ")
             .append("FROM time_records t ")
             .append("INNER JOIN users u ON u.id = t.user_id ")
             .append("INNER JOIN projects p ON p.id = t.project_id ")
             .append("INNER JOIN status s ON s.id = t.status_id ")
             .append("INNER JOIN tasks pt ON pt.id = t.task_id ")
             .append("LEFT OUTER JOIN job_titles j ON j.id = u.job_title ")
             .append("WHERE s.name = 'APPROVED' ");

        if (projectId != null && !projectId.isEmpty()) {
            query.append("AND p.id in ( :projectId ) ");
        }

        if (userId != null && !userId.isEmpty()) {
            logger.debug("Filtering by the users: {}", userId);
            query.append("AND u.id in ( :userId ) ");
        }

        if (approverId != null) {
            logger.debug("Filtering by approverId: {}", approverId);
            query.append("AND p.manager = :approverId ");
        }

        if (startDate != null) {
            query.append("AND t.start_date >= :startDate ");
        }

        if (endDate != null) {
            query.append("AND t.end_date <= :endDate ");
        }

        String baseQuery = query.toString();

        // Apply sorting logic
        String dataQueryStr;
        if (export) {
            dataQueryStr = baseQuery + " ORDER BY " + orderBy + ", p.name ASC";
        } else {
            dataQueryStr = baseQuery + " ORDER BY " + orderBy;
        }
        String countQueryStr = "SELECT COUNT(*) FROM (" + baseQuery + ") AS count_query";

        logger.debug("Data Query: {}", dataQueryStr);
        logger.debug("Count Query: {}", countQueryStr);

        Query dataQuery = entityManager.createNativeQuery(dataQueryStr);
        Query countQuery = entityManager.createNativeQuery(countQueryStr);

        if (projectId != null && !projectId.isEmpty()) {
            dataQuery.setParameter("projectId", projectId);
            countQuery.setParameter("projectId", projectId);
        }

        if (approverId != null) {
            dataQuery.setParameter("approverId", approverId);
            countQuery.setParameter("approverId", approverId);
        }

        if (userId != null && !userId.isEmpty()) {
            dataQuery.setParameter("userId", userId);
            countQuery.setParameter("userId", userId);
        }

        if (startDate != null) {
            dataQuery.setParameter("startDate", startDate);
            countQuery.setParameter("startDate", startDate);
        }

        if (endDate != null) {
            dataQuery.setParameter("endDate", endDate);
            countQuery.setParameter("endDate", endDate);
        }

        if (!export) {
            // Apply pagination
            dataQuery.setFirstResult(firstRow);
            dataQuery.setMaxResults(numRows);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        List<Sort.Order> sortOrders = orderByClauses.stream()
            .map(clause -> {
                boolean desc = clause.endsWith("DESC");
                String col = clause.split(" ")[0];
                return new Sort.Order(desc ? Sort.Direction.DESC : Sort.Direction.ASC, col);
            })
            .toList();
        return new PageImpl<>(
            content,
            PageRequest.of(page, numRows, Sort.by(sortOrders)),
            totalElements
        );
    }

    @Override
    public Page<Object[]> getMonthlyProjectCostsWithUserFiltered(Long projectId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate) {

        int page = (firstRow != 0) ? (firstRow / numRows) : 0;

        // CHECKSTYLE.OFF: MultipleStringLiterals
        StringBuilder query = new StringBuilder();
        query.append("SELECT t.user_id, ")
             .append("DATE_TRUNC('month', t.start_date) AS month_start, ")
             .append("SUM(t.hours) AS total_hours, ")
             .append("SUM(t.hours * j.rate) AS total_cost ")
             .append("FROM time_records t ")
             .append("INNER JOIN users u ON u.id = t.user_id ")
             .append("INNER JOIN projects p ON p.id = t.project_id ")
             .append("INNER JOIN status s ON s.id = t.status_id ")
             .append("LEFT OUTER JOIN job_titles j ON j.id = u.job_title ")
             .append("WHERE p.id = :projectId ")
             .append("AND s.name <> 'DRAFT' ");

        if (userId != null && !userId.isEmpty()) {
            query.append("AND u.id in ( :userId ) ");
        }

        if (startDate != null) {
            query.append("AND t.start_date >= :startDate ");
        }

        if (endDate != null) {
            query.append("AND t.end_date <= :endDate ");
        }

        query.append("GROUP BY DATE_TRUNC('month', t.start_date), t.user_id ")
             .append("ORDER BY DATE_TRUNC('month', t.start_date) DESC ");

        String baseQueryStr = query.toString();
        String countQueryStr = "SELECT COUNT(*) FROM (" + baseQueryStr + ") AS count_query";

        Query dataQuery = entityManager.createNativeQuery(baseQueryStr);
        Query countQuery = entityManager.createNativeQuery(countQueryStr);

        dataQuery.setParameter("projectId", projectId);
        countQuery.setParameter("projectId", projectId);

        if (userId != null && !userId.isEmpty()) {
            dataQuery.setParameter("userId", userId);
            countQuery.setParameter("userId", userId);
        }

        if (startDate != null) {
            dataQuery.setParameter("startDate", startDate);
            countQuery.setParameter("startDate", startDate);
        }

        if (endDate != null) {
            dataQuery.setParameter("endDate", endDate);
            countQuery.setParameter("endDate", endDate);
        }

        dataQuery.setFirstResult(firstRow);
        dataQuery.setMaxResults(numRows);

        @SuppressWarnings("unchecked")
        List<Object[]> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, numRows, Sort.by("month_start")),
                totalElements);

    }
}
