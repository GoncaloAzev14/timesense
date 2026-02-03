package com.datacentric.timesense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.datacentric.timesense.model.ProjectAssignment;



@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long>,
        JpaSpecificationExecutor<ProjectAssignment> {

    @Query("SELECT pa " +
           " FROM ProjectAssignment pa " + 
           " WHERE pa.project.id = ?1 AND pa.deleted = false ")
    List<ProjectAssignment> getProjectAssignments(Long projectId);
}
