package com.datacentric.timesense.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
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

import com.datacentric.timesense.controller.payloads.BatchUpdateRequest;
import com.datacentric.timesense.controller.payloads.TimeRecordPatch;
import com.datacentric.timesense.model.AuditableTable;
import com.datacentric.timesense.model.Project;
import com.datacentric.timesense.model.ProjectTask;
import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.SystemSetting;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.TimeRecord;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.ProjectRepository;
import com.datacentric.timesense.repository.ProjectTaskRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.TimeRecordRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.FileParseUtils;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/time-records")
public class TimeRecordController {

    private Logger log = LoggerFactory.getLogger(TimeRecordController.class);
    private static final String PROJECT = "Project";
    private static final String REQUIRED_PERMISSION_RECORD =
        Project.ProjectPermission.RECORD_TIME_PROJECTS.toString();
    private static final String REQUIRED_PERMISSION_EDIT =
        Project.ProjectPermission.EDIT_PROJECTS.toString();
    private static final String REQUIRED_PERMISSION_MANAGE =
        Project.ProjectPermission.TIME_APPROVAL.toString();

    private interface Views {

        interface GetBasicInfo extends TimeRecord.Views.Public, User.Views.Basic,
                Project.Views.Public, Status.Views.Public, ProjectTask.Views.Basic {
        }

        interface GetTimeRecords extends TimeRecord.Views.Public, User.Views.Public,
                Project.Views.Public, JsonViewPage.Views.Public, Status.Views.Public,
                AuditableTable.Views.List, ProjectTask.Views.Basic {
        }

        interface GetTimeRecord extends TimeRecord.Views.Complete,
                User.Views.Public, Project.Views.Public, Status.Views.Public,
                ProjectTask.Views.Basic {
        }
    }

    private TimeRecordRepository timeRecordRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private ProjectTaskRepository projectTaskRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;
    private StatusRepository statusRepository;
    private SystemSettingRepository systemSettingRepository;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DENIED = "DENIED";
    private static final String STATUS_DRAFT = "DRAFT";

    private static final String CMD_APPROVE = "approve";
    private static final String CMD_DENY = "deny";
    private static final String CMD_DRAFT = "draft";

    private static final String ADMIN_ROLE = "Admin";
    private static final String MANAGER_ROLE = "Manager";
    private static final String ID = "id";
    private static final String CONTENT = "content";
    private static final String TOTAL_ELEMENTS = "totalElements";
    private static final String TOTAL_PAGES = "totalPages";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";
    private static final String COMMA_SEPARATOR = ",";
    private static final String SEMICOLON_SEPARATOR = ";";
    private static final String SCOPE_TEAM = "SCOPE-TEAM";
    private static final String SCOPE_USER = "SCOPE-USER";
    private static final String PROJECT_NAME = "ProjectName";
    private static final String MANAGER = "manager";
    private static final String LINE_MANAGER = "lineManager";
    private static final String LINE_MANAGER_ID = "lineManagerId";


    private static final String COMPANY_SCOPE = "company";
    private static final String MY_TEAMS_SCOPE = "my_teams";
    private static final String MY_PROJECT_SCOPE = "my_projects";


    private static final String ERR_PROJECT = "PROJECT";
    private static final String PROJECT_FIELD = "project";
    private static final String TYPE_FIELD = "type";
    private static final String USER_FIELD = "user";

    private static final String ERR_TASK = "TASK";

    // The Byte Order Mark (BOM) for UTF-8 encoding notifies applications that
    // the file is encoded in UTF-8. This helps prevent issues with applications
    // that may misinterpret the encoding, especially when dealing with special
    // characters.
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    public TimeRecordController(TimeRecordRepository timeRecordRepository,
            UserRepository userRepository, ProjectRepository projectRepository,
            ProjectTaskRepository projectTaskRepository,
            UserUtils userUtils, SecurityUtils securityUtils,
            StatusRepository statusRepository, SystemSettingRepository systemSettingRepository) {
        this.timeRecordRepository = timeRecordRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
        this.statusRepository = statusRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @JsonView(Views.GetTimeRecords.class)
    @GetMapping
    public JsonViewPage<TimeRecord> getAllTimeRecords(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = ID, required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter,
            @RequestParam(defaultValue = COMPANY_SCOPE, required = false) String scope) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        boolean isAdmin = userRoles.contains(ADMIN_ROLE);

        Specification<TimeRecord> finalSpec = null;

        if (!filter.isEmpty()) {
            finalSpec = RestUtils.getSpecificationFromFilter("basic", filter);
        }

        if (!isAdmin) {
            // A "normal user can see for approval the records that he manages
            Specification<TimeRecord> isProjectManagerSpec = (root, query, cb) ->
                cb.equal(root.get(PROJECT_FIELD).get(MANAGER).get(ID), user.getId());

            Specification<TimeRecord> isTeamManagerSpec = (root, query, cb) ->
                cb.and(
                    cb.isTrue(root.get(PROJECT_FIELD).get(TYPE_FIELD).get(LINE_MANAGER)),
                    cb.equal(root.get(USER_FIELD).get(LINE_MANAGER_ID), user.getId())
                );

            finalSpec = (finalSpec != null)
                ? finalSpec.and(isProjectManagerSpec.or(isTeamManagerSpec))
                : isProjectManagerSpec.or(isTeamManagerSpec);
        }

        Specification<TimeRecord> isProjectManagerSpec = (root, query, cb) ->
                cb.equal(root.get(PROJECT_FIELD).get(MANAGER).get(ID), user.getId());

        Specification<TimeRecord> isTeamManagerSpec = (root, query, cb) ->
                cb.and(
                        cb.isTrue(root.get(PROJECT_FIELD).get(TYPE_FIELD).get(LINE_MANAGER)),
                        cb.equal(root.get(USER_FIELD).get(LINE_MANAGER_ID), user.getId())
                );

        Specification<TimeRecord> scopeSpec;

        switch (scope.toLowerCase()) {
            case MY_PROJECT_SCOPE:
                scopeSpec = isProjectManagerSpec;
                break;

            case MY_TEAMS_SCOPE:
                scopeSpec = isTeamManagerSpec;
                break;

            case COMPANY_SCOPE:
            default:
                scopeSpec = null;
                break;
        }

        if (scopeSpec != null) {
            finalSpec = (finalSpec != null) ? finalSpec.and(scopeSpec) : scopeSpec;
        }

        return new JsonViewPage<>(
                (finalSpec != null)
                    ? timeRecordRepository.findAll(finalSpec, pageable)
                    : timeRecordRepository.findAll(pageable)
        );
    }

    @JsonView(Views.GetTimeRecords.class)
    @GetMapping("/user-page")
    public JsonViewPage<TimeRecord> getUserTimeRecords(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam Timestamp startDate) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        return new JsonViewPage<>(timeRecordRepository.findByUserIdAndDateRange(
            currentUser.getId(), startDate, pageable));
    }

    @JsonView(Views.GetTimeRecord.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getTimeRecordById(@PathVariable Long id) {
        try {
            TimeRecord timeRecord = timeRecordRepository.findById(id).orElse(null);
            if (timeRecord == null) {
                return I18nResponses.notFound(MessagesCodes.TIME_RECORD_NOT_FOUND);
            }

            return ResponseEntity.ok(timeRecord);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.TIME_RECORD_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetBasicInfo.class)
    @GetMapping("/byUser")
    public ResponseEntity<?> getTimeRecordByUserAndDate(
            @RequestParam Timestamp startDate,
            @RequestParam Timestamp endDate) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            List<TimeRecord> timeRecords = timeRecordRepository.getTimeRecordsByUserAndDate(
                    currentUser.getId(), startDate, endDate);

            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.TIME_RECORD_LIST_OK, timeRecords);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.TIME_RECORD_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the list of time records to be shown in the time approval screen
     * of the frontend.
     *
     * TODO: Consider merging this with the general getAllTimeRecords endpoint.
     */
    @JsonView(Views.GetTimeRecord.class)
    @GetMapping("/filtered")
    public ResponseEntity<?> getFilteredTimeRecords(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> projectFilter,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        boolean isAdmin = userRoles.contains(ADMIN_ROLE);

        List<Long> effectiveReporterFilter = getManagerTeam(user, reporterFilter);

        // Admins can see all records, managers only see their own projects
        Long approverId = isAdmin ? null : currentUser.getId();

        // TODO: Consider only projects where the approver has the approve times role
        Page<Object[]> filteredTimeRecords = timeRecordRepository.getFilteredTimeRecords(
                projectFilter,
                approverId,
                firstRow, numRows, sort,
                effectiveReporterFilter, startDateFilter, endDateFilter, false
        );

        List<Map<String, Object>> content = filteredTimeRecords.getContent().stream().map(row -> {
            int i = 0;
            Map<String, Object> map = new HashMap<>();
            map.put("userName", row[i++]);
            map.put("projectName", row[i++]);
            map.put("projectDescription", row[i++]);
            map.put("taskName", row[i++]);
            map.put("description", row[i++]);
            map.put("hours", convertToBigDecimal(row[i++]));
            map.put("startDate", (Instant) row[i++]);
            map.put("endDate", (Instant) row[i++]);
            map.put("statusName", row[i++]);
            map.put(ID, row[i++]);
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put(CONTENT, content);
        response.put(TOTAL_ELEMENTS, filteredTimeRecords.getTotalElements());
        response.put(TOTAL_PAGES, filteredTimeRecords.getTotalPages());
        response.put(PAGE, filteredTimeRecords.getNumber());
        response.put(PAGE_SIZE, filteredTimeRecords.getSize());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportTimeRecords(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "startDate", required = false) String sort,
            @RequestParam(required = false) List<Long> projectFilter,
            @RequestParam(required = false) List<Long> reporterFilter,
            @RequestParam(required = false) Timestamp startDateFilter,
            @RequestParam(required = false) Timestamp endDateFilter,
            @RequestParam(defaultValue = SCOPE_TEAM, required = false) String scope) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        List<Long> effectiveReporterFilter = new ArrayList<>();
        if (scope.equals(SCOPE_USER)) {
            effectiveReporterFilter.add(user.getId());
        } else {
            effectiveReporterFilter = getManagerTeam(user, reporterFilter);
        }

        String delimiter = COMMA_SEPARATOR;
        NumberFormat numberFormatter = NumberFormat.getInstance(LocaleContextHolder.getLocale());
        if (numberFormatter instanceof DecimalFormat) {
            DecimalFormatSymbols numberSymbols =
                ((DecimalFormat)numberFormatter).getDecimalFormatSymbols();

            delimiter = (numberSymbols.getDecimalSeparator() == ','
                    || numberSymbols.getGroupingSeparator() == ',') ?
                SEMICOLON_SEPARATOR : COMMA_SEPARATOR;
        }

        log.debug("Exporting using the locale: {}", LocaleContextHolder.getLocale());

        Page<Object[]> filteredTimeRecords = timeRecordRepository.getFilteredTimeRecords(
                    projectFilter, null, firstRow, numRows, sort,
                    effectiveReporterFilter, startDateFilter, endDateFilter, true);

        StringBuilder csvBuilder = new StringBuilder();

        // CSV header
        csvBuilder.append(BYTE_ORDER_MARK);
        csvBuilder.append(
                String.join(delimiter,
                    "ProjectCode", PROJECT_NAME, "Reporter",
                    "StartDate", "Task", "Description", "Hours"));
        csvBuilder.append('\n');

        for (Object[] row : filteredTimeRecords) {
            int col = 0;
            String userName = (String) row[col++];
            String projectCode = (String) row[col++];
            String projectName = (String) row[col++];
            String taskName = (String) row[col++];
            String description = (String) row[col++];
            BigDecimal hours = convertToBigDecimal(row[col++]);
            Instant startDateInst = (Instant) row[col++];
            // Convert Instant to LocalDate in system default timezone
            String startDate = startDateInst != null
                    ? startDateInst.atZone(ZoneId.systemDefault())
                            .toLocalDate().format(dateFormatter)
                    : "";

            String hoursText = hours != null ? numberFormatter.format(hours) : "";
            csvBuilder
                    .append(FileParseUtils.escapeCsv(projectCode)).append(delimiter)
                    .append(FileParseUtils.escapeCsv(projectName)).append(delimiter)
                    .append(FileParseUtils.escapeCsv(userName)).append(delimiter)
                    .append(FileParseUtils.escapeCsv(startDate)).append(delimiter)
                    .append(FileParseUtils.escapeCsv(taskName)).append(delimiter)
                    .append(FileParseUtils.escapeCsv(description)).append(delimiter)
                    .append(hoursText).append(delimiter)
                    .append("\n");
        }
        byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"general-time-records.csv\"");
        headers.set(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

    }

    @JsonView(Views.GetTimeRecord.class)
    @PostMapping
    public ResponseEntity<?> createTimeRecord(@RequestBody TimeRecord timeRecord) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasPermissionByIds(PROJECT, timeRecord.getProject().getId(),
                    Arrays.asList(REQUIRED_PERMISSION_RECORD),
                    currentUser.getId(), currentUser.getRoles(), currentUser.getUserGroups())) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Status pendingStatus = statusRepository.findByName(STATUS_PENDING);
            timeRecord.setStatus(pendingStatus);

            if (!projectRepository.existsById(timeRecord.getProject().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }
            if (!userRepository.existsById(timeRecord.getApprovedBy().getId())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }
            timeRecord.setUser(user);

            TimeRecord savedTimeRecord = timeRecordRepository.save(timeRecord);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.TIME_RECORD_CREATED_OK,
                    savedTimeRecord);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetTimeRecord.class)
    @PostMapping("/bulk")
    public ResponseEntity<?> createTimeRecords(@RequestBody List<TimeRecord> timeRecords,
                @RequestParam(name = "submit", defaultValue = "false") boolean submit) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (timeRecords == null || timeRecords.isEmpty()) {

                return I18nResponses.badRequest(
                        MessagesCodes.TIME_RECORD_LIST_ERROR);
            }
            if (!securityUtils.hasPermissionByIds(PROJECT, timeRecords.get(0).getProject().getId(),
                    Arrays.asList(REQUIRED_PERMISSION_RECORD),
                    currentUser.getId(), currentUser.getRoles(), currentUser.getUserGroups())) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return I18nResponses.notFound(
                        MessagesCodes.USER_NOT_FOUND);
            }

            Status pendingStatus = statusRepository.findByName(STATUS_PENDING);
            Status draftStatus = statusRepository.findByName(STATUS_DRAFT);

            SystemSetting maxSetting = systemSettingRepository
                .findByName(SystemSettings.PROJ_DAILLY_MAX_HOURS);
            if (maxSetting == null) {
                return I18nResponses.notFound(
                        MessagesCodes.SYSTEM_SETTING_NOT_FOUND);
            }
            Double maxHours = Double.valueOf(maxSetting.getValue());

            // Step 1: Collect all relevant dates and IDs for bulk query
            Set<Timestamp> dates = timeRecords.stream()
                .map(TimeRecord::getStartDate)
                .collect(Collectors.toSet());
            List<Long> projectIds = timeRecords.stream()
                .map(t -> t.getProject().getId())
                .distinct()
                .collect(Collectors.toList());
            List<Long> taskIds = timeRecords.stream()
                .map(t -> t.getTask().getId())
                .distinct()
                .collect(Collectors.toList());

            // Step 2: Bulk fetch existing records for the user, projects, tasks, and dates
            List<TimeRecord> existingRecords = timeRecordRepository
                .findByUserAndProjectsAndTasksAndDates(
                    user.getId(), projectIds, taskIds, dates);

            // Step 3: Create a map of existing hours by date
            Map<Timestamp, Double> existingHoursByDate = existingRecords.stream()
                .collect(Collectors.groupingBy(
                    TimeRecord::getStartDate,
                    Collectors.summingDouble(TimeRecord::getHours)
                ));

            // Step 4: Process timeRecords and validate hours
            List<TimeRecord> timeRecordsToSave = new ArrayList<>();
            Map<Timestamp, Double> newHoursByDate = new HashMap<>();

            for (TimeRecord t : timeRecords) {
                Optional<TimeRecord> existingTimeRecord = existingRecords.stream()
                    .filter(r -> r.getProject().getId().equals(t.getProject().getId())
                        && r.getTask().getId().equals(t.getTask().getId())
                        && r.getStartDate().equals(t.getStartDate()))
                    .findFirst();

                TimeRecord recordToSave;
                if (existingTimeRecord.isPresent()) {
                    recordToSave = existingTimeRecord.get();
                    // Subtract existing hours before adding new ones
                    newHoursByDate.compute(
                        t.getStartDate(),
                        (k, v) -> (v == null ? 0.0 : v) - recordToSave.getHours() + t.getHours()
                    );
                    recordToSave.setHours(t.getHours());
                    recordToSave.setEndDate(t.getEndDate());
                    recordToSave.setDescription(t.getDescription());
                    if (submit && t.getStatus().getName().equals(STATUS_DRAFT)) {
                        recordToSave.setStatus(pendingStatus);
                    } else {
                        recordToSave.setStatus(t.getStatus());
                    }
                } else {
                    recordToSave = t;
                    recordToSave.setUser(user);
                    if (submit) {
                        recordToSave.setStatus(pendingStatus);
                    } else {
                        recordToSave.setStatus(draftStatus);
                    }
                    newHoursByDate.merge(t.getStartDate(), t.getHours(), Double::sum);
                }
                timeRecordsToSave.add(recordToSave);

                // Check total hours for the date (existing + new)
                Timestamp date = t.getStartDate();
                double totalHours = existingHoursByDate.getOrDefault(date, 0.0)
                    + newHoursByDate.getOrDefault(date, 0.0);
                if (totalHours > maxHours) {
                    return I18nResponses.badRequest(MessagesCodes.TIME_RECORD_INVALID_HOURS,
                        String.format(
                            "Total hours (%.2f) for date %s exceeds maximum allowed hours (%.2f)",
                            totalHours, date, maxHours));
                }
            }

            // Step 5: Save all valid records
            List<TimeRecord> savedRecords = timeRecordRepository.saveAll(timeRecordsToSave);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.TIME_RECORD_CREATED_OK, savedRecords);
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
    public ResponseEntity<?> deleteTimeRecord(@PathVariable Long id) {
        try {

            Optional<TimeRecord> result = timeRecordRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(
                        MessagesCodes.TIME_RECORD_NOT_FOUND);
            } else {
                UserSecurityData currentUser = userUtils.getOrCreateUser();
                if (!securityUtils.hasPermissionByIds(PROJECT, result.get().getProject().getId(),
                        Arrays.asList(REQUIRED_PERMISSION_RECORD),
                        currentUser.getId(),
                        currentUser.getRoles(), currentUser.getUserGroups())) {
                    return I18nResponses.forbidden(
                            MessagesCodes.PERMISSIONS_DENIED);
                }
            }
            timeRecordRepository.deleteById(id);
            return I18nResponses.accepted(MessagesCodes.TIME_RECORD_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetTimeRecord.class)
    @PutMapping("/batch")
    @Transactional
    public ResponseEntity<?> updateProjectAndTaskBatch(
            @RequestBody BatchUpdateRequest request) {

        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (request.getTimeRecordIds() == null || request.getTimeRecordIds().isEmpty()) {
                return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
            }

            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new NoSuchElementException(ERR_PROJECT));

            ProjectTask projectTask = projectTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new NoSuchElementException(ERR_TASK));

            List<TimeRecord> records = timeRecordRepository.findAllById(request.getTimeRecordIds());

            if (records.size() != request.getTimeRecordIds().size()) {
                return I18nResponses.notFound(MessagesCodes.TIME_RECORD_NOT_FOUND);
            }

            for (TimeRecord tr : records) {
                if (!securityUtils.hasPermissionByIds(
                        PROJECT,
                        tr.getProject().getId(),
                        currentUser,
                        REQUIRED_PERMISSION_RECORD,
                        REQUIRED_PERMISSION_EDIT)) {

                    return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
                }
            }

            for (TimeRecord record : records) {

                if (timeRecordRepository.existsDuplicateCombination(
                        record.getUser().getId(),
                        request.getProjectId(),
                        request.getTaskId(),
                        record.getStartDate(),
                        record.getId()
                    )
                ) {
                    return I18nResponses.httpResponse(HttpStatus.CONFLICT,
                        MessagesCodes.TIME_RECORD_DUPLICATE_CONSTRAINT);
                }
                record.setProject(project);
                record.setTask(projectTask);
            }

            List<TimeRecord> updatedRecords = timeRecordRepository.saveAll(records);

            return I18nResponses.httpResponseWithData(
                    HttpStatus.ACCEPTED,
                    MessagesCodes.TIME_RECORD_UPDATED_OK,
                    updatedRecords
            );

        } catch (NoSuchElementException e) {

            if (ERR_PROJECT.equals(e.getMessage())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_NOT_FOUND);
            }

            if (ERR_TASK.equals(e.getMessage())) {
                return I18nResponses.notFound(MessagesCodes.PROJECT_TASK_NOT_FOUND);
            }

            return I18nResponses.notFound(MessagesCodes.TIME_RECORD_NOT_FOUND);

        } catch (Exception e) {
            log.error("Error in batch update: {}", e.getMessage(), e);
            return I18nResponses.httpResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR
            );
        }
    }

    @JsonView(Views.GetTimeRecord.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTimeRecordById(@PathVariable Long id,
            @RequestBody TimeRecord newTimeRecord,
            @RequestParam(defaultValue = "", required = false) String manage) {
        try {
            Optional<TimeRecord> result = timeRecordRepository.findById(id);
            UserSecurityData currentUser = userUtils.getOrCreateUser();

            if (result.isEmpty()) {
                return I18nResponses.notFound(
                        MessagesCodes.TIME_RECORD_NOT_FOUND);
            } else {
                if (!securityUtils.hasPermissionByIds(PROJECT, result.get().getProject().getId(),
                        currentUser, REQUIRED_PERMISSION_RECORD, REQUIRED_PERMISSION_EDIT,
                                REQUIRED_PERMISSION_MANAGE)) {
                    return I18nResponses.forbidden(
                            MessagesCodes.PERMISSIONS_DENIED);
                }
            }
            TimeRecord timeRecord = result.get();
            if (!manage.equals(CMD_DRAFT)) {
                timeRecord.setUser(newTimeRecord.getUser());
                timeRecord.setProject(newTimeRecord.getProject());
                timeRecord.setTask(newTimeRecord.getTask());
                timeRecord.setHours(newTimeRecord.getHours());
                timeRecord.setDescription(newTimeRecord.getDescription());
                timeRecord.setStatus(newTimeRecord.getStatus());
                timeRecord.setStartDate(newTimeRecord.getStartDate());
                timeRecord.setEndDate(newTimeRecord.getEndDate());
                timeRecord.setApprovedAt(newTimeRecord.getApprovedAt());
                timeRecord.setApprovedBy(newTimeRecord.getApprovedBy());
            }

            // only project manages can modify the record status
            if (!"".equals(manage)) {
                if (!securityUtils.hasPermissionByIds(PROJECT, timeRecord.getProject().getId(),
                        currentUser, REQUIRED_PERMISSION_MANAGE, REQUIRED_PERMISSION_EDIT)) {
                    return I18nResponses.forbidden(
                            MessagesCodes.PERMISSIONS_DENIED);
                }
            }

            if (manage.equals(CMD_APPROVE)) {
                User approver = userRepository.findById(currentUser.getId()).get();
                Status approvedStatus = statusRepository.findByName(STATUS_APPROVED);
                timeRecord.setStatus(approvedStatus);
                timeRecord.setApprovedBy(approver);
                timeRecord.setApprovedAt(new Timestamp(System.currentTimeMillis()));
            } else if (manage.equals(CMD_DENY)) {
                User approver = userRepository.findById(currentUser.getId()).get();
                Status deniedStatus = statusRepository.findByName(STATUS_DENIED);
                timeRecord.setStatus(deniedStatus);
                timeRecord.setReason(newTimeRecord.getReason());
                timeRecord.setApprovedBy(approver);
                timeRecord.setApprovedAt(new Timestamp(System.currentTimeMillis()));
            } else if (manage.equals(CMD_DRAFT)) {
                User approver = userRepository.findById(currentUser.getId()).get();
                Status draftStatus = statusRepository.findByName(STATUS_DRAFT);
                timeRecord.setStatus(draftStatus);
                timeRecord.setApprovedBy(approver);
                timeRecord.setApprovedAt(null);
            }

            List<ValidationFailure> validationErrors = timeRecord.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }
            if (!userRepository.existsById(timeRecord.getUser().getId())) {
                return I18nResponses.notFound(
                        MessagesCodes.USER_NOT_FOUND);
            }
            if (!projectRepository.existsById(timeRecord.getProject().getId())) {
                return I18nResponses.notFound(
                        MessagesCodes.PROJECT_NOT_FOUND);
            }
            if (!userRepository.existsById(timeRecord.getApprovedBy().getId())) {
                return I18nResponses.notFound(
                        MessagesCodes.PROJECT_NOT_FOUND);
            }

            TimeRecord updatedTimeRecord = timeRecordRepository.save(timeRecord);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.TIME_RECORD_UPDATED_OK,
                    updatedTimeRecord);

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

    /**
     * Endpoint to process bulk commands on time records, such as approve, deny,
     * or set to draft.
     */
    @Transactional
    @PatchMapping
    public ResponseEntity<?> patchCommandDispatcher(@RequestBody List<TimeRecordPatch> commands) {
        if (commands == null || commands.isEmpty()) {
            return I18nResponses.badRequest(MessagesCodes.EMPTY_COMMAND_LIST);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        boolean isAdmin = userRoles.contains(ADMIN_ROLE);

        for (TimeRecordPatch command : commands) {
            Status newStatus;
            switch (command.command) {
                case CMD_APPROVE:
                    newStatus = statusRepository.findByName(STATUS_APPROVED);
                    break;
                case CMD_DENY:
                    newStatus = statusRepository.findByName(STATUS_DENIED);
                    break;
                case CMD_DRAFT:
                    newStatus = statusRepository.findByName(STATUS_DRAFT);
                    break;
                default:
                    return I18nResponses.badRequest(MessagesCodes.INVALID_COMMAND);
            }

            // Load all time records before checking
            List<TimeRecord> records = timeRecordRepository.findAllById(command.data.ids);

            // Check user permissions
            for (TimeRecord record : records) {
                Project project = record.getProject();
                if (!isAdmin && (project == null || project.getManager() == null ||
                        !project.getManager().getId().equals(user.getId()))) {
                    return I18nResponses.forbidden(
                            MessagesCodes.TIME_RECORD_PROJECT_ACCESS_DENIED,
                            Map.of(PROJECT_NAME, project != null ? project.getName() : "unknown")
                    );
                }
            }
            // update records
            updateTimeRecords(command.data.ids, newStatus, command.data.reason, user);
        }

        // TODO: Change to a better response returning the response for each command
        return I18nResponses.accepted(MessagesCodes.TIME_RECORD_UPDATED_OK);
    }

    private void updateTimeRecords(List<Long> ids, Status newStatus, String reason, User user) {

        // TODO - Sanity check of the given values
        // TODO - Check for possible errors and handle optimistic locking
        timeRecordRepository.updateTimeRecordStatus(newStatus, reason, ids, user);
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

    /**
     * Returns the list of all users managed by the given user.
     *
     * @param user
     *      The user for whom to get the managed team.
     * @param argUsersFilter
     *      The initial user filter provided as argument.
     *
     * @return
     *    The effective user filter including only the users managed by the given user.
     */
    private List<Long> getManagerTeam (User user, List<Long> argUsersFilter) {
        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        boolean isAdmin = userRoles.contains(ADMIN_ROLE);
        boolean isManager = userRoles.contains(MANAGER_ROLE);

        // Build reporter filter based on role
        List<Long> effectiveReporterFilter = argUsersFilter;
        if (!isAdmin) {
            if (isManager) {
                // Manager sees their own records + their subordinates
                List<Long> managedUsers = userRepository.findManagerTeam(user.getId());
                managedUsers.add(user.getId());
                if (effectiveReporterFilter == null || effectiveReporterFilter.isEmpty()) {
                    effectiveReporterFilter = managedUsers;
                } else {
                    effectiveReporterFilter = effectiveReporterFilter.stream()
                            .filter(managedUsers::contains)
                            .toList();
                }
            } else {
                // Regular user → only their own records
                effectiveReporterFilter = List.of(user.getId());
            }
        }
        return effectiveReporterFilter;
    }
}
