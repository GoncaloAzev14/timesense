package com.datacentric.timesense.controller;

import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.UserGroupRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityCache;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/user-groups")
public class UserGroupController {

    private Logger logger = LoggerFactory.getLogger(UserGroupController.class);

    private static final String SYSTEM = "System";

    interface Views {
        interface GetUserGroups extends UserGroup.Views.List, JsonViewPage.Views.Public,
                User.Views.Public {
        }

        interface GetUserGroup extends UserGroup.Views.Complete, UserRole.Views.Minimal {
        }
    }

    private UserGroupRepository userGroupRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;
    private UserSecurityCache userSecurityCache;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";

    @Autowired
    public UserGroupController(UserGroupRepository userGroupRepository, UserUtils userUtils,
            SecurityUtils securityUtils, UserSecurityCache userSecurityCache) {
        this.userGroupRepository = userGroupRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
        this.userSecurityCache = userSecurityCache;
    }

    @JsonView(Views.GetUserGroups.class)
    @GetMapping
    public JsonViewPage<UserGroup> getAllUserGroups(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "name", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {
        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<UserGroup> spec = RestUtils.getSpecificationFromFilter("basic", filter);
            return new JsonViewPage<>(userGroupRepository.findAll(spec, pageable));
        }
        return new JsonViewPage<>(userGroupRepository.findAll(pageable));
    }

    @JsonView(Views.GetUserGroup.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserGroupById(@PathVariable Long id) {
        UserGroup userGroup = userGroupRepository.findById(id).orElse(null);
        if (userGroup == null) {
            return I18nResponses.notFound(MessagesCodes.USER_GROUP_NOT_FOUND);
        }

        return ResponseEntity.ok(userGroup);
    }

    @JsonView(Views.GetUserGroup.class)
    @PostMapping
    public ResponseEntity<?> createUserGroup(@RequestBody UserGroup userGroup) {
        UserSecurityData user = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), user.getId(), user.getRoles(),
                user.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        user.markCreatedBy(userGroup);
        user.markUpdatedBy(userGroup);
        UserGroup createdGroup = userGroupRepository.save(userGroup);
        return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                MessagesCodes.USER_GROUP_CREATED_OK,
                createdGroup);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserGroup(@PathVariable Long id) {
        UserSecurityData user = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), user.getId(), user.getRoles(),
                user.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        try {
            Optional<UserGroup> result = userGroupRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.USER_GROUP_NOT_FOUND);
            } else {
                userGroupRepository.deleteUserGroupById(id, user.getId());
                userSecurityCache.invalidateUserGroup(id);
                return I18nResponses.accepted(MessagesCodes.USER_GROUP_DELETED_OK);
            }
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetUserGroup.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserGroupById(@PathVariable Long id,
            @RequestBody UserGroup newUserGroup) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                    Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), user.getId(), user.getRoles(),
                    user.getUserGroups())) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            Optional<UserGroup> result = userGroupRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.USER_GROUP_NOT_FOUND);
            } else {
                UserGroup userGroup = result.get();
                userGroup.setName(newUserGroup.getName());
                userGroup.setUserRoles(newUserGroup.getUserRoles());
                user.markUpdatedBy(userGroup);
                UserGroup updatedUserGroup = userGroupRepository.save(userGroup);
                userSecurityCache.invalidateUserGroup(id);
                return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                        MessagesCodes.USER_GROUP_UPDATED_OK,
                        updatedUserGroup);
            }
        } catch (Exception e) {
            logger.error("Error updating user group", e);
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

}
