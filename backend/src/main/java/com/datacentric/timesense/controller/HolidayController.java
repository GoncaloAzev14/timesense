package com.datacentric.timesense.controller;

import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.web.multipart.MultipartFile;

import com.datacentric.exceptions.DataCentricException;
import com.datacentric.timesense.model.Holiday;
import com.datacentric.timesense.repository.HolidayRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.TimeoffManagementUtils;
import com.datacentric.timesense.utils.hibernate.Message;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.imports.CsvImporter;
import com.datacentric.utils.imports.CsvImporter.CsvImporterConfiguration;
import com.datacentric.utils.imports.ColumnDescriptor;
import com.datacentric.utils.imports.ColumnType;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String BASIC = "basic";
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "MANAGE_TIMEOFF";
    private static final String HOLIDAY_DATE = "holidayDate";
    private static final String NAME = "name";

    private static final Logger log = LoggerFactory.getLogger(HolidayController.class);

    public static final class Views {

        public interface GetHolidays extends Holiday.Views.Public,
                JsonViewPage.Views.Public {
        }
    }

    private TimeoffManagementUtils timeoffUtils;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;
    private HolidayRepository holidayRepository;

    @Autowired
    public HolidayController(HolidayRepository holidayRepository,
            SecurityUtils securityUtils, UserUtils userUtils, TimeoffManagementUtils timeoffUtils) {
        this.holidayRepository = holidayRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
        this.timeoffUtils = timeoffUtils;
    }

    @GetMapping
    public List<Holiday> getHolidays(
            @RequestParam(defaultValue = "", required = false) String filter) {

        if (!filter.isEmpty()) {
            Specification<Holiday> filterSpec = RestUtils
                    .getSpecificationFromFilter(BASIC, filter);
            return (holidayRepository.findAll(filterSpec));
        } else {
            return (holidayRepository.findAll());
        }
    }

    @JsonView(Views.GetHolidays.class)
    @GetMapping("/HolidaysPage")
    public JsonViewPage<Holiday> getHolidaysPage(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = HOLIDAY_DATE, required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        if (!filter.isEmpty()) {
            Specification<Holiday> filterSpec = RestUtils
                    .getSpecificationFromFilter(BASIC, filter);
            return new JsonViewPage<>(holidayRepository.findAll(filterSpec, pageable));
        } else {
            return new JsonViewPage<>(holidayRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetHolidays.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetHolidayById(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(
                        MessagesCodes.PERMISSIONS_DENIED);
            }
            Optional<Holiday> holiday = holidayRepository.findById(id);
            if (holiday == null) {
                return I18nResponses.notFound(
                        MessagesCodes.HOLIDAY_NOT_FOUND);
            }

            return ResponseEntity.ok(holiday);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.HOLIDAY_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importHolidays(@RequestParam(value = "file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                Message message = new Message(MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR,
                        List.of("The file passed is null or empty!"));
                return I18nResponses.httpResponseWithData(HttpStatus.BAD_REQUEST,
                        MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR, message);
            }

            log.info("Importing holidays from csv file: {} ", file.getOriginalFilename());
            InputStream inputStream = file.getInputStream();

            final List<Holiday> holidaysList = new ArrayList<>();
            CsvImporter csvImporter = new CsvImporter();
            csvImporter.initialize(new CsvImporterConfiguration(",", true, data -> {
                Map<String, Object> row = (Map<String, Object>) data;
                Date holidayDate = (Date) row.get(HOLIDAY_DATE);
                String name = (String) row.get(NAME);
                if (holidayDate != null && name != null) {
                    Holiday day = new Holiday();
                    day.setHolidayDate(holidayDate.toLocalDate());
                    day.setName(name);
                    holidaysList.add(day);
                } else {
                    throw new RuntimeException("Invalid data in CSV file: " + row);
                }
            }).withColumnDescriptors(
                    new ColumnDescriptor(HOLIDAY_DATE, ColumnType.DATE, false),
                    new ColumnDescriptor(NAME, ColumnType.STRING, false)));

            csvImporter.importData(inputStream);

            // Extract the dates
            List<LocalDate> datesToCheck = holidaysList.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toList());

            List<LocalDate> duplicateHolidaysList = holidayRepository
                    .findAllExistingDates(datesToCheck);

            // Filter out the existing dates
            List<Holiday> holidaysToSave = holidaysList.stream()
                    .filter(holiday -> !duplicateHolidaysList.contains(holiday.getHolidayDate()))
                    .collect(Collectors.toList());

            if (holidaysToSave.isEmpty()) {
                Message isEmptyMessage = new Message(MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR,
                        List.of("The file does not include new holidays!"));
                return I18nResponses.httpResponseWithData(HttpStatus.BAD_REQUEST,
                        MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR, isEmptyMessage);
            }

            List<LocalDate> newHolidaysDates = holidaysToSave.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toList());

            timeoffUtils.recalculateUsersAbsences(newHolidaysDates, null);

            holidayRepository.saveAll(holidaysToSave);
            Message successMessage = new Message(MessagesCodes.HOLIDAY_CREATED_OK,
                    List.of("Imported holidays configuration successfully!"));
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.HOLIDAY_CREATED_OK,
                    successMessage);

        } catch (DataCentricException e) {
            Message message = new Message(MessagesCodes.RECALCULATE_TIMEOFF_ERROR,
                    List.of(e.getMessage()));
            return I18nResponses.httpResponseWithData(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.RECALCULATE_TIMEOFF_ERROR, message);
        } catch (Exception e) {
            Message message = new Message(MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR,
                    List.of(e.getMessage()));
            return I18nResponses.httpResponseWithData(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.IMPORT_HOLIDAY_CSV_ERROR, message);
        }
    }

    @JsonView(Views.GetHolidays.class)
    @PostMapping()
    public ResponseEntity<?> createHoliday(@RequestBody Holiday holiday) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(
                        MessagesCodes.PERMISSIONS_DENIED);
            }

            Holiday newHoliday = holidayRepository.save(holiday);
            timeoffUtils.recalculateUsersAbsences(List.of(newHoliday.getHolidayDate()), null);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.HOLIDAY_CREATED_OK,
                    newHoliday);
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
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        try {

            Optional<Holiday> result = holidayRepository.findById(id);
            if (result == null) {
                return I18nResponses.notFound(MessagesCodes.HOLIDAY_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            holidayRepository.deleteHolidayById(id);
            timeoffUtils.recalculateUsersAbsences(null, List.of(result.get().getHolidayDate()));
            return I18nResponses.accepted(MessagesCodes.HOLIDAY_DELETED_OK);

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

    @JsonView(Views.GetHolidays.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClusterId(@PathVariable Long id,
            @RequestBody Holiday newHoliday) {
        try {
            Optional<Holiday> result = holidayRepository.findById(id);
            Holiday holiday = result.get();

            if (holiday == null) {
                return I18nResponses.notFound(MessagesCodes.HOLIDAY_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }
            List<LocalDate> removedDate = new ArrayList<>();
            removedDate.add(holiday.getHolidayDate());

            holiday.setHolidayDate(newHoliday.getHolidayDate());
            holiday.setName(newHoliday.getName());

            Holiday updatedHoliday = holidayRepository.save(holiday);
            timeoffUtils.recalculateUsersAbsences(List.of(holiday.getHolidayDate()), removedDate);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.HOLIDAY_UPDATED_OK,
                    updatedHoliday);

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
