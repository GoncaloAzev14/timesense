package com.datacentric.timesense;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Absence;
import com.datacentric.timesense.model.AbsenceSubType;
import com.datacentric.timesense.model.AbsenceType;
import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.SystemSetting;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.AbsenceRepository;
import com.datacentric.timesense.repository.AbsenceSubTypeRepository;
import com.datacentric.timesense.repository.AbsenceTypeRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbsenceControllerTests extends SecurityBaseClass {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AbsenceTypeRepository absenceTypeRepository;

    @Autowired
    private AbsenceSubTypeRepository absenceSubTypeRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    private User approver;
    private AbsenceType absenceType;
    private AbsenceSubType absenceSubType;
    private Absence absence;
    private Status status;

    @BeforeEach
    public void setup() {

        savePermission("System", 0L,
                SystemAccessTypes.MANAGE_TIMEOFF, "user", dummyUser.getId());

        SystemSetting systemSetting = new SystemSetting();
        systemSetting.setName(SystemSettings.CURRENT_YEAR);
        systemSetting.setValue("2025");
        systemSettingRepository.save(systemSetting);

        absenceType = new AbsenceType();
        absenceType.setName("VACATION");
        absenceTypeRepository.save(absenceType);

        absenceType = new AbsenceType();
        absenceType.setName("AbsenceType1");
        absenceTypeRepository.save(absenceType);

        absenceSubType = new AbsenceSubType();
        absenceSubType.setName("AbsenceSubType1");
        absenceSubTypeRepository.save(absenceSubType);

        approver = new User();
        approver.setName("Some Approver");
        approver.setEmail("approver@email.com");
        approver.setCurrentYearVacationDays(23.0);
        approver.setPrevYearVacationDays(23.0);
        approver.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(approver);

        dummyUser.setLineManagerId(approver.getId());
        userRepository.save(dummyUser);

        status = new Status();
        status.setName("DONE");
        statusRepository.save(status);

        status = new Status();
        status.setName("PENDING");
        statusRepository.save(status);

        absence = new Absence();
        absence.setType(absenceType);
        absence.setUser(dummyUser);
        absence.setName("Ferias2025");
        absence.setApprovedDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        absence.setStartDate(Timestamp.valueOf("2025-12-12 01:02:03.123456789"));
        absence.setEndDate(Timestamp.valueOf("2025-12-13 01:02:03.123456789"));
        absence.setApprover(approver);
        absence.setApprovedBy(approver);
        absence.setStatus(status);

        absenceRepository.save(absence);

    }

    // ------------------------------ GET ------------------------------
    @Test
    @WithMockUser
    void testGetAllAbsences() throws Exception {

        mockMvc.perform(get("/api/absences")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type.name").value("AbsenceType1"))
                .andExpect(jsonPath("$.content[0].user.name").value("test"))
                .andExpect(jsonPath("$.content[0].name").value("Ferias2025"))
                .andExpect(jsonPath("$.content[0].approvedDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].startDate").value("2025-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].endDate").value("2025-12-13T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].approver.name").value("Some Approver"))
                .andExpect(jsonPath("$.content[0].approvedBy.name").value("Some Approver"))
                .andExpect(jsonPath("$.content[0].status.name").value("PENDING"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser
    void testGetAbsenceById() throws Exception {
        mockMvc.perform(get("/api/absences/" + absence.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type.name").value("AbsenceType1"))
                .andExpect(jsonPath("$.user.name").value("test"))
                .andExpect(jsonPath("$.name").value("Ferias2025"))
                .andExpect(jsonPath("$.approvedDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.startDate").value("2025-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.endDate").value("2025-12-13T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.approver.name").value("Some Approver"))
                .andExpect(jsonPath("$.approvedBy.name").value("Some Approver"))
                .andExpect(jsonPath("$.status.name").value("PENDING"));
    }

    @Test
    @WithMockUser
    void testGetAbsenceNotFound() throws Exception {
        mockMvc.perform(get("/api/absences/333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_NOT_FOUND))
                .andExpect(status().isNotFound());
    }

    // ------------------------------ POST ------------------------------
    @Test
    @WithMockUser
    void testPostAbsence() throws Exception {

        Absence newAbsence = new Absence();
        newAbsence = new Absence();
        newAbsence.setType(absenceType);
        newAbsence.setSubType(absenceSubType);
        newAbsence.setUser(dummyUser);
        newAbsence.setApprover(approver);
        newAbsence.setName("Ferias2026");
        newAbsence.setRecordType("Day");
        newAbsence.setAbsenceHours(3.3);
        newAbsence.setApprovedDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        newAbsence.setStartDate(Timestamp.valueOf("2025-12-12 01:02:03.123456789"));
        newAbsence.setEndDate(Timestamp.valueOf("2025-12-13 01:02:03.123456789"));
        newAbsence.setStatus(status);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(post("/api/absences")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAbsence)))
                .andDo(print())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_CREATED_OK))
                .andExpect(status().isCreated()).andReturn();
    }

    /*
     * // ------------------------------ DELETE ------------------------------
     *
     * @Test
     * void testDeleteAbsence() throws Exception {
     * mockMvc.perform(delete("/api/absences/" + absence.getId())
     * .contentType(MediaType.APPLICATION_JSON))
     * .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_DELETED_OK))
     * .andExpect(status().isAccepted());
     * }
     *
     * @Test
     *
     * void testDeleteAbsenceNotFound() throws Exception {
     * mockMvc.perform(delete("/api/absences/777")
     * .contentType(MediaType.APPLICATION_JSON))
     * .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND))
     * .andExpect(status().isNotFound());
     * }
     */
    // ------------------------------ UPDATE ------------------------------
    @Test
    @WithMockUser
    void testUpdateAbsenceById() throws Exception {
        absenceType.setName("AbsenceType1");

        approver.setName("Some Approver");
        approver.setEmail("approver@email.com");
        approver.setCurrentYearVacationDays(23.0);
        approver.setPrevYearVacationDays(23.0);
        approver.setBirthdate(LocalDate.of(2007, 12, 03));

        absence.setType(absenceType);
        absence.setSubType(absenceSubType);
        absence.setUser(dummyUser);
        absence.setName("Ferias2027");
        absence.setRecordType("Day");
        absence.setApprovedDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        absence.setStartDate(Timestamp.valueOf("2025-12-12 01:02:03.123456789"));
        absence.setEndDate(Timestamp.valueOf("2025-12-13 01:02:03.123456789"));
        absence.setApprover(approver);
        absence.setApprovedBy(approver);
        absence.setStatus(status);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/absences/" + absence.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(absence)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_UPDATED_OK))
                .andExpect(jsonPath("$.data.type.name").value("AbsenceType1"))
                .andExpect(jsonPath("$.data.user.name").value("test"))
                .andExpect(jsonPath("$.data.name").value("Ferias2027"))
                .andExpect(jsonPath("$.data.approvedDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.startDate").value("2025-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-12-13T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.approver.name").value("Some Approver"))
                .andExpect(jsonPath("$.data.approvedBy.name").value("Some Approver"))
                .andExpect(jsonPath("$.data.status.name").value("PENDING"));
    }

    @Test
    @WithMockUser
    void testUpdateAbsenceByIdMissingVacationDays() throws Exception {
        absenceType.setName("VACATION");

        approver.setName("Some Approver");
        approver.setEmail("approver@email.com");
        approver.setCurrentYearVacationDays(23.0);
        approver.setPrevYearVacationDays(1.0);
        approver.setBirthdate(LocalDate.of(2007, 12, 03));

        absence.setType(absenceType);
        absence.setUser(dummyUser);
        absence.setName("Ferias2027");
        absence.setApprovedDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        absence.setStartDate(Timestamp.valueOf("2019-12-12 01:02:03.123456789"));
        absence.setEndDate(Timestamp.valueOf("2019-12-13 01:02:03.123456789"));
        absence.setWorkDays(2.0);
        absence.setStatus(status);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/absences/" + absence.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(absence)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_UPDATED_OK))
                .andExpect(jsonPath("$.data.type.name").value("VACATION"))
                .andExpect(jsonPath("$.data.user.name").value("test"))
                .andExpect(jsonPath("$.data.name").value("Ferias2027"))
                .andExpect(jsonPath("$.data.approvedDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.startDate").value("2019-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2019-12-13T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.approver.name").value("Some Approver"))
                .andExpect(jsonPath("$.data.approvedBy.name").value("Some Approver"))
                .andExpect(jsonPath("$.data.status.name").value("PENDING"));
    }

    @Test
    @WithMockUser
    void testUpdateAbsenceNotFound() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/absences/3333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(absence)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_NOT_FOUND));
    }

    @Test
    @WithMockUser
    void testImportVacations() throws Exception {

        String csvData = "userEmail,year,name,startDate,endDate,businessDays,status\n"
                + "test@somewhere,2025,test,2025-12-23 00:00:00,2025-12-24 00:00:00,2,DONE\n";

        mockMvc.perform(multipart("/api/absences/import")
                .file(new MockMultipartFile("file", "vacations.csv", "text/plain", csvData.getBytes()))
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.ABSENCE_CREATED_OK));

        mockMvc.perform(get("/api/absences")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].user.email").value("test@somewhere"))
                .andExpect(jsonPath("$.content[1].name").value("test"))
                .andExpect(jsonPath("$.content[1].type.name").value("VACATION"))
                .andExpect(jsonPath("$.content[1].startDate").value("2025-12-23T00:00:00.000+00:00"))
                .andExpect(jsonPath("$.content[1].endDate").value("2025-12-24T00:00:00.000+00:00"))
                .andExpect(jsonPath("$.content[1].workDays").value(2.0))
                .andExpect(jsonPath("$.content[1].status.name").value("DONE"));

        mockMvc.perform(get("/api/users/" + dummyUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentYearVacationDays").value(21.0))
                .andExpect(jsonPath("$.prevYearVacationDays").value(23.0));
    }
}
