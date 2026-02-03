package com.datacentric.timesense.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;

import com.datacentric.timesense.model.Absence;
import com.datacentric.timesense.model.Status;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long>,
        JpaSpecificationExecutor<Absence> {

    @Query("SELECT a FROM Absence a WHERE ?1 BETWEEN a.startDate AND a.endDate ")            
    List<Absence> findAllAbsencesByDate(Timestamp date);

    @Query("SELECT a FROM Absence a WHERE a.id IN ?1 AND a.deleted = false ")
    List<Absence> findAllAbsencesById(List<Long> ids);

    @Query("SELECT a " + 
        "  FROM Absence a " +
        "  WHERE a.startDate <= ?2 AND a.endDate >= ?1 AND a.deleted = false " + 
        "       AND a.status.name != 'DENIED' ")
    List<Absence> getAllAbsencesByDate(Timestamp start, Timestamp end);

    @Cacheable(value = "absencesByDate", key = "#start.toString() + '-' + #end.toString()")
    @Query("SELECT a FROM Absence a " +
        "JOIN FETCH a.user u " +
        "JOIN FETCH a.status s " +
        "JOIN FETCH a.type t " +
        "WHERE a.startDate <= ?2 AND a.endDate >= ?1 " +
        "AND a.deleted = false " +
        "AND s.name != 'DENIED' " +
        "AND (?3 IS NULL OR u.id IN ?3) " +
        "AND (?4 IS NULL OR s.id IN ?4) " +
        "AND (?5 IS NULL OR t.id IN ?5) " +
        "AND (?6 IS NULL OR a.businessYear IN ?6)")
    List<Absence> getOptimizedAbsencesByDateWithFilters(
        Timestamp start, 
        Timestamp end, 
        List<Long> userFilter, 
        List<Long> statusFilter, 
        List<Long> typeFilter, 
        List<String> businessYearFilter);

    @Query("SELECT a " + 
        "FROM Absence a " +
        "WHERE a.endDate < ?1 AND a.status.name IN ('PENDING', 'APPROVED')")
    List<Absence> getPassedAbsences(Timestamp date);

    @Query("SELECT a FROM Absence a WHERE a.deleted = false " + 
            " AND a.endDate >= ?1 AND a.startDate <= ?2 ")
    List<Absence> getAbsencesFromDate(Timestamp start, Timestamp end);

    @Modifying
    @Query("UPDATE Absence a SET status = ?1, observations = ?2, approvedBy.id = ?4, " +
            " approvedDate = CURRENT_TIMESTAMP WHERE a.id IN ?3 ")
    int updateAbsencesStatus(Status status, String reason, List<Long> ids, Long approverId);

}
