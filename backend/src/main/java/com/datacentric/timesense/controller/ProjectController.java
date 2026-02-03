package com.datacentric.timesense.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.datacentric.exceptions.DataCentricException;
import com.datacentric.timesense.model.AuditableTable;
import com.datacentric.timesense.model.Client;
import com.datacentric.timesense.model.Project;
import com.datacentric.timesense.model.ProjectAssignment;
import com.datacentric.timesense.model.ProjectTask;
import com.datacentric.timesense.model.ProjectType;
import com.datacentric.timesense.model.Status;
import static com.datacentric.timesense.model.SystemAccessTypes.CREATE_PROJECTS;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.ClientRepository;
import com.datacentric.timesense.repository.HolidayRepository;
import com.datacentric.timesense.repository.ProjectAssignmentRepository;
import com.datacentric.timesense.repository.ProjectRepository;
import com.datacentric.timesense.repository.ProjectTaskRepository;
import com.datacentric.timesense.repository.ProjectTypeRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.TimeRecordRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.hibernate.Message;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.FileParseUtils;
import com.datacentric.utils.imports.ColumnDescriptor;
import com.datacentric.utils.imports.ColumnType;
import com.datacentric.utils.imports.CsvImporter;
import com.datacentric.utils.imports.CsvImporter.CsvImporterConfiguration;
import com.datacentric.utils.rest.BasicFilterSpecification;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private Logger log = LoggerFactory.getLogger(ProjectController.class);

    private interface Views {

        interface GetBasicInfo extends Project.Views.Basic, User.Views.Basic,
                ProjectAssignment.Views.Public {
        }

        interface GetProjects extends Project.Views.Public, User.Views.Public,
                JsonViewPage.Views.Public, ProjectType.Views.Public, Client.Views.Public,
                Status.Views.Public, AuditableTable.Views.List, ProjectTask.Views.Basic {
        }

        interface GetProject extends Project.Views.Complete, User.Views.Complete,
                JsonViewPage.Views.Public, ProjectType.Views.Public, Client.Views.Public,
                Status.Views.Public, AuditableTable.Views.List, ProjectTask.Views.Basic {
        }
    }

    private ProjectAssignmentRepository projectAssignmentRepository;
    private ProjectTaskRepository projectTaskRepository;
    private ProjectTypeRepository projectTypeRepository;
    private TimeRecordRepository timeRecordRepository;
    private HolidayRepository holidayRepository;
    private ProjectRepository projectRepository;
    private UserRoleRepository userRoleRepository;
    private ClientRepository clientRepository;
    private StatusRepository statusRepository;
    private UserRepository userRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String TIME_ZONE = "Europe/Lisbon";
    private static final int BUSINESS_DAY_HOURS = 8;
    private static final String PROJ_RESOURCE_TYPE = "Project";
    private static final String ROLE_USER = "User";
    private static final String CLOSED_STATUS = "FINISHED";
    private static final String HOURS_STR = "hours";
    private static final String COST_STR = "cost";
    private static final String CSV_DELIMITER = ",";
    private static final String CONTENT = "content";
    private static final String TOTAL_ELEMENTS = "totalElements";
    private static final String TOTAL_PAGES = "totalPages";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";
    private static final String ID = "id";
    private static final String FIELD_PROJECT_CODE = "proj_code";
    private static final String FIELD_PROJECT_NAME = "proj_name";
    private static final String FIELD_PROJECT_TYPE = "proj_type";
    private static final String FIELD_PROJECT_MANAGER = "manager";
    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_START_DATE = "start_date";
    private static final String FIELD_EXPECTED_DUE_DATE = "expected_due_date";
    private static final String FIELD_BUDGET = "budget";
    private static final String SCOPE_USER = "SCOPE-USER";
    private static final String SCOPE_COMPANY = "SCOPE-COMPANY";
    private static final String COLUMN_PROJECT = "project";

    // The Byte Order Mark (BOM) for UTF-8 encoding notifies applications that
    // the file is encoded in UTF-8. This helps prevent issues with applications
    // that may misinterpret the encoding, especially when dealing with special
    // characters.
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // CHECKSTYLE.OFF: ParameterNumber
    @Autowired
    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository,
            ProjectTypeRepository projectTypeRepository,
            UserUtils userUtils, SecurityUtils securityUtils,
            TimeRecordRepository timeRecordRepository,
            ProjectAssignmentRepository projectAssignmentRepository,
            UserRoleRepository userRoleRepository, StatusRepository statusRepository,
            HolidayRepository holidayRepository, ClientRepository clientRepository,
            ProjectTaskRepository projectTaskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
        this.projectTypeRepository = projectTypeRepository;
        this.timeRecordRepository = timeRecordRepository;
        this.projectAssignmentRepository = projectAssignmentRepository;
        this.userRoleRepository = userRoleRepository;
        this.statusRepository = statusRepository;
        this.holidayRepository = holidayRepository;
        this.clientRepository = clientRepository;
        this.projectTaskRepository = projectTaskRepository;
    }
    // CHECKSTYLE.ON: ParameterNumber

    @JsonView(Views.GetProjects.class)
    @GetMapping
    public JsonViewPage<Project> getAllProjects(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "name", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter,
            @RequestParam(required = false) List<Long> statusFilter,
            @RequestParam(required = false) List<Long> clientFilter,
            @RequestParam(required = false) List<Long> managerFilter,
            @RequestParam(defaultValue = SCOPE_COMPANY, required = false) String scope) {

        if (numRows <= 0) {
            numRows = DEFAULT_PAGE_SIZE;
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, MAX_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        Specification<Project> spec = Specification.where(null);

        if (!filter.isEmpty()) {
            spec = spec.and(RestUtils.getSpecificationFromFilter("basic", filter));
        }

        spec = spec
                .and(BasicFilterSpecification.applyFilterSpec(statusFilter, "status", ID))
                .and(BasicFilterSpecification.applyFilterSpec(clientFilter, FIELD_CLIENT, ID))
                .and(BasicFilterSpecification.applyFilterSpec(managerFilter,
                        FIELD_PROJECT_MANAGER, ID));

        if (scope.equals(SCOPE_USER)) {
            Long userId = currentUser.getId();
            spec = spec.and(userProjectRestriction(userId));
        }

        return new JsonViewPage<>(projectRepository.findAll(spec, pageable));
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        try {
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                    !canEditProject(id, currentUser) &&
                    !Objects.equals(project.getManager().getId(), currentUser.getId())) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            return ResponseEntity.ok(project);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }
    }

    @JsonView(Views.GetProjects.class)
    @GetMapping("/{projectId}/projectTasks")
    public ResponseEntity<?> getProjectTasks(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            List<ProjectTask> projectTasks = project.getProjectTasks();

            if (projectTasks == null || projectTasks.isEmpty()) {
                List<Long> projectTasksIds = projectTaskRepository
                    .findTasksByProjectTypeId(project.getProjectType().getId());
                projectTasks = projectTaskRepository.findAllById(projectTasksIds);
            }

            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_TASK_CREATED_OK,
                    projectTasks);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/last-used")
    public ResponseEntity<?> getLastUsedProjectsByUser() {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            Timestamp twoWeeksAgoDate = Timestamp.valueOf(LocalDateTime.now().minusWeeks(2));

            User user = userRepository.findById(currentUser.getId()).get();
            List<Project> projectsList = projectRepository
                    .findUserTimeRecordsFromDate(twoWeeksAgoDate, user.getId());

            return ResponseEntity.ok(projectsList);

        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}/costByDay")
    public ResponseEntity<?> getProjectCostByDay(@PathVariable Long id,
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        Page<Object[]> projectCostsByDay;
        projectCostsByDay = timeRecordRepository.getDailyProjectCosts(id,
                    firstRow, numRows, sort,
                    reporterFilter, startDateFilter, endDateFilter, false);

        List<Map<String, Object>> content = projectCostsByDay.getContent().stream().map(row -> {
            int i = 0;
            Map<String, Object> map = new HashMap<>();
            map.put("userName", row[i++]);
            map.put("task", row[i++]);
            map.put("description", row[i++]);
            map.put(HOURS_STR, convertToBigDecimal(row[i++]));
            map.put(COST_STR, convertToBigDecimal(row[i++]));
            map.put(FIELD_START_DATE, (Instant) row[i++]);
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put(CONTENT, content);
        response.put(TOTAL_ELEMENTS, projectCostsByDay.getTotalElements());
        response.put(TOTAL_PAGES, projectCostsByDay.getTotalPages());
        response.put(PAGE, projectCostsByDay.getNumber());
        response.put(PAGE_SIZE, projectCostsByDay.getSize());

        return ResponseEntity.ok(response);
    }

    // TODO: Export exports all records VS exports records according to filters ?
    @PostMapping("/{id}/costByDay/export")
    public ResponseEntity<?> exportProjectCostByDayAsCSV(
            @PathVariable Long id,
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        Page<Object[]> projectCostsByDay = timeRecordRepository.getDailyProjectCosts(
                id, firstRow, numRows, sort, reporterFilter, startDateFilter, endDateFilter, true);

        StringBuilder csvBuilder = new StringBuilder();

        // CHECKSTYLE.OFF: MultipleStringLiterals
        // CSV header
        csvBuilder.append(BYTE_ORDER_MARK);
        csvBuilder.append("ProjectCode,Reporter,StartDate,Task,Description,Hours,Cost \n");

        for (Object[] row : projectCostsByDay) {
            int col = 0;
            String userName = (String) row[col++];
            String taskName = (String) row[col++];
            String description = (String) row[col++];
            BigDecimal hours = convertToBigDecimal(row[col++]);
            BigDecimal cost = convertToBigDecimal(row[col++]);
            Instant startDateInst = (Instant) row[col++];
            // Convert Instant to LocalDate in system default timezone
            String startDate = startDateInst != null
                    ? startDateInst.atZone(ZoneId.systemDefault())
                            .toLocalDate().format(dateFormatter)
                    : "";

            csvBuilder
                    .append(FileParseUtils.escapeCsv(project.getName())).append(CSV_DELIMITER)
                    .append(FileParseUtils.escapeCsv(userName)).append(CSV_DELIMITER)
                    .append(FileParseUtils.escapeCsv(startDate)).append(CSV_DELIMITER)
                    .append(FileParseUtils.escapeCsv(taskName)).append(CSV_DELIMITER)
                    .append(FileParseUtils.escapeCsv(description)).append(CSV_DELIMITER)
                    .append(hours != null ? hours.toPlainString() : "").append(CSV_DELIMITER)
                    .append(cost != null ? cost.toPlainString() : "")
                    .append("\n");
        }

        byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"project-cost-by-day.csv\"");
        headers.set(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}/costByWeek")
    public ResponseEntity<?> getProjectCostByWeek(@PathVariable Long id,
            @RequestParam(required = false) boolean includeReporter) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        List<Object[]> projectCostsByWeek;
        if (includeReporter) {
            projectCostsByWeek = timeRecordRepository.getWeeklyProjectCostsWithUser(id);
        } else {
            projectCostsByWeek = timeRecordRepository.getWeeklyProjectCosts(id);
        }
        List<Map<String, Object>> costByWeek = new ArrayList<>();

        for (Object[] row : projectCostsByWeek) {
            User user = null;
            Instant startWeek = null;
            BigDecimal hours = null;
            BigDecimal cost = null;
            int column = 0;
            if (includeReporter) {
                user = userRepository.findById((Long) row[column++]).orElse(null);
            }
            startWeek = (Instant)row[column++];
            Object hoursObj = row[column++];
            Object costObj = row[column++];

            hours = convertToBigDecimal(hoursObj);
            cost = convertToBigDecimal(costObj);
            Map<String, Object> costMap = new HashMap();
            costMap.put("user", user);
            costMap.put("startWeek", startWeek);
            costMap.put(HOURS_STR, hours);
            costMap.put(COST_STR, cost);
            costByWeek.add(costMap);
        }

        return ResponseEntity.ok(costByWeek);
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}/costByMonthPage")
    public ResponseEntity<?> getProjectCostByMonthPage(@PathVariable Long id,
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        Page<Object[]> projectCostsByWeekPage = timeRecordRepository
            .getMonthlyProjectCostsWithUserFiltered(id, firstRow, numRows, sort,
                    reporterFilter, startDateFilter, endDateFilter);

        List<Map<String, Object>> content =
            projectCostsByWeekPage.getContent().stream().map(row -> {
                int i = 0;
                Map<String, Object> map = new HashMap<>();

                Long userId = (Long) row[i++];
                Instant month = (Instant) row[i++];
                Object hoursObj = row[i++];
                Object costObj = row[i++];

                map.put("user", userRepository.findById(userId).orElse(null));
                map.put("month", month);
                map.put(HOURS_STR, convertToBigDecimal(hoursObj));
                map.put(COST_STR, convertToBigDecimal(costObj));

                return map;
            }
        ).toList();

        Map<String, Object> response = new HashMap<>();
        response.put(CONTENT, content);
        response.put(TOTAL_ELEMENTS, projectCostsByWeekPage.getTotalElements());
        response.put(TOTAL_PAGES, projectCostsByWeekPage.getTotalPages());
        response.put(PAGE, projectCostsByWeekPage.getNumber());
        response.put(PAGE_SIZE, projectCostsByWeekPage.getSize());

        return ResponseEntity.ok(response);
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}/costByWeekPage")
    public ResponseEntity<?> getProjectCostByWeekPage(@PathVariable Long id,
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        Page<Object[]> projectCostsByWeekPage = timeRecordRepository
            .getWeeklyProjectCostsWithUserFiltered(id, firstRow, numRows, sort,
                    reporterFilter, startDateFilter, endDateFilter);

        List<Map<String, Object>> content =
            projectCostsByWeekPage.getContent().stream().map(row -> {
                int i = 0;
                Map<String, Object> map = new HashMap<>();

                Long userId = (Long) row[i++];
                Instant startWeek = (Instant) row[i++];
                Object hoursObj = row[i++];
                Object costObj = row[i++];

                map.put("user", userRepository.findById(userId).orElse(null));
                map.put("startWeek", startWeek);
                map.put(HOURS_STR, convertToBigDecimal(hoursObj));
                map.put(COST_STR, convertToBigDecimal(costObj));

                return map;
            }
        ).toList();

        Map<String, Object> response = new HashMap<>();
        response.put(CONTENT, content);
        response.put(TOTAL_ELEMENTS, projectCostsByWeekPage.getTotalElements());
        response.put(TOTAL_PAGES, projectCostsByWeekPage.getTotalPages());
        response.put(PAGE, projectCostsByWeekPage.getNumber());
        response.put(PAGE_SIZE, projectCostsByWeekPage.getSize());

        return ResponseEntity.ok(response);
    }

    @JsonView(Views.GetProject.class)
    @GetMapping("/{id}/budget")
    public ResponseEntity<?> getProjectBudget(@PathVariable Long id) {

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                !canEditProject(id, currentUser) &&
                !Objects.equals(project.getManager().getId(), currentUser.getId())) {

            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        ZoneId zone = ZoneId.of(TIME_ZONE);
        Double projectBudget = 0.0;
        List<ProjectAssignment> projectAssignments = projectAssignmentRepository
                .getProjectAssignments(id);

        LocalDate projStartDate = project.getStartDate().toInstant().atZone(zone).toLocalDate();
        LocalDate projEndDate;

        // TODO: Calculate the maximum end date from all assignments end dates
        // for the project end date. Right now I just wanted to prevent an NPE
        if (project.getExpectedDueDate() == null) {
            projEndDate = projStartDate;
        } else {
            projEndDate = project.getExpectedDueDate().toInstant().atZone(zone)
                .toLocalDate();
        }

        List<LocalDate> holidaysList = holidayRepository
                .findAllHolidaysDatesByDateInterval(projStartDate, projEndDate);

        for (ProjectAssignment pa : projectAssignments) {

            if (pa.getUser() == null || pa.getUser().getJobTitle() == null) {
                log.warn("INCONSISTENCY! Skipping project assignment id {} for project id {} "
                        + "because user or user job title is null",
                        pa.getId(), id);
                continue;
            }
            double rate = pa.getUser().getJobTitle().getRate();

            if (pa.getStartDate() == null || pa.getEndDate() == null) {
                log.warn("INCONSISTENCY! Skipping project assignment id {} for project id {} "
                        + "because start date or end date is null",
                        pa.getId(), id);
                continue;
            }

            Long businessDaysAllocated = countBusinessDaysBetween(
                    pa.getStartDate().toInstant().atZone(zone).toLocalDate(),
                    pa.getEndDate().toInstant().atZone(zone).toLocalDate(),
                    holidaysList);
            Double userDaysAllocation = businessDaysAllocated * BUSINESS_DAY_HOURS
                    * pa.getAllocationPercentage();
            Double userCostAllocation = userDaysAllocation * rate;
            projectBudget += userCostAllocation;
        }
        return ResponseEntity.ok(projectBudget);
    }

    // TODO: Add condition to return weekly or daily allocations?
    @JsonView(Views.GetBasicInfo.class)
    @GetMapping("/{id}/user-allocations")
    public ResponseEntity<?> getProjectUserAllocations(@PathVariable Long id) {
        // UserSecurityData currentUser = userUtils.getOrCreateUser();
        // TODO: Add permissioms validation

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
        }

        List<ProjectAssignment> projectAssignments = projectAssignmentRepository
                .getProjectAssignments(id);

        for (ProjectAssignment pa : projectAssignments) {
            // set start and end dates to the first day of the week they are representing
            Timestamp startDayOfWeekStart = getStartOfWeek(pa.getStartDate());
            Timestamp startDayOfWeekEnd = getStartOfWeek(pa.getEndDate());
            pa.setStartDate(startDayOfWeekStart);
            pa.setEndDate(startDayOfWeekEnd);
        }

        return ResponseEntity.ok(projectAssignments);
    }

    @JsonView(Views.GetProject.class)
    @PostMapping
    @Transactional
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            List<ValidationFailure> validationErrors = project.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }
            if (projectRepository.findByCode(project.getName()).isPresent()) {
                return I18nResponses.httpResponseWithData(HttpStatus.CONFLICT,
                        MessagesCodes.PROJECT_CODE_CONFLICT,
                        project.getName());
            }

            if (!userRepository.existsById(project.getManager().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            if (!projectTypeRepository.existsById(project.getProjectType().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
            }

            currentUser.markCreatedBy(project);
            currentUser.markUpdatedBy(project);
            if (project.getStartDate() == null) {
                project.setStartDate(new Timestamp(System.currentTimeMillis()));
            }
            Project savedProject = projectRepository.save(project);

            securityUtils.addUserPermission(PROJ_RESOURCE_TYPE, savedProject.getId(),
                Project.ProjectPermission.EDIT_PROJECTS.toString(), project.getManager());

            if (currentUser.getId() != project.getManager().getId()) {
                securityUtils.addPermission(PROJ_RESOURCE_TYPE, savedProject.getId(),
                    Project.ProjectPermission.EDIT_PROJECTS.toString(), currentUser);
            }

            UserRole userRole = userRoleRepository.findByName(ROLE_USER);

            securityUtils.addRolePermission(PROJ_RESOURCE_TYPE, savedProject.getId(),
                Project.ProjectPermission.RECORD_TIME_PROJECTS.toString(), userRole, currentUser);

            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.PROJECT_CREATED_OK,
                    savedProject);
        } catch (HttpMessageNotReadableException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            log.error("Error creating project! {} ", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<Project> result = projectRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            projectRepository.deleteById(id);
            return I18nResponses.accepted(MessagesCodes.PROJECT_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetProject.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProjectById(@PathVariable Long id,
            @RequestBody Project newProject) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                    !canEditProject(id, currentUser) &&
                    !Objects.equals(id, currentUser.getId())) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<Project> result = projectRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }
            Project project = result.get();
            if (!project.getName().equals(newProject.getName())) {
                // Check if the new project code is not already in the database
                Optional<Project> projectWithSameCode = projectRepository
                        .findByCode(newProject.getName());
                if (projectWithSameCode.isPresent()) {
                    return I18nResponses.httpResponseWithData(HttpStatus.CONFLICT,
                            MessagesCodes.PROJECT_CODE_CONFLICT,
                            newProject.getName());
                }
            }

            project.setName(newProject.getName());
            project.setDescription(newProject.getDescription());
            project.setProjectType(newProject.getProjectType());
            project.setManager(newProject.getManager());
            project.setClient(newProject.getClient());
            project.setStartDate(newProject.getStartDate());
            project.setExpectedDueDate(newProject.getExpectedDueDate());
            project.setEndDate(newProject.getEndDate());
            project.setStatus(newProject.getStatus());
            project.setRealBudget(newProject.getRealBudget());
            project.setProjectTasks(newProject.getProjectTasks());

            List<ValidationFailure> validationErrors = project.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }
            if (!userRepository.existsById(project.getManager().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            if (!projectTypeRepository.existsById(project.getProjectType().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TYPE_NOT_FOUND);
            }

            currentUser.markUpdatedBy(project);
            Project updatedProject = projectRepository.save(project);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_UPDATED_OK,
                    updatedProject);

        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            log.error("Error updating project! {}", e.getMessage());
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetProject.class)
    @PatchMapping("/{id}/close")
    public ResponseEntity<?> closeProject(@PathVariable Long id) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS) &&
                    !canEditProject(id, currentUser) &&
                    !Objects.equals(id, currentUser.getId())) {

                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<Project> result = projectRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            Status closedStatus = statusRepository.findByName(CLOSED_STATUS);

            Project project = result.get();
            project.setEndDate(new Timestamp(System.currentTimeMillis()));
            project.setStatus(closedStatus);

            currentUser.markUpdatedBy(project);
            projectRepository.save(project);
            return I18nResponses.httpResponse(HttpStatus.ACCEPTED,
                    MessagesCodes.PROJECT_CLOSED_OK);

        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (Exception e) {
            log.error("Error closing project! {}", e.getMessage());
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importProjects(@RequestParam(value = "file") MultipartFile file)
            throws DataCentricException {

        if (file == null || file.isEmpty()) {
            Message message = new Message(MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR,
                    List.of("The file passed is null or empty!"));
            return I18nResponses.httpResponseWithData(HttpStatus.BAD_REQUEST,
                    MessagesCodes.IMPORT_PROJ_CSV_ERROR, message);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, CREATE_PROJECTS)) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        log.info("Importing projects from csv file: {} ", file.getOriginalFilename());
        CsvImporter importer = new CsvImporter();
        CsvImporterConfiguration config = new CsvImporterConfiguration(CSV_DELIMITER, true, null)
                .withColumnDescriptors(
                        new ColumnDescriptor(FIELD_PROJECT_CODE, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_PROJECT_NAME, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_PROJECT_TYPE, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_START_DATE, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_PROJECT_MANAGER, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_CLIENT, ColumnType.STRING, false),
                        new ColumnDescriptor(FIELD_EXPECTED_DUE_DATE, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_BUDGET, ColumnType.STRING, true));
        importer.initialize(config);

        final List<Project> projectsList = new ArrayList<>();
        AtomicInteger rowNumber = new AtomicInteger(0);
        try {
            importer.importDataWithCustomCallback(file.getInputStream(), data -> {
                try {
                    rowNumber.incrementAndGet();

                    Map<String, Object> row = (Map<String, Object>) data;
                    Project project = new Project();
                    String projCode = (String) row.get(FIELD_PROJECT_CODE);
                    project.setName(projCode);

                    Optional<Project> p = projectRepository.findByCode(projCode);
                    if (!p.isEmpty()) {
                        // TODO: Skip or update existing projects ?
                        return;
                    }

                    project.setDescription((String) row.get(FIELD_PROJECT_NAME));

                    String typeName = (String) row.get(FIELD_PROJECT_TYPE);
                    ProjectType type = projectTypeRepository.findByName(typeName)
                            .orElseThrow(() ->
                                new DataCentricException("Project type not found: " + typeName));

                    project.setProjectType(type);

                    project.setStartDate(parseTimestamp((String) row.get(FIELD_START_DATE)));
                    String userName = (String) row.get(FIELD_PROJECT_MANAGER);
                    User projManager = userRepository.findByName(userName)
                            .orElseThrow(() ->
                                new DataCentricException("Manager not found: " + userName));
                    project.setManager(projManager);

                    String clientName = (String) row.get(FIELD_CLIENT);
                    Client client = clientRepository.findByName(clientName)
                        .orElseThrow(() ->
                            new DataCentricException("Client not found: " + clientName));
                    project.setClient(client);

                    project.setExpectedDueDate(parseTimestamp(
                            (String) row.get(FIELD_EXPECTED_DUE_DATE)));
                    String budgetStr = (String) row.get(FIELD_BUDGET);
                    if (budgetStr != null && !budgetStr.equals("")) {
                        project.setRealBudget(Double.parseDouble(
                            (String) row.get(FIELD_BUDGET)));
                    }

                    Status openStatus = statusRepository.findByName("OPEN");
                    project.setStatus(openStatus);
                    currentUser.markCreatedBy(project);
                    currentUser.markUpdatedBy(project);

                    List<ValidationFailure> validationErrors = project.getValidationFailures();
                    if (!validationErrors.isEmpty()) {
                        log.error("Validation error {}", validationErrors);
                        throw new DataCentricException("Validation failed at row " +
                                rowNumber.get());
                    }

                    projectsList.add(project);

                } catch (Exception e) {
                    throw new RuntimeException(
                            "Error processing row " + rowNumber.get() + ": " + e.getMessage(), e);
                }
            });

            if (!projectsList.isEmpty()) {
                projectRepository.saveAll(projectsList);
            }

            Message successMessage = new Message(MessagesCodes.PROJECT_CREATED_OK,
                    List.of("Imported jobs successfully!"));
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.PROJECT_CREATED_OK,
                    successMessage);

        } catch (IOException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error reading file: {}", ex.getMessage());
            return I18nResponses.badRequest(MessagesCodes.IMPORT_PROJ_CSV_ERROR);
        }
    }

    private static Long countBusinessDaysBetween(LocalDate start, LocalDate end,
            List<LocalDate> holidays) {
        return start.datesUntil(end.plusDays(1))
                .filter(d -> !d.getDayOfWeek().equals(DayOfWeek.SATURDAY) &&
                        !d.getDayOfWeek().equals(DayOfWeek.SUNDAY) &&
                        (holidays == null || !holidays.contains(d)))
                .count();
    }

    private static Timestamp getStartOfWeek(Timestamp timestamp) {
        ZoneId zone = ZoneId.of(TIME_ZONE);
        LocalDateTime dateTime = timestamp.toInstant().atZone(zone).toLocalDateTime();
        LocalDateTime monday = dateTime
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return Timestamp.valueOf(monday);
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        } else if (value instanceof Number) {
            return new BigDecimal(((Number) value).toString());
        } else {
            throw new IllegalArgumentException("Cannot convert value to BigDecimal: " + value);
        }
    }

    private Timestamp parseTimestamp(String date) {
        if (date == null) {
            log.warn("Trying to parse null date!");
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return new Timestamp(sdf.parse(date).getTime());
        } catch (IllegalArgumentException e) {
            log.error("Invalid date format: {}", date);
            return null;
        } catch (ParseException ignored) {
            log.error("Invalid date format: {}", date);
            return null;
        }
    }

    private Specification<Project> userProjectRestriction(Long userId) {
        return (root, query, cb) -> {

            // user is the manager
            Predicate isManager = cb.equal(root.get(FIELD_PROJECT_MANAGER).get(ID), userId);

            // subquery to check if user is assigned through ProjectAssignment
            // use exists subquery because there is no direct mapping to project
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProjectAssignment> pa = subquery.from(ProjectAssignment.class);

            subquery.select(pa.get(COLUMN_PROJECT).get(ID))
                    .where(
                            cb.equal(pa.get(COLUMN_PROJECT).get(ID), root.get(ID)),
                            cb.equal(pa.get("user").get(ID), userId)
                );

            Predicate isAssigned = cb.exists(subquery);

            return cb.or(isManager, isAssigned);
        };
    }

    private boolean canEditProject(Long id, UserSecurityData currentUser) {
        return securityUtils.hasPermissionByIds(PROJ_RESOURCE_TYPE, id, currentUser,
                        Project.ProjectPermission.EDIT_PROJECTS.toString());
    }
}
