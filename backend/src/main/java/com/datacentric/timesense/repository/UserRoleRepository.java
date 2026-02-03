package com.datacentric.timesense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long>, 
    JpaSpecificationExecutor<UserRole> {

    @Query("SELECT r from UserRole r WHERE r.name = ?1 AND r.deleted = false ")    
    UserRole findByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE UserRole ur " + 
            "SET ur.deleted = true , ur.updatedBy.id = ?2 , ur.updatedAt = CURRENT_TIMESTAMP " + 
            "WHERE ur.id = ?1 ")
    void deleteUserRoleById(Long userRoleId, Long userId);
}
