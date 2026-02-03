package com.datacentric.timesense.controller;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.JobTitle;
import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.SystemSetting;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityCache;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private Logger log = LoggerFactory.getLogger(UserController.class);

    private interface Views {
        interface GetBasicInfo extends User.Views.Basic {
        }

        interface GetUsers extends User.Views.Public, JsonViewPage.Views.Public,
                JobTitle.Views.Public {
        }

        interface GetUser extends User.Views.Complete, UserRole.Views.Minimal,
                UserGroup.Views.Minimal, JobTitle.Views.Public {
        }
    }

    private UserRepository userRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;
    private UserSecurityCache userSecurityCache;
    private SystemSettingRepository systemSettingRepository;

    private static final String SYSTEM = "System";
    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final String ADMIN_ROLE = "Admin";
    private static final String MANAGER_ROLE = "Manager";

    private static final Double DEFAULT_VACATION_DAYS = 23.0;
    private static final String SYSTEM_SETTING_VACATION_DAYS = "default_vacation_days";
    private static final String ID = "id";

    private static final String SCOPE_TEAM = "SCOPE-TEAM";
    private static final String SCOPE_MANAGER = "SCOPE-MANAGER";
    private static final String SCOPE_COMPANY = "SCOPE-COMPANY";

    @Autowired
    public UserController(UserRepository userRepository, UserUtils userUtils,
            SecurityUtils securityUtils, UserSecurityCache userSecurityCache,
            SystemSettingRepository systemSettingRepository) {
        this.userRepository = userRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
        this.userSecurityCache = userSecurityCache;
        this.systemSettingRepository = systemSettingRepository;
    }

    @JsonView(Views.GetUsers.class)
    @GetMapping
    public JsonViewPage<User> getAllUsers(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "name", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter,
            @RequestParam(defaultValue = SCOPE_TEAM, required = false) String scope) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        Specification<User> spec = Specification.where(null);

        // Apply optional text filter first
        if (!filter.isEmpty()) {
            spec = spec.and(RestUtils.getSpecificationFromFilter("basic", filter));
        }

        if (user != null && user.getUserRoles() != null && !scope.equals(SCOPE_COMPANY)) {
            List<String> roleNames = user.getUserRoles().stream()
                    .map(UserRole::getName)
                    .collect(Collectors.toList());

            if (roleNames.contains(ADMIN_ROLE) && !scope.equals(SCOPE_MANAGER)) {
                // Admin sees all users — no additional filtering
            } else if (roleNames.contains(MANAGER_ROLE) || roleNames.contains(ADMIN_ROLE)) {
                // Manager sees users they manage OR themselves
                spec = spec.and((root, query, cb) ->
                        cb.or(
                            cb.equal(root.get("lineManagerId"), user.getId()),
                            cb.equal(root.get(ID), user.getId())
                        )
                );
            } else {
                // Regular users only see themselves
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get(ID), user.getId()));
            }
        }

        return new JsonViewPage<>(userRepository.findAll(spec, pageable));
    }

    @JsonView(Views.GetUser.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            User lineManager = null;

            if (user.getLineManagerId() != null) {
                lineManager = userRepository.findById(user.getLineManagerId()).orElse(null);
            }

            if (lineManager != null) {
                user.setLineManager(lineManager);
            }

            return ResponseEntity.ok(user);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error retreiving user by id: {}", e.getMessage());
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetUser.class)
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo() {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            return ResponseEntity.ok(user);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }

    }

    @JsonView(Views.GetUser.class)
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                    Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                    currentUser.getRoles(), currentUser.getUserGroups())) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            List<ValidationFailure> validationErrors = user.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED,
                        validationErrors);
            }

            SystemSetting setVacationDaysOnCreation = systemSettingRepository.findByName(
                    SystemSettings.SET_VACATION_DAYS_ON_USER_CREATION);
            if (setVacationDaysOnCreation != null &&
                    setVacationDaysOnCreation.getValue().equalsIgnoreCase("true")) {
                SystemSetting vacsDaysSetting =
                    systemSettingRepository.findByName(SYSTEM_SETTING_VACATION_DAYS);

                if (vacsDaysSetting != null) {
                    Double vacationDays = Double.valueOf(vacsDaysSetting.getValue());
                    user.setPrevYearVacationDays(vacationDays);
                    user.setCurrentYearVacationDays(vacationDays);
                } else {
                    user.setPrevYearVacationDays(DEFAULT_VACATION_DAYS);
                    user.setCurrentYearVacationDays(DEFAULT_VACATION_DAYS);
                }
            } else {
                user.setPrevYearVacationDays(0.0);
                user.setCurrentYearVacationDays(0.0);
            }

            User savedUser = userRepository.save(user);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.USER_CREATED_OK,
                    savedUser);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(
                    MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                currentUser.getRoles(), currentUser.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        try {
            Optional<User> result = userRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(
                        MessagesCodes.USER_NOT_FOUND);
            }

            userSecurityCache.invalidateUser(result.get().getEmail());
            userRepository.deleteById(id);
            return I18nResponses.httpResponse(HttpStatus.ACCEPTED,
                    MessagesCodes.USER_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(
                    MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetUser.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserById(@PathVariable Long id,
            @RequestBody User newUser) {
        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasPermissionByIds(SYSTEM, 0L,
                Arrays.asList(SystemAccessTypes.MANAGE_SECURITY), currentUser.getId(),
                currentUser.getRoles(), currentUser.getUserGroups())) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        try {
            Optional<User> result = userRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            User user = result.get();
            user.setName(newUser.getName());
            user.setBirthdate(newUser.getBirthdate());
            user.setEmail(newUser.getEmail());
            user.setLineManagerId(newUser.getLineManagerId());
            user.setLineManager(newUser.getLineManager());
            user.setJobTitle(newUser.getJobTitle());
            user.setCurrentYearVacationDays(newUser.getCurrentYearVacationDays());
            user.setPrevYearVacationDays(newUser.getPrevYearVacationDays());
            user.setUserRoles(newUser.getUserRoles());
            user.setUserGroups(newUser.getUserGroups());
            user.setAdmissionDate(newUser.getAdmissionDate());
            user.setExitDate(newUser.getExitDate());

            List<ValidationFailure> validationErrors = user.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }

            User updatedUser = userRepository.save(user);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.USER_UPDATED_OK,
                    updatedUser);

        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            return I18nResponses.badRequest(
                    MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/synchronize")
    public ResponseEntity<?> synchronizeUsers() {
        log.info("Synchronizing users...");

        try {
            securityUtils.synchronizeUsers();
            return I18nResponses.httpResponse(HttpStatus.OK, MessagesCodes.USERS_SYNCHRONIZED_OK);
        } catch (Exception e) {
            log.error("Unexpected exception ", e);
            log.error("Error synchronizing users: {}", e.getMessage());
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
