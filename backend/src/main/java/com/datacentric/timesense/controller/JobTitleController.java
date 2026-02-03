package com.datacentric.timesense.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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

import com.datacentric.timesense.model.JobTitle;
import com.datacentric.timesense.repository.JobTitleRepository;
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
@RequestMapping("/api/job-titles")
public class JobTitleController {

    private Logger log = LoggerFactory.getLogger(JobTitleController.class);

    public static final class Views {

        public interface GetJobTitles extends JobTitle.Views.Complete,
                JsonViewPage.Views.Public {
        }

        public interface GetBasicInfo extends JobTitle.Views.Public,
                JsonViewPage.Views.Public {
        }
    }

    private JobTitleRepository jobTitleRepository;
    private UserRepository userRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public JobTitleController(JobTitleRepository jobTitleRepository,
            SecurityUtils securityUtils, UserUtils userUtils,
            UserRepository userRepository) {
        this.jobTitleRepository = jobTitleRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
        this.userRepository = userRepository;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final String REQUIRED_PERMISSION = "MANAGE_SECURITY";

    @JsonView(Views.GetJobTitles.class)
    @GetMapping
    public JsonViewPage<JobTitle> getJobTitles(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        UserSecurityData user = userUtils.getOrCreateUser();
        boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);
        if (!hasPermission) {
            return new JsonViewPage<>(Page.empty());
        }

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<JobTitle> filterSpec = RestUtils
                    .getSpecificationFromFilter("basic", filter);
            return new JsonViewPage<>(jobTitleRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(jobTitleRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetJobTitles.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetJobTitle(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            JobTitle jobTitle = jobTitleRepository.findById(id).orElse(null);
            if (jobTitle == null) {
                return I18nResponses.notFound(MessagesCodes.JOB_TITLE_NOT_FOUND);
            }

            return ResponseEntity.ok(jobTitle);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.JOB_TITLE_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetJobTitles.class)
    @PostMapping()
    public ResponseEntity<?> createJobTitle(@RequestBody JobTitle jobTitle) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (jobTitleRepository.existsByName(jobTitle.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            JobTitle newJobTitle = jobTitleRepository.save(jobTitle);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.JOB_TITLE_CREATED_OK,
                    newJobTitle);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobTitle(@PathVariable Long id) {
        try {
            Optional<JobTitle> result = jobTitleRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.JOB_TITLE_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            boolean isReferencedByUser = userRepository.existsInUserByJobTitleId(id);

            if (isReferencedByUser) {
                return I18nResponses.httpResponse(HttpStatus.CONFLICT,
                        MessagesCodes.JOB_TITLE_CONFLICT_USER);
            }

            jobTitleRepository.deleteJobTitleById(id);
            return I18nResponses.accepted(MessagesCodes.JOB_TITLE_DELETED_OK);

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

    @JsonView(Views.GetJobTitles.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJobTitle(@PathVariable Long id,
            @RequestBody JobTitle newJobTitle) {
        try {
            Optional<JobTitle> result = jobTitleRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.JOB_TITLE_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            JobTitle jobTitle = result.get();
            if (!jobTitle.getName().equals(newJobTitle.getName())) {
                if (jobTitleRepository.existsByName(newJobTitle.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            jobTitle.setName(newJobTitle.getName());
            jobTitle.setRate(newJobTitle.getRate());
            jobTitle.setStartDate(newJobTitle.getStartDate());
            jobTitle.setEndDate(newJobTitle.getEndDate());

            JobTitle updatedJobTitle = jobTitleRepository.save(jobTitle);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.JOB_TITLE_UPDATED_OK,
                    updatedJobTitle);

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

    // TODO: ADD DELETE AND CREATE
    // ON DELETE MAKE SURE THERE IS NO USER REFERENCING THAT JOB TITLE
}
