package com.datacentric.timesense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Status;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long>, 
        JpaSpecificationExecutor<Status> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
            "FROM Status a " +
            "WHERE a.name = ?1 and a.deleted = false ")
     boolean existsByName(String name);

    @Query("SELECT s FROM Status s WHERE s.deleted = false and s.name = ?1")
     Status findByName(String name);
 
    @Modifying
    @Transactional
    @Query("UPDATE Status c " + 
             "SET c.deleted = true " + 
             "WHERE c.id = ?1 ")
     void deleteStatusById(Long typeId);
    
}
