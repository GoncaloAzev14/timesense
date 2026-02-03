package com.datacentric.timesense.controller;

import java.util.List;
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

import com.datacentric.timesense.model.ProjectTask;
import com.datacentric.timesense.model.ProjectType;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ProjectTaskRepository;
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
@RequestMapping("/api/project-tasks")
public class ProjectTaskController {

    public static final class Views {
        public interface GetInfo extends ProjectTask.Views.Basic, JsonViewPage.Views.Public,
                User.Views.Basic, ProjectType.Views.Public{
        }
    }

    private ProjectTaskRepository projectTaskRepository;
    private UserRepository userRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public ProjectTaskController(ProjectTaskRepository projectTaskRepository,
            UserRepository userRepository, SecurityUtils securityUtils, UserUtils userUtils) {
        this.projectTaskRepository = projectTaskRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 1000;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "CREATE_PROJECTS";
    private static final String BASIC = "basic";

    @JsonView(Views.GetInfo.class)
    @GetMapping
    public JsonViewPage<ProjectTask> getAllProjectTasks(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<ProjectTask> filterSpec = RestUtils
                    .getSpecificationFromFilter(BASIC, filter);
            return new JsonViewPage<>(projectTaskRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(projectTaskRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetInfo.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectTaskById(@PathVariable Long id) {
        try {

            ProjectTask projectTask = projectTaskRepository.findById(id).orElse(null);
            if (projectTask == null) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TASK_NOT_FOUND);
            }

            return ResponseEntity.ok(projectTask);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_TASK_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetInfo.class)
    @PostMapping()
    public ResponseEntity<?> createProjectTask(@RequestBody ProjectTask projectTask) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (projectTaskRepository.existsByName(projectTask.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            projectTask.setUpdatedBy(user);

            ProjectTask newProjectTask = projectTaskRepository.save(projectTask);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.PROJECT_TASK_CREATED_OK,
                    newProjectTask);
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
            @RequestBody ProjectTask newProjectTask) {
        try {
            Optional<ProjectTask> result = projectTaskRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TASK_NOT_FOUND);
            }

            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(currentUser, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            ProjectTask projectTask = result.get();
            if (!projectTask.getName().equals(newProjectTask.getName())) {
                if (projectTaskRepository.existsByName(newProjectTask.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            projectTask.setName(newProjectTask.getName());
            projectTask.setDescription(newProjectTask.getDescription());
            projectTask.setProjectTypes(newProjectTask.getProjectTypes());

            User user = userRepository.findById(currentUser.getId()).get();
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            projectTask.setUpdatedBy(user);

            ProjectTask updatedProjectTask = projectTaskRepository.save(projectTask);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_TASK_UPDATED_OK,
                    updatedProjectTask);

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

    @JsonView(Views.GetInfo.class)
    @GetMapping("/{id}/byType")
    public ResponseEntity<?> getTasksByProjectType(@PathVariable Long id) {
        try {
            List<Long> projectTasksIds = projectTaskRepository.findTasksByProjectTypeId(id);
            List<ProjectTask> prokectTasks = projectTaskRepository.findAllById(projectTasksIds);

            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_TASK_CREATED_OK,
                    prokectTasks);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

}
