package com.datacentric.timesense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.SystemSetting;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long>, 
        JpaSpecificationExecutor<SystemSetting> {
    
    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN true ELSE false END " + 
            "FROM SystemSetting a " +
            "WHERE a.name = ?1 and a.deleted = false ")
     boolean existsByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE SystemSetting c " + 
             "SET c.deleted = true " + 
             "WHERE c.id = ?1 ")
     void deleteSystemSettingById(Long id);

    @Query("SELECT s FROM SystemSetting s WHERE s.deleted = false AND s.name = ?1")
     SystemSetting findByName(String name);
    
}
