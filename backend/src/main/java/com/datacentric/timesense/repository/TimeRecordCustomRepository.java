package com.datacentric.timesense.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.domain.Page;

public interface TimeRecordCustomRepository {
    Page<Object[]> getWeeklyProjectCostsWithUserFiltered(Long projectId,
            int firstRow, int numRows, String sort,
            List<Long> userId,Timestamp startDate, Timestamp endDate);

    Page<Object[]> getDailyProjectCosts(Long projectId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate, boolean export);

    Page<Object[]> getFilteredTimeRecords(List<Long> projectId, Long approverId,
            int firstRow, int numRows, String sort,
            List<Long> userId, Timestamp startDate, Timestamp endDate, boolean export);

    Page<Object[]> getMonthlyProjectCostsWithUserFiltered(Long projectId,
        int firstRow, int numRows, String sort,
        List<Long> userId,Timestamp startDate, Timestamp endDate);
}
