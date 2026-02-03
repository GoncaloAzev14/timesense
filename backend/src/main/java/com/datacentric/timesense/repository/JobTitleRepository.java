package com.datacentric.timesense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.JobTitle;

@Repository
public interface JobTitleRepository extends JpaRepository<JobTitle, Long>, 
        JpaSpecificationExecutor<JobTitle> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
           "FROM JobTitle a " +
           "WHERE a.name = ?1 and a.deleted = false ")
    boolean existsByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE JobTitle j " + 
            "SET j.deleted = true " + 
            "WHERE j.id = ?1 ")
    void deleteJobTitleById(Long id);
}
