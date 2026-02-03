package com.datacentric.timesense.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.datacentric.timesense.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>,
        JpaSpecificationExecutor<Project> {

    @Query("SELECT p " +
        "FROM Project p " +
        "INNER JOIN TimeRecord t ON " +
        "       t.deleted = false AND t.user.id = ?2 AND t.project.id = p.id AND " +
        "                               (t.updatedAt >= ?1 OR t.createdAt >= ?1) " +
        "WHERE p.deleted = false ")
    List<Project> findUserTimeRecordsFromDate(Timestamp date, Long userId);

    @Query("SELECT p FROM Project p WHERE p.name = ?1 ")
    Optional<Project> findByCode(String code);

}
