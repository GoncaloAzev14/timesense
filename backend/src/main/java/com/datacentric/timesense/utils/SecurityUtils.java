package com.datacentric.timesense.utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.datacentric.exceptions.DataCentricException;
import com.datacentric.security.GroupMetadataProvider;
import com.datacentric.security.UserData;
import com.datacentric.security.UserGroupData;
import com.datacentric.timesense.model.JobTitle;
import com.datacentric.timesense.model.ResourcePermission;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.JobTitleRepository;
import com.datacentric.timesense.repository.ResourcePermissionRepository;
import com.datacentric.timesense.repository.UserGroupRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.utils.security.UserSecurityData;

import jakarta.annotation.PostConstruct;

@Component
public class SecurityUtils {

    private Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private static final String SYSTEM_RESOURCE_TYPE = "System";

    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String GROUPS = "groups";
    private static final String JWT_USER_MAPPING_NAME = "server.jwt.user-mapping.name";
    private static final String JWT_USER_MAPPING_EMAIL = "server.jwt.user-mapping.email";
    private static final String JWT_USER_MAPPING_GROUPS = "server.jwt.user-mapping.groups";
    private static final String JWT_SYNCHRONIZE_GROUPS = "server.jwt.synchronize.groups";

    private static final String ADD_MANAGER_PLACEHOLDER_TO_DB = "Adding Manager placeholder to DB";
    private static final String MANAGER_FOUND_IN_DB = "Manager found in DB: ";
    private static final String UPDATING_MANAGER_FOR_USER = "Updating manager for user {}";
    private static final String ERROR_SYNCHRONIZING_USERS = "Error synchronizing users";
    private static final String USER_INVALID_JOB_TITLE = "User {} has no valid role '{}', skipping";

    private static final String UNKNOWN_USER = "Unknown user";
    private static final String SUBJECT_TYPE_USER = "user";

    private static final List<String> USERS_TITLES = List.of("Trainee", "Internship",
            "Junior Developer I", "Junior Developer II", "Technician I", "Technician II",
            "Technician III");
    private static final List<String> MANAGERS_TITLES = List.of("Technical Lead I",
            "Technical Lead II", "Senior Consultant I", "Senior Manager");
    private static final List<String> ADMIN_TITLES = List.of("Partner");

    private Environment env;
    private ResourcePermissionRepository resourcePermissionRepository;
    private UserRepository userRepository;
    private UserGroupRepository userGroupRepository;
    private UserRoleRepository userRoleRepository;
    private GroupMetadataProvider groupMetadataProvider;
    private JobTitleRepository jobTitleRepository;

    boolean updateGroupsBasedOnToken;

    @Autowired
    public SecurityUtils(Environment env, ResourcePermissionRepository resourcePermissionRepository,
            UserRepository userRepository, UserGroupRepository userGroupRepository,
            GroupMetadataProvider groupMetadataProvider, JobTitleRepository jobTitleRepository,
            UserRoleRepository userRoleRepository) {
        this.updateGroupsBasedOnToken = false;
        this.env = env;
        this.resourcePermissionRepository = resourcePermissionRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupMetadataProvider = groupMetadataProvider;
        this.jobTitleRepository = jobTitleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @PostConstruct
    public void start() {
        updateGroupsBasedOnToken = "true"
                .equalsIgnoreCase(env.getProperty(JWT_SYNCHRONIZE_GROUPS, "false"));
        if (updateGroupsBasedOnToken) {
            log.info("Synchronizing user groups based on token");
        }
    }

    /**
     * Updates the user groups based on the token information.
     */
    public void updateUserGroups(User user, List<String> userGroups) {
        if (!updateGroupsBasedOnToken || userGroups == null) {
            return;
        }

        List<UserGroup> userGroupsWithTokenList = updateUserGroupsFromRemoteProvider(userGroups);
        List<UserGroup> userGroupsList = user.getUserGroups();
        if (userGroupsList == null) {
            userGroupsList = new ArrayList<>();
        }
        Set<String> currentGroupsTokenIds = userGroupsList.stream()
                .map(UserGroup::getTokenId)
                .filter(tokenId -> tokenId != null)
                .collect(Collectors.toSet());

        boolean userGroupsChanged = userGroupsList.removeIf(
                group -> group.getTokenId() != null && !userGroupsWithTokenList.contains(group));
        userGroupsWithTokenList
                .removeIf(group -> currentGroupsTokenIds.contains(group.getTokenId()));
        userGroupsList.addAll(userGroupsWithTokenList);
        userGroupsChanged = userGroupsChanged || !userGroupsWithTokenList.isEmpty();

        if (userGroupsChanged) {
            user.setUserGroups(userGroupsWithTokenList);
            log.info("Saving the user information with the updated user groups for user: {}",
                    user.getName());
            userRepository.save(user);
        }
    }

    /**
     * Downloads the metadata for the groups that the user is part of and updates
     * the
     * database with the new information.
     */
    private List<UserGroup> updateUserGroupsFromRemoteProvider(List<String> userGroups) {

        Set<String> groupsSet = new HashSet<>(userGroups);
        List<UserGroup> userGroupsWithTokenList = userGroupRepository.findByTokenIdIn(userGroups);

        // Update already known groups if our data is too old
        for (UserGroup group : userGroupsWithTokenList) {
            String prevGroupName = group.getName();
            groupsSet.remove(group.getTokenId());

            String newTokenId = group.getTokenId();
            UserGroupData newGroup = groupMetadataProvider.getUserGroup(newTokenId);

            if (prevGroupName != null && !prevGroupName.equals(newGroup.getName())) {
                log.info("Updating group name for tokenId {}", newTokenId);
                group.setName(newGroup.getName());
                userGroupRepository.save(group);
            }
        }

        // Create missing groups in the database
        if (!groupsSet.isEmpty()) {
            // Synchronize this block so that we don't end up with duplicate groups
            // with the same external uuid on the database when multiple requests
            // are being executed at the same time.
            synchronized (this) {
                // check if some of the groups have been created meanwhile and
                // remove them so that we don't download their metadata from
                // the third party service again. This is only executed when a
                // new group is created which is a rare event.
                List<UserGroup> newUserGroups = userGroupRepository
                        .findByTokenIdIn(new ArrayList<>(groupsSet));
                for (UserGroup group : userGroupsWithTokenList) {
                    groupsSet.remove(group.getTokenId());
                }

                newUserGroups = new ArrayList<>(groupsSet.size());
                for (String groupId : groupsSet) {
                    UserGroupData group = groupMetadataProvider.getUserGroup(groupId);
                    UserGroup newGroup = new UserGroup();
                    newGroup.setTokenId(group.getExternalId());
                    newGroup.setName(group.getName());

                    newUserGroups.add(newGroup);
                }
                if (!newUserGroups.isEmpty()) {
                    log.info("Adding {} new groups to the database", newUserGroups.size());
                    newUserGroups = userGroupRepository.saveAll(newUserGroups);
                }
                userGroupsWithTokenList.addAll(newUserGroups);
            }
        }
        return userGroupsWithTokenList;
    }

    /**
     * Extracts the user defining attributes from the claims of the access token.
     */
    public Map<String, Object> getUserAttributesFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> userDetails = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userName = jwt
                    .getClaimAsString(env.getProperty(JWT_USER_MAPPING_NAME, NAME));
            String userEmail = jwt
                    .getClaimAsString(env.getProperty(JWT_USER_MAPPING_EMAIL, EMAIL));
            List<String> userGroups = jwt
                    .getClaimAsStringList(env.getProperty(JWT_USER_MAPPING_GROUPS, GROUPS));

            userDetails.put("userName", userName);
            userDetails.put("userEmail", userEmail);
            userDetails.put("userGroups", userGroups);

            return userDetails;
        }
        return null;
    }

    public boolean hasPermissionByIds(String resourceType, Long resourceId,
            List<String> accessType, Long userId, List<Long> userRoles,
            List<Long> userGroups) {

        List<ResourcePermission> result = resourcePermissionRepository.getResourcePermission(
                resourceType, resourceId, accessType, userId, userRoles, userGroups);

        if (result.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean hasPermissionByIds(String resourceType, Long resourceId,
            UserSecurityData user, String... accessTypes) {
        return hasPermissionByIds(resourceType, resourceId, List.of(accessTypes),
                user.getId(), user.getRoles(), user.getUserGroups());
    }

    public boolean hasSystemPermission(UserSecurityData user, String... accessTypes) {
        return hasPermissionByIds(SYSTEM_RESOURCE_TYPE, 0L, List.of(accessTypes),
                user.getId(), user.getRoles(), user.getUserGroups());
    }

    public boolean hasPermission(String resourceType, Long resourceId, List<String> accessType,
            User user, List<UserRole> roles, List<UserGroup> userGroups) {

        List<Long> roleIds = roles.stream().map(UserRole::getId).toList();
        List<Long> userGroupIds = userGroups.stream().map(UserGroup::getId).toList();

        return hasPermissionByIds(resourceType, resourceId, accessType, user.getId(), roleIds,
                userGroupIds);
    }

    public boolean hasPermission(String resourceType, Long resourceId, List<String> accessType,
            User user) {

        return hasPermission(resourceType, resourceId, accessType, user, user.getUserRoles(),
                user.getUserGroups());
    }

    public List<Long> getResourceIdsWithAccess(String resourceType, List<Long> resourceIds,
            List<String> accessTypes, Long userId, List<Long> userRoles, List<Long> userGroups) {
        return resourcePermissionRepository.allowedResourcesList(resourceType,
                resourceIds, accessTypes, userId, userRoles, userGroups);
    }

    public void addPermission(String resourceType, Long resourceId, String accessType,
            UserSecurityData user) {

        ResourcePermission appResourcePermission = new ResourcePermission(
                resourceType, resourceId, accessType, SUBJECT_TYPE_USER, user.getId(),
                user.toUserPlaceholder());
        resourcePermissionRepository.save(appResourcePermission);
    }

    public void addUserPermission(String resourceType, Long resourceId, String accessType,
            User user) {

        ResourcePermission appResourcePermission = new ResourcePermission(
                resourceType, resourceId, accessType, SUBJECT_TYPE_USER, user.getId(),
                user);
        resourcePermissionRepository.save(appResourcePermission);
    }

    public void addRolePermission(String resourceType, Long resourceId, String accessType,
            UserRole role, UserSecurityData createdBy) {

        ResourcePermission appResourcePermission = new ResourcePermission(
                resourceType, resourceId, accessType, "role", role.getId(),
                createdBy.toUserPlaceholder());
        resourcePermissionRepository.save(appResourcePermission);
    }

    public void synchronizeUsers() {
        try {
            // Load the roles to use in the synchronization
            UserRole adminRole = userRoleRepository.findByName("Admin");
            UserRole managerRole = userRoleRepository.findByName("Manager");
            UserRole userRole = userRoleRepository.findByName("User");

            List<JobTitle> jobTitlesList = jobTitleRepository.findAll();
            // Function.identity() returns the JobTitle object as the value
            Map<String, JobTitle> jobTitlesMap = jobTitlesList.stream().collect(
                    Collectors.toMap(JobTitle::getName, Function.identity()));

            groupMetadataProvider.synchronizeUsers(user -> {
                log.debug("Synchronizing user {}", user.getName());
                if ("N/A".equals(user.getEmail())) {
                    log.debug("User {} has no email, skipping", user.getName());
                    return;
                }
                Optional<User> userInDb = userRepository.findByEmail(user.getEmail());
                User userToUpdate = null;
                if (!userInDb.isPresent()) {
                    userToUpdate = new User();
                    userToUpdate.setName(user.getName());
                    userToUpdate.setEmail(user.getEmail());
                    userToUpdate.setBirthdate(user.getBirthdate());
                    userToUpdate.setUserRoles(new ArrayList<>());
                    if (jobTitlesMap.get(user.getJobTitle()) == null) {
                        log.warn(USER_INVALID_JOB_TITLE, user.getName(), user.getJobTitle());
                        return;
                    }
                    userToUpdate.setJobTitle(jobTitlesMap.get(user.getJobTitle()));
                    userToUpdate.setPrevYearVacationDays(0d);
                    userToUpdate.setCurrentYearVacationDays(0d);
                    userToUpdate.setUserGroups(new ArrayList<>());
                    userToUpdate.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    populateUserManager(user, userToUpdate);
                } else {
                    userToUpdate = userInDb.get();
                    userToUpdate.setName(user.getName());
                    userToUpdate.setEmail(user.getEmail());
                    userToUpdate.setBirthdate(user.getBirthdate());
                    if (jobTitlesMap.get(user.getJobTitle()) == null) {
                        log.warn(USER_INVALID_JOB_TITLE, user.getName(), user.getJobTitle());
                        return;
                    }
                    userToUpdate.setJobTitle(jobTitlesMap.get(user.getJobTitle()));
                    populateUserManager(user, userToUpdate);
                }

                List<UserRole> currentUserRoles = userToUpdate.getUserRoles();
                if (currentUserRoles == null) {
                    currentUserRoles = new ArrayList<>();
                }
                log.info("Current user roles for {}: {}", userToUpdate.getEmail(),
                        currentUserRoles.stream().map(UserRole::getName)
                                .collect(Collectors.joining(", ")));

                // set system user role based on user job title
                if (MANAGERS_TITLES.contains(userToUpdate.getJobTitle().getName())) {
                    if (!currentUserRoles.contains(managerRole)) {
                        log.info("Adding Manager role to user {}", userToUpdate.getEmail());
                        currentUserRoles.add(managerRole);
                    }
                } else if (ADMIN_TITLES.contains(userToUpdate.getJobTitle().getName())) {
                    if (!currentUserRoles.contains(adminRole)) {
                        log.info("Adding Admin role to user {}", userToUpdate.getEmail());
                        currentUserRoles.add(adminRole);
                    }
                } else {
                    if (!currentUserRoles.contains(userRole)) {
                        log.info("Adding User role to user {}", userToUpdate.getEmail());
                        currentUserRoles.add(userRole);
                    }
                }

                userToUpdate.setUserRoles(currentUserRoles);
                userToUpdate.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                userRepository.save(userToUpdate);
            });
        } catch (DataCentricException | InterruptedException e) {
            log.error(ERROR_SYNCHRONIZING_USERS, e);
            throw new RuntimeException(ERROR_SYNCHRONIZING_USERS, e);
        }
    }

    private void populateUserManager(UserData user, User userToUpdate) {
        if (user.getManagerId() == null) {
            return;
        }
        log.debug(UPDATING_MANAGER_FOR_USER, user.getName());
        Optional<User> managerInDb = userRepository
                .findByEmail(user.getManagerId());
        if (managerInDb.isPresent()) {
            log.debug(MANAGER_FOUND_IN_DB + managerInDb.get().getEmail());
            userToUpdate.setLineManagerId(managerInDb.get().getId());
        } else {
            log.debug(ADD_MANAGER_PLACEHOLDER_TO_DB);
            User managerToUpdate = new User();
            managerToUpdate.setEmail(user.getManagerId());
            managerToUpdate.setPrevYearVacationDays(0d);
            managerToUpdate.setCurrentYearVacationDays(0d);
            managerToUpdate.setName(UNKNOWN_USER);
            managerToUpdate = userRepository.save(managerToUpdate);
            managerToUpdate.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            userToUpdate.setLineManagerId(managerToUpdate.getId());
        }
    }
}
