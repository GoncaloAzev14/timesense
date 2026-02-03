package com.datacentric.timesense.controller;

import java.util.NoSuchElementException;
import java.util.Optional;

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

import com.datacentric.timesense.model.AbsenceType;
import static com.datacentric.timesense.model.SystemAccessTypes.MANAGE_TIMEOFF;
import com.datacentric.timesense.repository.AbsenceTypeRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.I18nResponses;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/absence-types")
public class AbsenceTypeController {

    private Logger log = LoggerFactory.getLogger(AbsenceTypeController.class);

    public static final class Views {

        interface GetAbsenceTypes extends AbsenceType.Views.List, JsonViewPage.Views.Public {
        }

        interface GetAbsenceType extends AbsenceType.Views.Complete {
        }
    }

    private AbsenceTypeRepository absenceTypeRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";

    @Autowired
    public AbsenceTypeController(AbsenceTypeRepository absenceTypeRepository,
            UserUtils userUtils, SecurityUtils securityUtils) {
        this.absenceTypeRepository = absenceTypeRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
    }

    @JsonView(Views.GetAbsenceTypes.class)
    @GetMapping
    public JsonViewPage<AbsenceType> getAllAbsenceTypes(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "name", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<AbsenceType> filterSpec = RestUtils
                    .getSpecificationFromFilter("basic",
                            filter);
            return new JsonViewPage<>(absenceTypeRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(absenceTypeRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetAbsenceType.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getAbsenceTypeById(@PathVariable Long id) {
        try {
            AbsenceType absenceType = absenceTypeRepository.findById(id).orElse(null);
            if (absenceType == null) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
            }

            return ResponseEntity.ok(absenceType);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }

    }

    @JsonView(Views.GetAbsenceType.class)
    @PostMapping
    public ResponseEntity<?> createAbsenceType(@RequestBody AbsenceType absenceType) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, MANAGE_TIMEOFF)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            AbsenceType savedAbsenceType = absenceTypeRepository.save(absenceType);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.ABSENCE_TYPE_CREATED_OK, savedAbsenceType);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAbsenceType(@PathVariable Long id) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, MANAGE_TIMEOFF)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<AbsenceType> result = absenceTypeRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
            }

            absenceTypeRepository.deleteById(id);
            return I18nResponses.accepted(MessagesCodes.ABSENCE_TYPE_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetAbsenceType.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAbsenceTypeById(@PathVariable Long id,
            @RequestBody AbsenceType newAbsenceType) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, MANAGE_TIMEOFF)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<AbsenceType> result = absenceTypeRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
            }

            AbsenceType absenceType = result.get();
            absenceType.setName(newAbsenceType.getName());

            AbsenceType updatedAbsenceType = absenceTypeRepository.save(absenceType);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.ABSENCE_TYPE_UPDATED_OK, updatedAbsenceType);

        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

}
