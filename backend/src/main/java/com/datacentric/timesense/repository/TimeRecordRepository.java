package com.datacentric.timesense.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.TimeRecord;
import com.datacentric.timesense.model.User;

@Repository
public interface TimeRecordRepository extends JpaRepository<TimeRecord, Long>,
        JpaSpecificationExecutor<TimeRecord>, TimeRecordCustomRepository {

    @Query("SELECT t FROM TimeRecord t WHERE t.user.id = ?1 " +
        "   AND t.startDate >= ?2 AND t.startDate <= ?3 " +
        "   AND t.deleted = false")
    List<TimeRecord> getTimeRecordsByUserAndDate(Long userId, Timestamp startDate,
                Timestamp endDate);

    @Query("SELECT t FROM TimeRecord t WHERE t.user.id = :userId " +
            " AND t.startDate >= :startDate AND t.deleted = false")
    Page<TimeRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId, @Param("startDate") Timestamp startDate,
            Pageable pageable
    );

    @Query("SELECT t FROM TimeRecord t WHERE t.user.id = :userId " +
            "AND t.project.id IN :projectIds " +
            "AND t.task.id IN :taskIds " +
            "AND t.startDate IN :dates " +
            "AND t.deleted = false")
    List<TimeRecord> findByUserAndProjectsAndTasksAndDates(
        @Param("userId") Long userId,
        @Param("projectIds") List<Long> projectIds,
        @Param("taskIds") List<Long> taskIds,
        @Param("dates") Set<Timestamp> dates);

    @Query("SELECT t " +
            "FROM TimeRecord t " +
            "WHERE t.startDate >= ?1 AND t.endDate <= ?2 " +
            "AND t.project.id = ?3 AND t.deleted = false " )
    List<TimeRecord> getTimeRecordsFromDateInterval(Timestamp startDate, Timestamp endDate,
        Long projectId);

    @Query(value = " SELECT " +
                    "  DATE_TRUNC('week', t.start_date) AS week_start, " +
                    "  SUM(t.hours) AS total_hours, " +
                    "  SUM(t.hours * j.rate) AS total_cost " +
                    "FROM time_records t " +
                    "INNER JOIN users u ON u.id = t.user_id " +
                    "INNER JOIN job_titles j ON j.id = u.job_title " +
                    "INNER JOIN projects p on p.id = t.project_id " +
                    "INNER JOIN status s on s.id = t.status_id " +
                    "WHERE p.id = ?1 " +
                    "AND s.name <> 'DRAFT' " +
                    "GROUP BY " +
                    "  DATE_TRUNC('week', t.start_date) " +
                    "ORDER BY week_start ",
        nativeQuery = true)
    List<Object[]> getWeeklyProjectCosts(Long projectId);

    @Query(value = " SELECT " +
                    "  t.user_id, " +
                    "  DATE_TRUNC('week', t.start_date) AS week_start, " +
                    "  SUM(t.hours) AS total_hours, " +
                    "  SUM(t.hours * j.rate) AS total_cost " +
                    "FROM time_records t " +
                    "INNER JOIN users u ON u.id = t.user_id " +
                    "INNER JOIN job_titles j ON j.id = u.job_title " +
                    "INNER JOIN projects p on p.id = t.project_id " +
                    "INNER JOIN status s on s.id = t.status_id " +
                    "WHERE p.id = ?1 " +
                    "AND s.name <> 'DRAFT' " +
                    "GROUP BY " +
                    "  DATE_TRUNC('week', t.start_date), " +
                    "  t.user_id " +
                    "ORDER BY week_start ",
        nativeQuery = true)
    List<Object[]> getWeeklyProjectCostsWithUser(Long projectId);

    @Modifying
    @Query("UPDATE TimeRecord t SET t.status = ?1, reason = ?2, approvedBy = ?4, " +
            " approvedAt = CURRENT_TIMESTAMP  WHERE t.id in ?3")
    int updateTimeRecordStatus(Status status, String reason, List<Long> ids, User approvedBy);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimeRecord t WHERE t.hours = ?1 ")
    void deleteByHourValue(Double hour);

    @Query("""
                SELECT COUNT(t) > 0 
                FROM TimeRecord t 
                WHERE t.user.id = :userId
                AND t.project.id = :projectId
                AND t.task.id = :taskId
                AND t.startDate = :startDate
                AND t.id <> :id
                AND t.deleted = false
                        """)
        boolean existsDuplicateCombination(
        @Param("userId") Long userId,
        @Param("projectId") Long projectId,
        @Param("taskId") Long taskId,
        @Param("startDate") Timestamp startDate,
        @Param("id") Long id
        );

}
