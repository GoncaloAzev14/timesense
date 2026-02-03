package com.datacentric.timesense.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.SystemSetting;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingController {

    public static final class Views {

        public interface GetSystemSettings extends SystemSetting.Views.Public,
                JsonViewPage.Views.Public, User.Views.Basic {
        }
    }

    private SystemSettingRepository systemSettingRepository;
    private UserRepository userRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public SystemSettingController(SystemSettingRepository systemSettingRepository,
            SecurityUtils securityUtils, UserUtils userUtils, UserRepository userRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "MANAGE_SECURITY";
    private static final String BASIC = "basic";

    @JsonView(Views.GetSystemSettings.class)
    @GetMapping
    public JsonViewPage<SystemSetting> getAllSystemSettings(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<SystemSetting> filterSpec = RestUtils
                    .getSpecificationFromFilter(BASIC, filter);
            return new JsonViewPage<>(systemSettingRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(systemSettingRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetSystemSettings.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetSystemSettingById(@PathVariable Long id) {
        try {
            SystemSetting systemSetting = systemSettingRepository.findById(id).orElse(null);
            if (systemSetting == null) {
                return I18nResponses.notFound(MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
            }

            return ResponseEntity.ok(systemSetting);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetSystemSettings.class)
    @GetMapping("/byName/{name}")
    public ResponseEntity<?> getGetSystemSettingByName(@PathVariable String name) {
        try {
            SystemSetting systemSetting = systemSettingRepository.findByName(name);
            if (systemSetting == null) {
                return I18nResponses.notFound(MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
            }

            return ResponseEntity.ok(systemSetting);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetSystemSettings.class)
    @PostMapping()
    public ResponseEntity<?> createSystemSetting(@RequestBody SystemSetting systemSetting) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (systemSettingRepository.existsByName(systemSetting.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            systemSetting.setUpdatedBy(user);

            SystemSetting newSystemSetting = systemSettingRepository.save(systemSetting);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.SYSTEM_SETTING_CREATED_OK,
                    newSystemSetting);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
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

    @JsonView(Views.GetSystemSettings.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSystemSetting(@PathVariable Long id,
            @RequestBody SystemSetting newSystemSetting) {
        try {
            Optional<SystemSetting> result = systemSettingRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
            }

            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            SystemSetting systemSetting = result.get();

            if (!systemSetting.isUserEditable()) {
                return I18nResponses.forbidden(MessagesCodes.SYSTEM_SETTING_NOT_EDITABLE);
            }

            if (!systemSetting.getName().equals(newSystemSetting.getName())) {
                if (systemSettingRepository.existsByName(newSystemSetting.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            systemSetting.setName(newSystemSetting.getName());
            systemSetting.setValue(newSystemSetting.getValue());
            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            systemSetting.setUpdatedBy(user);

            SystemSetting updatedSystemSetting = systemSettingRepository.save(systemSetting);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.SYSTEM_SETTING_UPDATED_OK,
                    updatedSystemSetting);

        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetSystemSettings.class)
    @PostMapping("/closeBusinessYear")
    public ResponseEntity<?> closeBusinessYear() {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            SystemSetting currentYearSetting = systemSettingRepository
                    .findByName(SystemSettings.CURRENT_YEAR);

            String newYear = String.valueOf(Integer.parseInt(currentYearSetting.getValue()) + 1);
            currentYearSetting.setValue(newYear);
            systemSettingRepository.save(currentYearSetting);
            Double defaultVacationDays = Double.parseDouble(systemSettingRepository
                    .findByName(SystemSettings.DEFAULT_VACATION_DAYS).getValue());
            userRepository.newBusinessYearVacations(defaultVacationDays);

            return I18nResponses.accepted(MessagesCodes.CLOSED_BUSINESS_YEAR_OK, null);

        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }       

    }

}
