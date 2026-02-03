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

import com.datacentric.timesense.model.AbsenceSubType;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.AbsenceSubTypeRepository;
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
@RequestMapping("/api/absence-sub-types")
public class AbsenceSubTypeController {

    public static final class Views {
        public interface GetInfo extends AbsenceSubType.Views.Basic, JsonViewPage.Views.Public,
                User.Views.Basic{
        }
    }

    private AbsenceSubTypeRepository absenceSubTypeRepository;
    private UserRepository userRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public AbsenceSubTypeController(AbsenceSubTypeRepository absenceSubTypeRepository,
            UserRepository userRepository, SecurityUtils securityUtils, UserUtils userUtils) {

        this.absenceSubTypeRepository = absenceSubTypeRepository;
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

    @JsonView(Views.GetInfo.class)
    @GetMapping
    public JsonViewPage<AbsenceSubType> getAllAbsenceSubTypes(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<AbsenceSubType> filterSpec = RestUtils
                    .getSpecificationFromFilter(BASIC, filter);
            return new JsonViewPage<>(absenceSubTypeRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(absenceSubTypeRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetInfo.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetAbsenceSubTypeById(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            AbsenceSubType absenceSubType = absenceSubTypeRepository.findById(id).orElse(null);
            if (absenceSubType == null) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_SUB_TYPE_NOT_FOUND);
            }

            return ResponseEntity.ok(absenceSubType);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.ABSENCE_SUB_TYPE_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetInfo.class)
    @PostMapping()
    public ResponseEntity<?> createAbsenceSubType(@RequestBody AbsenceSubType absenceSubType) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (absenceSubTypeRepository.existsByName(absenceSubType.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            absenceSubType.setUpdatedBy(user);

            AbsenceSubType newAbsenceSubType = absenceSubTypeRepository.save(absenceSubType);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.ABSENCE_SUB_TYPE_CREATED_OK,
                    newAbsenceSubType);
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

    @JsonView(Views.GetInfo.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAbsenceSubType(@PathVariable Long id,
            @RequestBody AbsenceSubType newAbsenceSubType) {
        try {
            Optional<AbsenceSubType> result = absenceSubTypeRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_SUB_TYPE_NOT_FOUND);
            }

            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            AbsenceSubType absenceSubType = result.get();
            if (!absenceSubType.getName().equals(newAbsenceSubType.getName())) {
                if (absenceSubTypeRepository.existsByName(newAbsenceSubType.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            absenceSubType.setName(newAbsenceSubType.getName());
            absenceSubType.setDescription(newAbsenceSubType.getDescription());

            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            absenceSubType.setUpdatedBy(user);

            AbsenceSubType updatedabsenceSubType = absenceSubTypeRepository.save(absenceSubType);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.ABSENCE_SUB_TYPE_UPDATED_OK,
                    updatedabsenceSubType);

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
