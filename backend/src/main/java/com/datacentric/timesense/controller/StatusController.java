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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/status")
public class StatusController {
    public static final class Views {
        interface GetBasicInfo extends User.Views.Basic {
        }

        public interface GetAllStatus extends Status.Views.Public,
                JsonViewPage.Views.Public {
        }

        public interface GetStatus extends Status.Views.Complete,
                JsonViewPage.Views.Public {
        }
    }

    private StatusRepository statusRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public StatusController(StatusRepository statusRepository,
            SecurityUtils securityUtils, UserUtils userUtils) {
        this.statusRepository = statusRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "CREATE_PROJECTS";
    private static final String BASIC = "basic";

    @JsonView(Views.GetAllStatus.class)
    @GetMapping
    public JsonViewPage<Status> getAllStatus(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        Specification<Status> spec = Specification.where(null);

        if (!filter.isEmpty()) {
            Specification<Status> filterSpec = RestUtils.getSpecificationFromFilter(BASIC, filter);
            spec = spec.and(filterSpec);
        }

        if (spec != null) {
            return new JsonViewPage<>(statusRepository.findAll(spec, pageable));
        } else {
            return new JsonViewPage<>(statusRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetStatus.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetStatusById(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            Status status = statusRepository.findById(id).orElse(null);
            if (status == null) {
                return I18nResponses.notFound(MessagesCodes.STATUS_NOT_FOUND);
            }

            return ResponseEntity.ok(status);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.STATUS_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetStatus.class)
    @PostMapping()
    public ResponseEntity<?> createStatus(@RequestBody Status status) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (statusRepository.existsByName(status.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            Status newStatus = statusRepository.save(status);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.STATUS_CREATED_OK,
                    newStatus);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStatus(@PathVariable Long id) {
        try {
            Optional<Status> result = statusRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.STATUS_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            statusRepository.deleteStatusById(id);
            return I18nResponses.accepted(MessagesCodes.STATUS_DELETED_OK);

        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetStatus.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClusterId(@PathVariable Long id,
            @RequestBody Status newStatus) {
        try {
            Optional<Status> result = statusRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.STATUS_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            Status status = result.get();
            if (!status.getName().equals(newStatus.getName())) {
                if (statusRepository.existsByName(newStatus.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            status.setName(newStatus.getName());
            status.setType(newStatus.getType());

            Status updatedStatus = statusRepository.save(status);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.STATUS_UPDATED_OK,
                    updatedStatus);

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
