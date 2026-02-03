package com.datacentric.timesense.controller;

import java.util.List;
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

import com.datacentric.timesense.model.AuditableTable;
import com.datacentric.timesense.model.Project;
import com.datacentric.timesense.model.ProjectAssignment;
import static com.datacentric.timesense.model.SystemAccessTypes.CREATE_PROJECTS;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ProjectAssignmentRepository;
import com.datacentric.timesense.repository.ProjectRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/project-assignments")
public class ProjectAssignmentController {

    private Logger log = LoggerFactory.getLogger(ProjectAssignmentController.class);

    private interface Views {

        interface GetProjectAssignments extends ProjectAssignment.Views.Public,
                Project.Views.Public, User.Views.Public, JsonViewPage.Views.Public,
                AuditableTable.Views.List {
        }

        interface GetProjectAssignment extends ProjectAssignment.Views.Complete,
                Project.Views.Complete, User.Views.Complete, AuditableTable.Views.List {
        }
    }

    private ProjectAssignmentRepository projectAssignmentRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final String PROJ_RESOURCE_TYPE = "Project";

    @Autowired
    public ProjectAssignmentController(ProjectAssignmentRepository projectAssignmentRepository,
            ProjectRepository projectRepository, UserRepository userRepository,
            UserUtils userUtils, SecurityUtils securityUtils) {
        this.projectAssignmentRepository = projectAssignmentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
    }

    @JsonView(Views.GetProjectAssignments.class)
    @GetMapping
    public JsonViewPage<ProjectAssignment> getAllProjectAssignments(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<ProjectAssignment> filterSpec = RestUtils
                    .getSpecificationFromFilter("basic", filter);
            return new JsonViewPage<>(projectAssignmentRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(projectAssignmentRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetProjectAssignment.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectAssignmentById(@PathVariable Long id) {
        try {
            ProjectAssignment projectAssignment = projectAssignmentRepository.findById(id)
                    .orElse(null);
            if (projectAssignment == null) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND);
            }

            return ResponseEntity.ok(projectAssignment);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }

    }

    @JsonView(Views.GetProjectAssignment.class)
    @PostMapping
    public ResponseEntity<?> createProjectAssignment(
            @RequestBody ProjectAssignment projectAssignment) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                    !canEditProject(projectAssignment.getProject().getId(), currentUser)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            List<ValidationFailure> validationErrors = projectAssignment.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }
            if (!userRepository.existsById(projectAssignment.getUser().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            if (!projectRepository.existsById(projectAssignment.getProject().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            currentUser.markCreatedBy(projectAssignment);
            currentUser.markUpdatedBy(projectAssignment);
            ProjectAssignment savedProjectAssignment =
                projectAssignmentRepository.save(projectAssignment);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.PROJECT_ASSIGNMENT_CREATED_OK,
                    savedProjectAssignment);
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
    public ResponseEntity<?> deleteProjectAssignment(@PathVariable Long id) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) ) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<ProjectAssignment> result = projectAssignmentRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(
                        MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND);
            }

            projectAssignmentRepository.deleteById(id);
            return I18nResponses.accepted(MessagesCodes.PROJECT_ASSIGNMENT_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetProjectAssignment.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProjectAssignmentById(@PathVariable Long id,
            @RequestBody ProjectAssignment newProjectAssignment) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                    !canEditProject(newProjectAssignment.getProject().getId(), currentUser)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<ProjectAssignment> result = projectAssignmentRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(
                        MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND);
            }

            ProjectAssignment projectAssignment = result.get();
            projectAssignment.setUser(newProjectAssignment.getUser());
            projectAssignment.setProject(newProjectAssignment.getProject());
            projectAssignment.setAllocation(newProjectAssignment.getAllocation());
            projectAssignment.setStartDate(newProjectAssignment.getStartDate());
            projectAssignment.setEndDate(newProjectAssignment.getEndDate());
            projectAssignment.setDescription(newProjectAssignment.getDescription());

            List<ValidationFailure> validationErrors = projectAssignment.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }
            if (!projectRepository.existsById(projectAssignment.getProject().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }
            if (!userRepository.existsById(projectAssignment.getUser().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            ProjectAssignment updatedProjectAssignment =
                projectAssignmentRepository.save(projectAssignment);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_ASSIGNMENT_UPDATED_OK,
                    updatedProjectAssignment);

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

    private boolean canEditProject(Long id, UserSecurityData currentUser) {
        return securityUtils.hasPermissionByIds(PROJ_RESOURCE_TYPE, id, currentUser,
                Project.ProjectPermission.EDIT_PROJECTS.toString());
    }
}
