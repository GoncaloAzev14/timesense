package com.datacentric.timesense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.datacentric.timesense.model.AbsenceSubType;

@Repository
public interface AbsenceSubTypeRepository extends JpaRepository<AbsenceSubType, Long>, 
        JpaSpecificationExecutor<AbsenceSubType> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
            "FROM AbsenceSubType a " +
            "WHERE a.name = ?1 and a.deleted = false ")
    boolean existsByName(String name);
    
}
