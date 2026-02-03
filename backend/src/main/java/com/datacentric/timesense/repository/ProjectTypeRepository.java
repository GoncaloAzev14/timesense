package com.datacentric.timesense.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.ProjectType;

@Repository
public interface ProjectTypeRepository extends JpaRepository<ProjectType, Long>, 
        JpaSpecificationExecutor<ProjectType> {
    
    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
           "FROM ProjectType a " +
           "WHERE a.name = ?1 and a.deleted = false ")
    boolean existsByName(String name);

    @Query("SELECT t " +
           "FROM ProjectType t WHERE t.name = ?1 ")
    Optional<ProjectType> findByName(String name);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Project a WHERE a.type.id = :typeId")
    boolean existsInProjectByTypeId(@Param("typeId") Long typeId);

    @Modifying
    @Transactional
    @Query("UPDATE ProjectType c " + 
            "SET c.deleted = true " + 
            "WHERE c.id = ?1 ")
    void deleteProjectTypeById(Long typeId);
}
