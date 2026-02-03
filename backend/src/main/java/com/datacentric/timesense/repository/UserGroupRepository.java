package com.datacentric.timesense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.UserGroup;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long>, 
    JpaSpecificationExecutor<UserGroup> {

    List<UserGroup> findByTokenIdIn(List<String> tokenIds);

    @Modifying
    @Transactional
    @Query("UPDATE UserGroup ug " + 
            "SET ug.deleted = true , ug.updatedBy.id = ?2 , ug.updatedAt = CURRENT_TIMESTAMP " + 
            "WHERE ug.id = ?1 ")
    void deleteUserGroupById(Long userGroupId, Long userId);

}
