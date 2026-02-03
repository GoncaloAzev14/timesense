package com.datacentric.timesense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.ResourcePermission;

@Repository
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, Long>, 
        JpaSpecificationExecutor<ResourcePermission> {

    List<ResourcePermission> findByResourceTypeAndResourceId(String resourceType, Long resourceId);

    @Transactional
    void deleteByResourceTypeAndResourceId(String resourceType, Long resourceId);

    @Query("select p.resourceType resourceType, p.resourceId resourceId, " +
            "p.accessType accessType, p.subjectType subjectType, p.subject as subjectId, " +
            "case when subjectType='role' then r.name " +
            "when subjectType='group' then g.name " +
            "when subjectType='user' then u.name end as subjectName " +
            "from ResourcePermission p " +
            "left join UserRole r on p.subjectType = 'role' and p.subject=r.id " +
            "left join UserGroup g on p.subjectType = 'group' and p.subject=g.id " +
            "left join User u on p.subjectType = 'user' and p.subject=u.id " +
            "where p.resourceType = ?1 and p.resourceId = ?2")
    List<ResourcePermissionWithSubjectName> findByResourceTypeAndResourceIdWithSubjectName(
            String resourceType, Long resourceId);

    @Query("with all_roles as ( " +
            "    select r.id id, r.parentRole parent_id from UserRole r where id in ?2 " +
            "    or id in (select r.id from UserGroup g inner join g.roles r where g.id in ?3) " +
            "    union all " +
            "    select r.id, r.parentRole from all_roles ar " +
            "        inner join UserRole r on r.id = ar.parent_id.id " +
            ") " +
            "select p " +
            "from ResourcePermission p " +
            "where ((p.subjectType = 'user' and p.subject = ?1) " +
            "or (p.subjectType = 'role' and p.subject in (select id from all_roles)) " +
            "or (p.subjectType = 'group' and p.subject in ?3))")
    List<ResourcePermission> getUserPermissions(Long userId, List<Long> userRoles,
            List<Long> userGroups);

    @Query("with all_roles as ( " +
            "    select r.id id, r.parentRole parent_id from UserRole r where id in ?5 " +
            "    or id in (select r.id from UserGroup g inner join g.roles r where g.id in ?6) " +
            "    union all " +
            "    select r.id, r.parentRole from all_roles ar " +
            "        inner join UserRole r on r.id = ar.parent_id.id " +
            ") " +
            "select p " +
            "from ResourcePermission p " +
            "where p.resourceType = ?1 and p.resourceId = ?2 and p.accessType in ?3 " +
            "and ((p.subjectType = 'user' and p.subject = ?4) " +
            "or (p.subjectType = 'role' and p.subject in (select id from all_roles)) " +
            "or (p.subjectType = 'group' and p.subject in ?6))")
    List<ResourcePermission> getResourcePermission(String resourceType, Long resourceId,
            List<String> accessType, Long userId, List<Long> userRoles, List<Long> userGroups);

    @Query("with all_roles as ( " +
            "    select r.id id, r.parentRole parent_id from UserRole r where id in ?5 " +
            "    or id in (select r.id from UserGroup g inner join g.roles r where g.id in ?6) " +
            "    union all " +
            "    select r.id, r.parentRole from all_roles ar " +
            "        inner join UserRole r on r.id = ar.parent_id.id " +
            ") " +
            "select distinct p.resourceId " +
            "from ResourcePermission p " +
            "where p.resourceType = ?1 and p.resourceId in ?2 and p.accessType in ?3 " +
            "and ((p.subjectType = 'user' and p.subject = ?4) " +
            "or (p.subjectType = 'role' and p.subject in (select id from all_roles)) " +
            "or (p.subjectType = 'group' and p.subject in ?6))")
    List<Long> allowedResourcesList(String resourceType, List<Long> resourceIds,
            List<String> accessTypes, Long userId, List<Long> userRoles, List<Long> userGroups);

    @Query("with all_roles as ( " +
            "    select r.id id, r.parentRole parent_id from UserRole r where id in ?4 " +
            "    or id in (select r.id from UserGroup g inner join g.roles r where g.id in ?5) " +
            "    union all " +
            "    select r.id, r.parentRole from all_roles ar " +
            "        inner join UserRole r on r.id = ar.parent_id.id " +
            ") " +
            "select p.accessType " +
            "from ResourcePermission p " +
            "where p.resourceType = ?1 and p.resourceId = ?2 " +
            "and ((p.subjectType = 'user' and p.subject = ?3) " +
            "or (p.subjectType = 'role' and p.subject in (select id from all_roles)) " +
            "or (p.subjectType = 'group' and p.subject in ?5))")
    List<String> getAllowedPermissions(String resourceType, Long resourceId,
            Long userId, List<Long> userRoles, List<Long> userGroups);

    interface ResourcePermissionWithSubjectName {
        String getResourceType();

        Long getResourceId();

        String getAccessType();

        String getSubjectType();

        Long getSubjectId();

        String getSubjectName();
    }

}
