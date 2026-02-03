package com.datacentric.timesense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.ProjectTask;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>, 
        JpaSpecificationExecutor<ProjectTask> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
            "FROM ProjectTask a " +
            "WHERE a.name = ?1 and a.deleted = false ")
     boolean existsByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE ProjectTask c " + 
             "SET c.deleted = true " + 
             "WHERE c.id = ?1 ")
     void deleteSystemSettingById(Long id);

    @Query("SELECT s FROM ProjectTask s WHERE s.deleted = false AND s.name = ?1")
     ProjectTask findByName(String name);

    @Query(value = "SELECT project_task_id FROM project_type_tasks WHERE project_type_id = ?1", 
         nativeQuery = true)
     List<Long> findTasksByProjectTypeId(Long projectTypeId);
    
}
