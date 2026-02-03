package com.datacentric.timesense.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

    private static final String SYSTEM = "System";

    interface Views {
        interface GetRoles extends UserRole.Views.List, JsonViewPage.Views.Public {
        }

        interface GetRole extends UserRole.Views.Complete {
        }
    }

    private UserRoleRepository roleRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;

    private static final long ROLE_USER_ID = 1L;
    private static final long ROLE_MANAGER_ID = 2L;
    private static final long ROLE_ADMIN_ID = 3L;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";

    @Autowired
    public UserRoleController(UserRoleRepository roleRepository, UserUtils userUtils,
            SecurityUtils securityUtils) {
        this.roleRepository = roleRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
    }

    @JsonView(Views.GetRoles.class)
    @GetMapping
    public JsonViewPage<UserRole> getAllRoles(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "name", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (filter != null && filter.length() > 0) {
            Specification<UserRole> spec = RestUtils.getSpecificationFromFilter("basic",
                    filter);
            return new JsonViewPage<>(roleRepository.findAll(spec, pageable));
        }
        return new JsonViewPage<>(roleRepository.findAll(pageable));
    }

    /**
     * Get all the user permissions.
     */
    @GetMapping("/user/internal")
    public final ResponseEntity<List<UserRole>> getMasterUserPermissions() {

        UserSecurityData user = userUtils.getOrCreateUser();

        /*
         * Restrict the roles to the internal roles.
         * The possibles are 1, 2 and 3. (1 = User, 2 = Manager, 3 = Admin)
         */
        List<Long> internalIds = Arrays.asList(ROLE_USER_ID, ROLE_MANAGER_ID, ROLE_ADMIN_ID);

        List<Long> userRolesIds = user.getRoles().stream()
                .filter(roleId -> internalIds.contains(roleId))
                .collect(Collectors.toList());
        List<UserRole> userRoles = roleRepository.findAllById(userRolesIds);

        return ResponseEntity.ok(userRoles.stream().collect(Collectors.toList()));
    }

    @JsonView(Views.GetRole.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id) {
        UserRole role = roleRepository.findById(id).orElse(null);
        if (role == null) {
            return I18nResponses.notFound(MessagesCodes.ROLE_NOT_FOUND);
        }
        return ResponseEntity.ok(role);
    }

    @JsonView(Views.GetRole.class)
    @PostMapping
    public ResponseEntity<?> createUserRole(@RequestBody UserRole role) {
        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                currentUser.getRoles(), currentUser.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        currentUser.markCreatedBy(role);
        currentUser.markUpdatedBy(role);
        UserRole createdRole = roleRepository.save(role);
        return I18nResponses.httpResponseWithData(HttpStatus.CREATED, MessagesCodes.ROLE_CREATED_OK,
                createdRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                currentUser.getRoles(), currentUser.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        try {
            Optional<UserRole> result = roleRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ROLE_NOT_FOUND);
            } else {
                roleRepository.deleteUserRoleById(id, currentUser.getId());
                return I18nResponses.accepted(MessagesCodes.ROLE_DELETED_OK);
            }
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetRole.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoleById(@PathVariable Long id,
            @RequestBody UserRole newRole) {

        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                    Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                    currentUser.getRoles(), currentUser.getUserGroups())) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<UserRole> result = roleRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ROLE_NOT_FOUND);
            } else {
                UserRole role = result.get();
                role.setName(newRole.getName());
                currentUser.markUpdatedBy(role);
                UserRole updatedRole = roleRepository.save(role);
                return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                        MessagesCodes.ROLE_UPDATED_OK,
                        updatedRole);
            }
        } catch (Exception e) {
            return I18nResponses.notFound(MessagesCodes.ROLE_NOT_FOUND);
        }
    }

}
