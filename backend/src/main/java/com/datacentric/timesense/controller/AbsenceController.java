package com.datacentric.timesense.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.datacentric.exceptions.DataCentricException;
import com.datacentric.timesense.controller.payloads.CalendarMatrixData;
import com.datacentric.timesense.controller.payloads.TimeRecordPatch;
import com.datacentric.timesense.model.Absence;
import com.datacentric.timesense.model.AbsenceAttachment;
import com.datacentric.timesense.model.AbsenceSubType;
import com.datacentric.timesense.model.AbsenceType;
import com.datacentric.timesense.model.AuditableTable;
import com.datacentric.timesense.model.Status;
import static com.datacentric.timesense.model.SystemAccessTypes.MANAGE_TIMEOFF;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.AbsenceAttachmentRepository;
import com.datacentric.timesense.repository.AbsenceRepository;
import com.datacentric.timesense.repository.AbsenceSubTypeRepository;
import com.datacentric.timesense.repository.AbsenceTypeRepository;
import com.datacentric.timesense.repository.HolidayRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import static com.datacentric.timesense.utils.TimeoffManagementUtils.hoursToBusinessDays;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.timesense.utils.storage.IStorageProvider;
import com.datacentric.utils.StringUtils;
import com.datacentric.utils.imports.ColumnDescriptor;
import com.datacentric.utils.imports.ColumnType;
import com.datacentric.utils.imports.CsvImporter;
import com.datacentric.utils.imports.CsvImporter.CsvImporterConfiguration;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private static final int BATCH_SIZE = 100;

    private static final String STACK_TRACE = "Stack trace: ";
    private static final String SAVE_USER = "Save user: {}";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_YEAR = "year";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_USER_EMAIL = "userEmail";
    private static final String FIELD_BUSINESS_DAYS = "businessDays";
    private static final String FIELD_STATUS = "status";
    private static final String PARTNER_JOB_TITLE = "Partner";

    private Logger log = LoggerFactory.getLogger(AbsenceController.class);

    private interface Views {

        interface GetBasicInfo extends Absence.Views.Basic, AbsenceType.Views.Minimal,
                User.Views.Basic, Status.Views.Public, CalendarMatrixData.Views.Basic,
                AbsenceSubType.Views.Basic {
        }

        interface GetAbsences extends Absence.Views.Public, AbsenceType.Views.Minimal,
                User.Views.Public, JsonViewPage.Views.Public, Status.Views.Public,
                AuditableTable.Views.Create, AbsenceSubType.Views.Basic {
        }

        interface GetAbsence extends Absence.Views.Complete, AbsenceType.Views.Minimal,
                User.Views.Public, Status.Views.Public, AuditableTable.Views.List,
                AbsenceSubType.Views.Basic {
        }
    }

    private AbsenceRepository absenceRepository;
    private AbsenceTypeRepository absenceTypeRepository;
    private AbsenceSubTypeRepository absenceSubTypeRepository;
    private UserRepository userRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;
    private StatusRepository statusRepository;
    private HolidayRepository holidayRepository;
    private SystemSettingRepository systemSettingRepository;
    private AbsenceAttachmentRepository absenceAttachmentRepository;
    private IStorageProvider storageProvider;

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "100";
    private static final String BASIC = "basic";
    private static final String VACATION = "VACATION";
    private static final String ABSENCE = "ABSENCES";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_DENIED = "DENIED";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String TIME_ZONE = "Europe/Lisbon";
    private static final String CMD_APPROVE = "approve";
    private static final String CMD_PENDING = "pending";
    private static final String CMD_DENY = "deny";
    private static final String ATTR_USER = "user";
    private static final String ADMIN_ROLE = "Admin";
    private static final String MANAGER_ROLE = "Manager";
    private static final String USER_ROLE = "User";
    private static final String UPDATED_USER = "updatedUser";
    private static final String ID = "id";
    private static final String DAILY_RECORD = "Day";
    private static final String SCOPE_COMPANY = "SCOPE-COMPANY";
    private static final String SCOPE_TEAM = "SCOPE-TEAM";
    private static final double HALF_DAY_INCREMENT = 0.5;
    private static final double DAY_INCREMENT = 1.0;
    private static final double DEFAULT_HOURS = 8.0;
    private static final String ATTACHMENT_NOT_FOUND = "Attachment not found";

    // CHECKSTYLE.OFF: ParameterNumber
    @Autowired
    public AbsenceController(AbsenceRepository absenceRepository,
            AbsenceTypeRepository absenceTypeRepository,
            AbsenceSubTypeRepository absenceSubTypeRepository, UserRepository userRepository,
            UserUtils userUtils, SecurityUtils securityUtils,
            StatusRepository statusRepository, HolidayRepository holidayRepository,
            SystemSettingRepository systemSettingRepository,
            AbsenceAttachmentRepository absenceAttachmentRepository,
            IStorageProvider storageProvider) {
        this.absenceRepository = absenceRepository;
        this.absenceTypeRepository = absenceTypeRepository;
        this.absenceSubTypeRepository = absenceSubTypeRepository;
        this.userRepository = userRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
        this.statusRepository = statusRepository;
        this.holidayRepository = holidayRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.absenceAttachmentRepository = absenceAttachmentRepository;
        this.storageProvider = storageProvider;
    }
    // CHECKSTYLE.ON: ParameterNumber

    @JsonView(Views.GetAbsences.class)
    @GetMapping
    public JsonViewPage<Absence> getAllAbsences(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = FIELD_NAME, required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        Specification<Absence> spec = Specification.where(null);

        // Apply optional text filter first
        if (!filter.isEmpty()) {
            spec = spec.and(RestUtils.getSpecificationFromFilter(BASIC, filter));
        }

        if (user != null && user.getUserRoles() != null) {
            List<String> userRoles = user.getUserRoles().stream()
                    .map(UserRole::getName)
                    .collect(Collectors.toList());

            if (userRoles.contains(ADMIN_ROLE)) {
                // Admin sees all users
            } else if (userRoles.contains(MANAGER_ROLE)) {
                // Manager sees users they manage
                spec = spec.and((root, query, cb) ->
                    cb.or(
                        cb.equal(root.get(ATTR_USER).get("lineManagerId"), user.getId()),
                        cb.equal(root.get(ATTR_USER).get(ID), user.getId())
                    )
                );
            } else {
                // Regular user sees only themselves
                spec = spec.and(
                        (root, query, cb) -> cb.equal(root.get(ATTR_USER).get(ID), user.getId()));
            }
        }

        return new JsonViewPage<>(absenceRepository.findAll(spec, pageable));
    }

    @JsonView(Views.GetAbsences.class)
    @GetMapping("/user")
    public JsonViewPage<Absence> getUserAbsences(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "approvedDate", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (filter.isEmpty()) {
            filter = "user.id=" + String.valueOf(currentUser.getId());
        } else {
            filter = filter + ",user.id=" + String.valueOf(currentUser.getId());
        }
        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));
        Specification<Absence> filterSpec = RestUtils
                .getSpecificationFromFilter(BASIC, filter);

        return new JsonViewPage<>(absenceRepository.findAll(filterSpec, pageable));
    }

    @JsonView(Views.GetAbsence.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getAbsenceById(@PathVariable Long id) {
        try {
            Absence absence = absenceRepository.findById(id).orElse(null);
            if (absence == null) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_NOT_FOUND);
            }

            return ResponseEntity.ok(absence);
        } catch (NumberFormatException | NoSuchElementException e) {
            return I18nResponses.notFound(MessagesCodes.ABSENCE_NOT_FOUND);
        }

    }

    @JsonView(Views.GetAbsence.class)
    @PostMapping
    @Transactional
    public ResponseEntity<?> createAbsence(@RequestBody Absence absence) {

        Status pendingStatus = statusRepository.findByName(STATUS_PENDING);
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (currentUser.getId() != absence.getUser().getId()) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return I18nResponses.badRequest(MessagesCodes.USER_NOT_FOUND);
            }

            if (!absenceTypeRepository.existsById(absence.getType().getId())) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
            }
            if (!userRepository.existsById(absence.getUser().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            absence.setUser(user);

            if (absence.getApprover() == null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return I18nResponses.badRequest(MessagesCodes.MISSING_APPROVER);
            }

            if (!userRepository.existsById(absence.getApprover().getId())) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            // Partner vacations are automatically approved
            if (user.getJobTitle() != null &&
                    user.getJobTitle().getName().equals(PARTNER_JOB_TITLE)) {
                Status approvedStatus = statusRepository.findByName(STATUS_APPROVED);
                absence.setApprover(user);
                absence.setStatus(approvedStatus);
            } else {
                User approver = userRepository
                        .findById(user.getLineManagerId()).orElse(null);
                absence.setApprover(approver);
                absence.setStatus(pendingStatus);
            }

            if (!isVacation(absence)) {
                String recordType = absence.getRecordType();
                if (recordType.equals(DAILY_RECORD)) {
                    absence.setAbsenceHours(DEFAULT_HOURS);
                } else {
                    double workDays = hoursToBusinessDays(absence.getAbsenceHours());
                    absence.setWorkDays(workDays);
                }
            }

            List<ValidationFailure> validationErrors = absence.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }

            if (UPDATED_USER.equals(handleAbsenceCreate(absence))) {
                // If the user was updated, we need to save it
                userRepository.save(user);
            }
            Absence savedAbsence = absenceRepository.save(absence);

            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.ABSENCE_CREATED_OK,
                    savedAbsence);
        } catch (HttpMessageNotReadableException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importAbsences(@RequestParam(value = "file") MultipartFile file)
            throws DataCentricException {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!canAdministerTimeoff(currentUser)) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        AbsenceType vacationType = absenceTypeRepository.findByName(VACATION).get();

        CsvImporter importer = new CsvImporter();
        CsvImporterConfiguration config = new CsvImporterConfiguration(",", true, null)
                .withColumnDescriptors(
                        new ColumnDescriptor(FIELD_USER_EMAIL, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_YEAR, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_NAME, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_START_DATE, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_END_DATE, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_BUSINESS_DAYS, ColumnType.STRING, true),
                        new ColumnDescriptor(FIELD_STATUS, ColumnType.STRING, true));
        importer.initialize(config);

        List<Absence> buffer = new ArrayList<>();
        Map<String, User> userCache = new HashMap<>();
        AtomicInteger rowNumber = new AtomicInteger(0);
        try {
            importer.importDataWithCustomCallback(file.getInputStream(), data -> {
                try {
                    rowNumber.incrementAndGet();

                    Map<String, Object> row = (Map<String, Object>) data;
                    String businessDays = (String) row.get(FIELD_BUSINESS_DAYS);
                    if (businessDays == null || businessDays.isEmpty()) {
                        log.warn("Row {}: Business days is empty or null.", rowNumber.get());
                        return;
                    }

                    Absence absence = new Absence();
                    absence.setName((String) row.get(FIELD_NAME));
                    absence.setBusinessYear((String) row.get(FIELD_YEAR));
                    absence.setStartDate(parseTimestamp((String) row.get(FIELD_START_DATE)));
                    absence.setEndDate(parseTimestamp((String) row.get(FIELD_END_DATE)));
                    absence.setType(vacationType);
                    absence.setWorkDays(Double.parseDouble(businessDays));
                    absence.setStatus(statusRepository
                            .findByName(((String) row.get(FIELD_STATUS)).toUpperCase()));
                    absence.setRecordType(DAILY_RECORD);

                    String email = (String) row.get(FIELD_USER_EMAIL);
                    User user;
                    if (userCache.containsKey(email)) {
                        user = userCache.get(email);
                    } else {
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        if (!userOpt.isPresent()) {
                            // If user is not found in cache, try to find it in the database
                            log.warn("User with email {} not found.", email);
                            return;
                        }
                        user = userOpt.get();
                    }

                    absence.setUser(user);
                    if (UPDATED_USER.equals(handleAbsenceCreate(absence))) {
                        userCache.put(email, user);
                    }
                    buffer.add(absence);
                    if (buffer.size() >= BATCH_SIZE) {
                        absenceRepository.saveAll(buffer);
                        for (User userRow : userCache.values()) {
                            userRepository.save(userRow);
                        }
                        buffer.clear();
                        userCache.clear();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Error processing row " + rowNumber.get() + ": " + e.getMessage(), e);
                }
            });
            if (!buffer.isEmpty()) {
                absenceRepository.saveAll(buffer);
                for (User userRow : userCache.values()) {
                    userRepository.save(userRow);
                }

            }
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.ABSENCE_CREATED_OK, null);
        } catch (IOException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error reading file: {}", ex.getMessage());
            return I18nResponses.badRequest(MessagesCodes.IMPORT_ABSENCE_CSV_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteAbsence(@PathVariable Long id) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            Optional<Absence> result = absenceRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_NOT_FOUND);
            }

            String currentYear = systemSettingRepository
                    .findByName(SystemSettings.CURRENT_YEAR).getValue();
            String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);
            Absence absence = result.get();

            if (!canAdministerTimeoff(currentUser) &&
                    (currentUser.getId() != absence.getUser().getId() ||
                            absence.getStatus().getName().equals(STATUS_DONE))) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (isVacation(absence) && !absence.getStatus().getName().equals(STATUS_DENIED)) {
                User absUser = userRepository.findById(absence.getUser().getId()).get();

                if (absence.getBusinessYear().equals(currentYear)) {
                    absUser.setCurrentYearVacationDays(absUser.getCurrentYearVacationDays() +
                            absence.getWorkDays());

                } else if (absence.getBusinessYear().equals(prevYear)) {
                    absUser.setPrevYearVacationDays(absUser.getPrevYearVacationDays() +
                            absence.getWorkDays());
                }
                userRepository.save(absUser);
            }

            List<AbsenceAttachment> attachments =
                    absenceAttachmentRepository.findByAbsenceId(id);

            absenceAttachmentRepository.deleteAll(attachments);
            absenceRepository.deleteById(id);

            for (AbsenceAttachment attachment : attachments) {
                try {
                    storageProvider.delete(attachment.getStorageObjectId());
                } catch (Exception e) {
                    log.error("Failed to delete attachment from storage. "
                        + attachment.getStorageObjectId(), e);
                }
            }


            return I18nResponses.accepted(MessagesCodes.ABSENCE_DELETED_OK);
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        }
    }

    @JsonView(Views.GetAbsence.class)
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateAbsenceById(@PathVariable Long id,
            @RequestBody Absence newAbsence,
            @RequestParam(defaultValue = "", required = false) String manage) {
        try {
            UserSecurityData currentUser = userUtils.getOrCreateUser();
            if (!securityUtils.hasSystemPermission(currentUser, MANAGE_TIMEOFF) &&
                    currentUser.getId() != newAbsence.getUser().getId()) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<Absence> result = absenceRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.ABSENCE_NOT_FOUND);
            }

            if (!absenceTypeRepository.existsById(newAbsence.getType().getId())) {
                return I18nResponses.badRequest(MessagesCodes.ABSENCE_TYPE_NOT_FOUND);
            }

            if (!userRepository.existsById(newAbsence.getUser().getId())) {
                return I18nResponses.notFound(MessagesCodes.USER_NOT_FOUND);
            }

            Optional<User> absUser = userRepository.findById(newAbsence.getUser().getId());
            if (absUser.isEmpty()) {
                return I18nResponses.badRequest(MessagesCodes.USER_NOT_FOUND);
            }

            User absenceUser = absUser.get();
            Absence absence = result.get();

            boolean wasOriginallyVacation = isVacation(absence);
            String originalBusinessYear = absence.getBusinessYear();
            double originalWorkDays = absence.getWorkDays() != null ? absence.getWorkDays() : 0.0;
            boolean wasNotDenied = !absence.getStatus().getName().equals(STATUS_DENIED);

            String currentYear = systemSettingRepository
                    .findByName(SystemSettings.CURRENT_YEAR).getValue();
            String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);

            String resultTypeChange = handleVacationTypeChange(
                absence,
                newAbsence,
                absenceUser,
                wasOriginallyVacation,
                wasNotDenied,
                originalBusinessYear,
                originalWorkDays,
                currentYear,
                prevYear
            );

            if (MessagesCodes.INSUFFICIENT_VACS_DAYS.equals(resultTypeChange)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return I18nResponses.badRequest(MessagesCodes.INSUFFICIENT_VACS_DAYS);
            }

            boolean needsToSaveUser = UPDATED_USER.equals(resultTypeChange);

            if (isVacation(newAbsence)) {
                absence.setSubType(null);
            } else {
                if (newAbsence.getSubType() == null ||
                        !absenceSubTypeRepository.existsById(newAbsence.getSubType().getId())) {
                    return I18nResponses.badRequest(MessagesCodes.ABSENCE_SUB_TYPE_NOT_FOUND);
                }
                absence.setSubType(newAbsence.getSubType());
                if (newAbsence.getRecordType().equals(DAILY_RECORD)) {
                    absence.setAbsenceHours(DEFAULT_HOURS);
                } else {
                    absence.setAbsenceHours(newAbsence.getAbsenceHours());
                    double workDays = hoursToBusinessDays(newAbsence.getAbsenceHours());
                    absence.setWorkDays(workDays);
                }
            }

            User user = userRepository.findById(currentUser.getId()).orElse(null);
            List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

            if (absence.getStatus().getName().equals(STATUS_DONE) &&
                    !userRoles.contains(ADMIN_ROLE)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            absence.setType(newAbsence.getType());
            absence.setUser(newAbsence.getUser());
            absence.setName(newAbsence.getName());
            absence.setStartDate(newAbsence.getStartDate());
            absence.setEndDate(newAbsence.getEndDate());
            absence.setRecordType(newAbsence.getRecordType());
            absence.setReason(newAbsence.getReason());
            absence.setObservations(newAbsence.getObservations());
            absence.setHasAttachments(newAbsence.getHasAttachments());

            String updateResult = null;
            if (manage.equals(CMD_APPROVE)) {
                handleAbsenceApproval(absence, currentUser.getId());
            } else if (manage.equals(CMD_DENY)) {
                handleAbsenceDenial(absence, newAbsence, absenceUser, currentUser.getId());
            } else {
                User lineManager = null;
                log.warn("Line manager ID: {}", absenceUser.getLineManagerId());
                if (absenceUser.getLineManagerId() != null) {
                    lineManager = userRepository.findById(absenceUser.getLineManagerId())
                        .orElse(null);
                }
                absence.setApprover(lineManager);

                if (absenceUser.getJobTitle().getName().equals(PARTNER_JOB_TITLE)) {
                    Status approvedStatus = statusRepository.findByName(STATUS_APPROVED);
                    absence.setApprover(absenceUser);
                    absence.setStatus(approvedStatus);
                } else {
                    User approver = userRepository
                            .findById(absenceUser.getLineManagerId()).orElse(null);
                    absence.setApprover(approver);
                }
                updateResult = handleAbsenceEdit(absence, newAbsence, absenceUser);
                if (MessagesCodes.INSUFFICIENT_VACS_DAYS.equals(updateResult)) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return I18nResponses.badRequest(MessagesCodes.INSUFFICIENT_VACS_DAYS);
                }
            }

            List<ValidationFailure> validationErrors = absence.getValidationFailures();
            if (!validationErrors.isEmpty()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return I18nResponses.httpResponseWithColumnsValidation(HttpStatus.BAD_REQUEST,
                        MessagesCodes.VALIDATION_FAILED, validationErrors);
            }

            Absence updatedAbsence = absenceRepository.save(absence);

            // Save the user if there was a change in type OR if handleAbsenceEdit changed
            if (needsToSaveUser || UPDATED_USER.equals(updateResult)) {
                userRepository.save(absenceUser);
            }

            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.ABSENCE_UPDATED_OK,
                    updatedAbsence);

        } catch (HttpMessageNotReadableException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Data integrity violation: {}", e.getMessage());
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        }
    }

    private String handleVacationTypeChange(
        Absence oldAbsence,
        Absence newAbsence,
        User absenceUser,
        boolean wasOriginallyVacation,
        boolean wasNotDenied,
        String originalBusinessYear,
        double originalWorkDays,
        String currentYear,
        String prevYear
    ) {
        boolean typeChanged = false;

        // VACATION -> ABSENSES
        if (wasOriginallyVacation && !isVacation(newAbsence) && wasNotDenied) {

            if (originalBusinessYear.equals(currentYear)) {
                absenceUser.setCurrentYearVacationDays(
                    absenceUser.getCurrentYearVacationDays() + originalWorkDays
                );
            } else if (originalBusinessYear.equals(prevYear)) {
                absenceUser.setPrevYearVacationDays(
                    absenceUser.getPrevYearVacationDays() + originalWorkDays
                );
            }

            typeChanged = true;
        }

        // ABSENSES -> VACATION
        if (!wasOriginallyVacation && isVacation(newAbsence)) {

            double newWorkDays = newAbsence.getWorkDays() != null
                    ? newAbsence.getWorkDays()
                    : 0.0;

            double availableDays = 0.0;

            if (newAbsence.getBusinessYear().equals(currentYear)) {
                availableDays = absenceUser.getCurrentYearVacationDays();
            } else if (newAbsence.getBusinessYear().equals(prevYear)) {
                availableDays = absenceUser.getPrevYearVacationDays();
            }

            if (availableDays < newWorkDays) {
                return MessagesCodes.INSUFFICIENT_VACS_DAYS;
            }

            if (newAbsence.getBusinessYear().equals(currentYear)) {
                absenceUser.setCurrentYearVacationDays(
                    absenceUser.getCurrentYearVacationDays() - newWorkDays
                );
            } else if (newAbsence.getBusinessYear().equals(prevYear)) {
                absenceUser.setPrevYearVacationDays(
                    absenceUser.getPrevYearVacationDays() - newWorkDays
                );
            }

            typeChanged = true;
        }

        return typeChanged ? UPDATED_USER : "NO_CHANGE";
    }

    private String handleAbsenceCreate(Absence absence) {

        String currentYear = systemSettingRepository
                .findByName(SystemSettings.CURRENT_YEAR).getValue();
        String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);
        User user = absence.getUser();
        if (!isVacation(absence)) {
            return null;
        }

        // in case vacation type check if user has enough vacation days and update it
        if (absence.getBusinessYear().equals(currentYear)) {
            double res = user.getCurrentYearVacationDays() - absence.getWorkDays();
            if (res < 0.0) {
                return MessagesCodes.INSUFFICIENT_VACS_DAYS;
            } else {
                // update remaining days
                user.setCurrentYearVacationDays(res);
                return UPDATED_USER;
            }
        } else if (absence.getBusinessYear().equals(prevYear)) {
            double res = user.getPrevYearVacationDays() - absence.getWorkDays();
            if (res < 0.0) {
                return MessagesCodes.INSUFFICIENT_VACS_DAYS;
            } else {
                // update remaining days
                user.setPrevYearVacationDays(res);
                return UPDATED_USER;
            }
        }
        return null;
    }

    private String handleAbsenceEdit(Absence absence, Absence newAbsence,
            User absenceUser) {

        String currentYear = systemSettingRepository
                .findByName(SystemSettings.CURRENT_YEAR).getValue();
        String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);
        Status previousStatus = absence.getStatus();
        String previousAbsenceYear = absence.getBusinessYear();
        String result = null;
        Status pendingStatus = statusRepository.findByName(STATUS_PENDING);

        absence.setBusinessYear(newAbsence.getBusinessYear());
        if (!absenceUser.getJobTitle().getName().equals(PARTNER_JOB_TITLE)) {
            absence.setStatus(pendingStatus);
        }

        if (!isVacation(absence)) {
            return null;
        }

        double prevWorkDays = 0.0;
        if (absence.getWorkDays() != null) {
            prevWorkDays = absence.getWorkDays();
        }
        if (!StringUtils.nullSafeEquals(previousAbsenceYear, newAbsence.getBusinessYear())) {
            prevWorkDays = 0.0;
        }

        if (previousStatus.getName().equals(STATUS_DENIED)) {
            prevWorkDays = 0.0;
            absence.setObservations(null);
        }

        double vacationDaysBalance = absenceUser.getCurrentYearVacationDays();
        if (StringUtils.nullSafeEquals(newAbsence.getBusinessYear(), prevYear)) {
            vacationDaysBalance = absenceUser.getPrevYearVacationDays();
        }

        vacationDaysBalance += prevWorkDays - newAbsence.getWorkDays();
        if (vacationDaysBalance < 0.0) {
            return MessagesCodes.INSUFFICIENT_VACS_DAYS;
        }

        if (StringUtils.nullSafeEquals(newAbsence.getBusinessYear(), currentYear)) {

            // If the business year from the absence has changed, then we need
            // to reset the user previous year remaing days
            if (!previousAbsenceYear.equals(newAbsence.getBusinessYear())) {
                double resetedDays = absenceUser.getPrevYearVacationDays() +
                        absence.getWorkDays();

                absenceUser.setPrevYearVacationDays(resetedDays);
            }

            // update remaining days
            absence.setWorkDays(newAbsence.getWorkDays());
            absenceUser.setCurrentYearVacationDays(vacationDaysBalance);
            result = UPDATED_USER;

        } else if (StringUtils.nullSafeEquals(newAbsence.getBusinessYear(), prevYear)) {

            // If the business year from the absence has changed, then we need
            // to reset the user current year remaing days
            if (!absence.getBusinessYear().equals(newAbsence.getBusinessYear())) {
                double resetedDays = absenceUser.getCurrentYearVacationDays() +
                        absence.getWorkDays();

                absenceUser.setCurrentYearVacationDays(resetedDays);
            }

            // update remaining days
            absence.setWorkDays(newAbsence.getWorkDays());
            absenceUser.setPrevYearVacationDays(vacationDaysBalance);
            result = UPDATED_USER;
        }

        return result;
    }

    @JsonView(Views.GetBasicInfo.class)
    @GetMapping("/byDate/{year}")
    public ResponseEntity<?> getAbsencesForYear(@PathVariable String year,
            @RequestParam(required = false) List<Long> userFilter,
            @RequestParam(required = false) List<Long> statusFilter,
            @RequestParam(required = false) List<Long> typeFilter,
            @RequestParam(required = false) List<String> businessYearFilter,
            @RequestParam(defaultValue = SCOPE_TEAM, required = false) String scope) {

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        // CHECKSTYLE.OFF: MagicNumber
        Timestamp start = Timestamp.valueOf(
                LocalDateTime.of(Integer.parseInt(year), 1, 1, 0, 0));
        Timestamp end = Timestamp.valueOf(
                LocalDateTime.of(Integer.parseInt(year), 12, 31, 23, 59, 59));
        LocalDate startYearDate = LocalDate.of(Integer.parseInt(year), 1, 1);
        LocalDate endYearDate = LocalDate.of(Integer.parseInt(year), 12, 31);
        // CHECKSTYLE.ON: MagicNumber

        List<LocalDate> holidaysList = holidayRepository
                .findAllHolidaysDatesByDateInterval(startYearDate, endYearDate);

        // Convert empty lists to null for proper filtering in query
        userFilter = userFilter != null && userFilter.isEmpty() ? null : userFilter;
        statusFilter = statusFilter != null && statusFilter.isEmpty() ? null : statusFilter;
        typeFilter = typeFilter != null && typeFilter.isEmpty() ? null : typeFilter;
        businessYearFilter = businessYearFilter != null && businessYearFilter.isEmpty()
            ? null : businessYearFilter;

        // Use optimized query with eager loading - filtering is done in database
        List<Absence> absencesList = absenceRepository.getOptimizedAbsencesByDateWithFilters(
                start, end, userFilter, statusFilter, typeFilter, businessYearFilter);

        List<Long> managerTeam = new ArrayList<>();
        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        if (!userRoles.contains(ADMIN_ROLE)) {
            managerTeam = userRepository.findManagerTeam(user.getId());
            // Add manager id so he can also see his absences
            managerTeam.add(user.getId());
        }

        Map<LocalDate, CalendarMatrixData> absencesByDate = new HashMap<>();
        ZoneId zone = ZoneId.of(TIME_ZONE);
        LocalDate startLocalDate = start.toLocalDateTime().toLocalDate();
        LocalDate endLocalDate = end.toLocalDateTime().toLocalDate();

        // Convert holiday list to Set for O(1) lookup instead of O(n)
        Set<LocalDate> holidaysSet =
            new HashSet<>(holidaysList);

        boolean isAdmin = userRoles.contains(ADMIN_ROLE);
        boolean isManager = userRoles.contains(MANAGER_ROLE);
        boolean isOnlyUser = userRoles.contains(USER_ROLE) && !isManager;

        // Process absences with minimal logic - security and scope filtering only
        for (Absence abs : absencesList) {
            // Early exit for cancelled absences
            if (abs.getStatus().getName().equals(STATUS_CANCELLED)) {
                continue;
            }

            Long absenceUserId = abs.getUser().getId();
            String absenceType = abs.getType().getName();
            String statusName = abs.getStatus().getName();

            // Security: Check permissions
            boolean isAbsenceForTeamOrSelf = isManager &&
                            (absenceType.equals(ABSENCE) &&
                            (exactEquals(abs.getUser().getLineManagerId(), user.getId()) ||
                            absenceUserId.equals(user.getId())));

            if (absenceType.equals(ABSENCE) &&
                    scope.equals(SCOPE_COMPANY) &&
                    !isAdmin && (isAbsenceForTeamOrSelf || isOnlyUser)) {
                continue;
            }

            // Scope: Check team visibility
            if (!managerTeam.contains(absenceUserId) &&
                    !isAdmin && scope.equals(SCOPE_TEAM)) {
                continue;
            }

            // Process date range - optimized loop
            LocalDate startDate = abs.getStartDate().toInstant().atZone(zone).toLocalDate();
            LocalDate endDate = abs.getEndDate().toInstant().atZone(zone).toLocalDate();

            // Ensure we don't process dates outside the requested year
            if (startDate.isBefore(startLocalDate)) {
                startDate = startLocalDate;
            }
            if (endDate.isAfter(endLocalDate)) {
                endDate = endLocalDate;
            }

            LocalDate currentDate = startDate;
            double increment = DAY_INCREMENT;

            // Pre-calculate increment once
            if (isVacation(abs)) {
                if (!abs.getRecordType().equals(DAILY_RECORD)) {
                    increment = HALF_DAY_INCREMENT;
                }
            } else {
                if (!abs.getRecordType().equals(DAILY_RECORD)) {
                    increment = hoursToBusinessDays(abs.getAbsenceHours());
                }
            }

            while (!currentDate.isAfter(endDate)) {
                // Discard weekends and holidays using Set lookup
                DayOfWeek day = currentDate.getDayOfWeek();
                boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
                boolean isHoliday = holidaysSet.contains(currentDate);
                if (!isWeekend && !isHoliday) {
                    absencesByDate
                        .computeIfAbsent(currentDate, d -> new CalendarMatrixData())
                        .incrementEntry(statusName, absenceType, increment);
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        return I18nResponses.httpResponseWithData(HttpStatus.OK,
                MessagesCodes.ABSENCE_BY_DATE_MAP_OK, absencesByDate);

    }

    @JsonView(Views.GetAbsences.class)
    @GetMapping("/byDateDetails")
    public ResponseEntity<?> getAbsencesForDate(@RequestParam Timestamp date,
            @RequestParam(required = false) List<Long> userFilter,
            @RequestParam(required = false) List<Long> statusFilter,
            @RequestParam(required = false) List<Long> typeFilter,
            @RequestParam(required = false) List<String> businessYearFilter,
            @RequestParam(defaultValue = SCOPE_TEAM, required = false) String scope) {

        ZoneId dbZone = ZoneId.of(TIME_ZONE);
        LocalDate localDate = date.toInstant().atZone(dbZone).toLocalDate();

        // Build start and end in Lisbon time, then convert to UTC Instant
        ZonedDateTime zonedStart = localDate.atStartOfDay(dbZone);
        ZonedDateTime zonedEnd = localDate.plusDays(1).atStartOfDay(dbZone).minusSeconds(1);

        Timestamp start = Timestamp.from(zonedStart.toInstant());
        Timestamp end = Timestamp.from(zonedEnd.toInstant());

        List<Absence> absencesList = absenceRepository.getAbsencesFromDate(start, end);

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        List<String> userRoles = user.getUserRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());

        // TODO: Improve method performance in case there are a large number of records
        absencesList = absencesList.stream()
                .filter(a -> {
                    Long absenceUserId = a.getUser().getId();

                    // Team-based visibility logic
                    if (userRoles.contains(ADMIN_ROLE) || scope.equals(SCOPE_COMPANY)) {
                        // no restriction
                        return true;
                    } else if (userRoles.contains(MANAGER_ROLE)) {
                        // Only absences of users this manager manages
                        return a.getUser().getLineManagerId() != null &&
                                a.getUser().getLineManagerId().equals(user.getId())
                                || absenceUserId.equals(user.getId());
                    } else {
                        // Only their own absences
                        return absenceUserId.equals(user.getId());
                    }
                })
                .filter(a -> {
                    boolean userMatches = filterMatches(userFilter, a.getUser().getId());

                    boolean isStatusValid = a.getStatus() == null
                        || (!STATUS_CANCELLED.equals(a.getStatus().getName())
                            && !STATUS_DENIED.equals(a.getStatus().getName()));

                    boolean statusMatches = filterMatches(
                        statusFilter,
                        a.getStatus() != null ? a.getStatus().getId() : null
                    );

                    boolean typeMatches = filterMatches(typeFilter, a.getType().getId());

                    boolean businessYearMatches = businessYearFilter == null
                        || businessYearFilter.isEmpty()
                            || businessYearFilter.contains(a.getBusinessYear());

                    return userMatches
                            && isStatusValid
                            && statusMatches
                            && typeMatches
                            && businessYearMatches;
                })
                .filter(a -> {
                    Long absenceUserId = a.getUser().getId();
                    boolean isAbsenceForTeamOrSelf = a.getType().getName().equals(ABSENCE) &&
                                (exactEquals(a.getUser().getLineManagerId(), user.getId()) ||
                                absenceUserId.equals(user.getId()));
                    return a.getType().getName().equals(VACATION) ||
                            userRoles.contains(ADMIN_ROLE) ||
                            (userRoles.contains(MANAGER_ROLE) && isAbsenceForTeamOrSelf);
                })
                .collect(Collectors.toList());

        return I18nResponses.httpResponseWithData(HttpStatus.OK,
                MessagesCodes.ABSENCE_BY_DATE_MAP_OK, absencesList);
    }

    @Transactional
    @PatchMapping
    public ResponseEntity<?> patchCommandDispatcher(@RequestBody List<TimeRecordPatch> commands) {
        if (commands == null || commands.isEmpty()) {
            return I18nResponses.badRequest(MessagesCodes.EMPTY_COMMAND_LIST);
        }

        UserSecurityData currentUser = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(currentUser, MANAGE_TIMEOFF)) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }

        for (TimeRecordPatch command : commands) {
            Status newStatus;
            switch (command.command) {
                case CMD_APPROVE:
                    newStatus = statusRepository.findByName(STATUS_APPROVED);
                    updateAbsences(command.data.ids, newStatus, null, currentUser.getId(),
                            CMD_APPROVE);
                    break;
                case CMD_DENY:
                    newStatus = statusRepository.findByName(STATUS_DENIED);
                    updateAbsences(command.data.ids, newStatus, command.data.reason,
                            currentUser.getId(), CMD_DENY);
                    break;
                case CMD_PENDING:
                    newStatus = statusRepository.findByName(STATUS_PENDING);
                    updateAbsences(command.data.ids, newStatus, command.data.reason,
                            currentUser.getId(), CMD_PENDING);
                    break;
                default:
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return I18nResponses.badRequest(MessagesCodes.INVALID_COMMAND);
            }
        }

        // TODO: Change to a better response returning the response for each command
        return I18nResponses.accepted(MessagesCodes.TIME_RECORD_UPDATED_OK);
    }

    @PostMapping("/{absenceId}/attachments")
    public ResponseEntity<?> uploadAbsenceAttachments(@PathVariable Long absenceId,
            @RequestParam(value = "files") List<MultipartFile> files) {

        Optional<Absence> result = absenceRepository.findById(absenceId);
        if (result.isEmpty()) {
            return I18nResponses.notFound(MessagesCodes.ABSENCE_NOT_FOUND);
        }
        Absence absence = result.get();

        if (files == null || files.isEmpty()) {
            return I18nResponses.badRequest(MessagesCodes.NO_FILES_UPLOADED);
        }

        for (MultipartFile file : files) {
            try {
                String uuid = UUID.randomUUID().toString();
                String originalFileName = file.getOriginalFilename();

                String fileExtension = "";
                if (originalFileName != null) {
                    int dotIndex = originalFileName.lastIndexOf('.');
                    if (dotIndex != -1) {
                        fileExtension = originalFileName.substring(dotIndex);
                    }
                }

                String storageObjectId = "absences/" + absenceId + "/" + absenceId + "-" + uuid
                    + fileExtension;

                try (InputStream inputStream = file.getInputStream()) {
                    // Store using the storage provider
                    storageProvider.put(storageObjectId, inputStream, file.getSize());
                }

                // Save attachment record
                AbsenceAttachment attachment = new AbsenceAttachment();
                attachment.setOriginalFileName(originalFileName);
                attachment.setStorageObjectId(storageObjectId);
                attachment.setAbsence(absence);

                absenceAttachmentRepository.save(attachment);
            } catch (Exception e) {
                return I18nResponses.badRequest(MessagesCodes.FILE_UPLOAD_FAILED);
            }
        }

        // mark absence as having attachments
        absence.setHasAttachments(true);
        absenceRepository.save(absence);

        return I18nResponses.accepted(MessagesCodes.ABSENCE_ATTACHMENTS_UPLOADED_OK);
    }

    @GetMapping("/{absenceId}/attachments/{attachmentId}")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(
            @PathVariable Long absenceId,
            @PathVariable Long attachmentId) {

        try {
            AbsenceAttachment attachment = absenceAttachmentRepository
                .findByAbsenceIdAndAttachmentId(absenceId, attachmentId)
                .orElseThrow(() -> new RuntimeException(ATTACHMENT_NOT_FOUND));

            StreamingResponseBody stream = outputStream -> {
                try {
                    storageProvider.get(attachment.getStorageObjectId(), outputStream);
                } catch (Exception e) {
                    throw new IOException("Failed to retrieve file from storage", e);
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + attachment.getOriginalFileName() + "\"");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
        } catch (Exception e ) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{absenceId}/attachments/downloadAll")
    public ResponseEntity<StreamingResponseBody> downloadAllAttachments(
            @PathVariable Long absenceId) {

        try {
            List<AbsenceAttachment> attachments =
                    absenceAttachmentRepository.findByAbsenceId(absenceId);

            if (attachments.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            /*Create ZIP on stream - files are added
            directly without loading everything into memory*/
            StreamingResponseBody stream = outputStream -> {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

                    Map<String, Integer> fileNameCount = new HashMap<>();

                    for (AbsenceAttachment attachment : attachments) {

                        String originalFileName = attachment.getOriginalFileName();
                        String zipFileName = getUniqueFileName(
                                originalFileName, fileNameCount);

                        try {
                            ZipEntry entry = new ZipEntry(zipFileName);
                            zipOutputStream.putNextEntry(entry);

                            storageProvider.get(attachment.getStorageObjectId(), zipOutputStream);

                            zipOutputStream.closeEntry();

                        } catch (Exception storageException) {
                            String errorFileName = getUniqueFileName(originalFileName +
                                ".txt", fileNameCount);
                            String message =
                                "File \"" + originalFileName + "\" is missing from storage.\n" +
                                "Storage Object ID: " + attachment.getStorageObjectId();

                            ZipEntry errorEntry = new ZipEntry(errorFileName);
                            zipOutputStream.putNextEntry(errorEntry);
                            zipOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
                            zipOutputStream.closeEntry();
                        }
                    }

                    zipOutputStream.finish();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create ZIP file", e);
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"absence-" + absenceId + "-attachments.zip\"");

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Generate unique file name in case of duplicates
    private String getUniqueFileName(
            String originalName,
            Map<String, Integer> fileNameCount) {

        int count = fileNameCount.getOrDefault(originalName, 0);
        fileNameCount.put(originalName, count + 1);

        // First time -> keeps the name
        if (count == 0) {
            return originalName;
        }

        int dotIndex = originalName.lastIndexOf('.');
        String name =
                (dotIndex == -1)
                        ? originalName
                        : originalName.substring(0, dotIndex);

        String extension =
                (dotIndex == -1)
                        ? ""
                        : originalName.substring(dotIndex);

        return name + " (" + (count + 1) + ")" + extension;
    }

    @DeleteMapping("/{absenceId}/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable Long absenceId,
            @PathVariable Long attachmentId) throws Exception {

        AbsenceAttachment attachment = absenceAttachmentRepository
            .findByAbsenceIdAndAttachmentId(absenceId, attachmentId)
            .orElseThrow(() -> new RuntimeException(ATTACHMENT_NOT_FOUND));

        storageProvider.delete(attachment.getStorageObjectId());

        absenceAttachmentRepository.deleteById(attachmentId);

        boolean stillHasAttachments =
            absenceAttachmentRepository.existsByAbsenceId(absenceId);
        if (!stillHasAttachments) {
            Absence absence = absenceRepository.findById(absenceId).get();
            absence.setHasAttachments(false);
            absenceRepository.save(absence);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{absenceId}/attachments")
    public ResponseEntity<?> getAttachmentsByAbsenceId (@PathVariable Long absenceId) {
        try {
            List<AbsenceAttachment> attachments =
                absenceAttachmentRepository.findByAbsenceId(absenceId);
            return I18nResponses.httpResponseWithData(HttpStatus.OK,
                    MessagesCodes.ABSENCE_ATTACHMENTS_FOUND_OK, attachments);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateAbsences(List<Long> ids, Status newStatus, String observation,
            Long approverId, String cmd) {

        String currentYear = systemSettingRepository
                .findByName(SystemSettings.CURRENT_YEAR).getValue();
        String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);
        List<Absence> absencesList = absenceRepository.findAllAbsencesById(ids);
        Map<Long, Map<String, Double>> userYearWorkDaysMap = new HashMap<>();

        if (cmd.equals(CMD_PENDING)) {
            absenceRepository.updateAbsencesStatus(newStatus, observation, ids, approverId);
            return;
        }

        for (Absence abs : absencesList) {
            if (!VACATION.equals(abs.getType().getName()) || cmd.equals(CMD_APPROVE))
                continue;

            Long userId = abs.getUser().getId();
            String businessYear = abs.getBusinessYear();
            Double workDays = abs.getWorkDays();

            userYearWorkDaysMap
                    .computeIfAbsent(userId, k -> new HashMap<>())
                    .merge(businessYear, workDays, Double::sum);
        }

        for (Map.Entry<Long, Map<String, Double>> userEntry : userYearWorkDaysMap.entrySet()) {
            Long userId = userEntry.getKey();
            User user = userRepository.findById(userId).orElse(null);
            if (user == null)
                continue;

            Map<String, Double> yearMap = userEntry.getValue();
            for (Map.Entry<String, Double> yearEntry : yearMap.entrySet()) {
                String year = yearEntry.getKey();
                Double totalDays = yearEntry.getValue();

                if (year.equals(currentYear)) {
                    user.setCurrentYearVacationDays(user.getCurrentYearVacationDays() + totalDays);
                } else if (year.equals(prevYear)) {
                    user.setPrevYearVacationDays(user.getPrevYearVacationDays() + totalDays);
                }
            }
            userRepository.save(user);
        }

        // TODO - Sanity check of the given values
        // TODO - Check for possible errors and handle optimistic locking
        absenceRepository.updateAbsencesStatus(newStatus, observation, ids, approverId);
    }

    private static <T> boolean filterMatches(List<T> filter, T item) {
        if (filter == null || filter.isEmpty()) {
            return true; // No filter applied
        }
        return filter.contains(item);
    }

    private void handleAbsenceApproval(Absence absence, Long approverId) {
        User approver = userRepository.findById(approverId).get();
        Status approvedStatus = statusRepository.findByName(STATUS_APPROVED);
        absence.setStatus(approvedStatus);
        absence.setApprovedBy(approver);
        absence.setApprovedDate(new Timestamp(System.currentTimeMillis()));
    }

    private void handleAbsenceDenial(Absence absence, Absence newAbsence, User absenceUser,
            Long approverId) {
        String currentYear = systemSettingRepository
                .findByName(SystemSettings.CURRENT_YEAR).getValue();
        String prevYear = String.valueOf(Integer.parseInt(currentYear) - 1);
        User approver = userRepository.findById(approverId).get();
        Status deniedStatus = statusRepository.findByName(STATUS_DENIED);
        absence.setStatus(deniedStatus);
        absence.setApprovedBy(approver);
        absence.setApprovedDate(new Timestamp(System.currentTimeMillis()));
        // If the vacation request was denied reset the vacation days used
        if (newAbsence.getType().getName().equals(VACATION)) {
            if (newAbsence.getBusinessYear().equals(currentYear)) {
                absenceUser.setCurrentYearVacationDays(absenceUser
                        .getCurrentYearVacationDays() + newAbsence.getWorkDays());
            } else if (newAbsence.getBusinessYear().equals(prevYear)) {
                absenceUser.setPrevYearVacationDays(absenceUser.getPrevYearVacationDays() +
                        newAbsence.getWorkDays());
            }
            userRepository.save(absenceUser);
        }
    }

    private boolean isVacation(Absence absence) {
        return VACATION.equals(absence.getType().getName());
    }

    private Timestamp parseTimestamp(String date) {
        try {
            return Timestamp.valueOf(date);
        } catch (IllegalArgumentException e) {
            log.error("Invalid date format: {}", date);
            return null;
        }
    }

    private boolean canAdministerTimeoff(UserSecurityData user) {
        return securityUtils.hasSystemPermission(user, MANAGE_TIMEOFF);
    }

    private boolean exactEquals(Object obj1, Object obj2) {
        return obj1 != null && obj2.equals(obj1);
    }
}
