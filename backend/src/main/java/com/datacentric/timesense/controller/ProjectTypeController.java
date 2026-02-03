package com.datacentric.timesense.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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

import com.datacentric.timesense.model.ProjectType;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ProjectTypeRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/project-types")
public class ProjectTypeController {

    public static final class Views {
        interface GetBasicInfo extends User.Views.Basic {
        }

        public interface GetProjectTypes extends ProjectType.Views.Public,
                JsonViewPage.Views.Public {
        }

        public interface GetProjectType extends ProjectType.Views.Complete,
                JsonViewPage.Views.Public {
        }
    }

    private ProjectTypeRepository projectTypeRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public ProjectTypeController(ProjectTypeRepository projectTypeRepository,
            SecurityUtils securityUtils, UserUtils userUtils) {
        this.projectTypeRepository = projectTypeRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "CREATE_PROJECTS";

    @JsonView(Views.GetProjectTypes.class)
    @GetMapping
    public JsonViewPage<ProjectType> getProjectTypes(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<ProjectType> filterSpec = RestUtils
                    .getSpecificationFromFilter("basic", filter);
            return new JsonViewPage<>(projectTypeRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(projectTypeRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetProjectType.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetProjectTypeById(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            ProjectType projectType = projectTypeRepository.findById(id).orElse(null);
            if (projectType == null) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
            }

            return ResponseEntity.ok(projectType);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetBasicInfo.class)
    @PutMapping("/list")
    public ResponseEntity<?> getProjectTypesByIdList(@RequestBody List<Long> projectTypesIds) {
        try {
            // UserSecurityData currentUser = userUtils.getOrCreateUser();
            // TODO: Add permission validation

            List<ProjectType> projectTypes = new ArrayList<>();

            for (Long id : projectTypesIds) {
                Optional<ProjectType> projectType = projectTypeRepository.findById(id);
                if (projectType.isEmpty()) {
                    return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
                }
                projectTypes.add(projectType.get());
            }

            return I18nResponses.httpResponseWithData(HttpStatus.OK,
                    MessagesCodes.USER_CREATED_OK, projectTypes);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetProjectType.class)
    @PostMapping()
    public ResponseEntity<?> createProjectType(@RequestBody ProjectType projectType) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (projectTypeRepository.existsByName(projectType.getName())) {
                return I18nResponses.badRequest(
                        MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            ProjectType newProjectType = projectTypeRepository.save(projectType);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.PROJECT_TYPE_CREATED_OK,
                    newProjectType);
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
    public ResponseEntity<?> deleteProjectType(@PathVariable Long id) {
        try {
            Optional<ProjectType> result = projectTypeRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            boolean isReferencedByProject = projectTypeRepository
                    .existsInProjectByTypeId(id);

            if (isReferencedByProject) {
                return I18nResponses.httpResponse(HttpStatus.CONFLICT,
                        MessagesCodes.TYPE_CONFLICT_PROJECT);
            }

            projectTypeRepository.deleteProjectTypeById(id);
            return I18nResponses.accepted(MessagesCodes.PROJECT_TYPE_DELETED_OK);

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

    @JsonView(Views.GetProjectType.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProjectTypeById(@PathVariable Long id,
            @RequestBody ProjectType newProjectType) {
        try {
            Optional<ProjectType> result = projectTypeRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            ProjectType projectType = result.get();
            if (!projectType.getName().equals(newProjectType.getName())) {
                if (projectTypeRepository.existsByName(newProjectType.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            projectType.setName(newProjectType.getName());
            projectType.setDescription(newProjectType.getDescription());
            projectType.setLineManager(newProjectType.getLineManager());

            ProjectType updatedProjectType = projectTypeRepository.save(projectType);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_TYPE_UPDATED_OK,
                    updatedProjectType);

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
