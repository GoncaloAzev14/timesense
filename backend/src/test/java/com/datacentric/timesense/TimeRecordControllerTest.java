package com.datacentric.timesense;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Client;
import com.datacentric.timesense.model.Project;
import com.datacentric.timesense.model.ProjectType;
import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.TimeRecord;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ClientRepository;
import com.datacentric.timesense.repository.ProjectRepository;
import com.datacentric.timesense.repository.ProjectTypeRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.TimeRecordRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimeRecordControllerTests extends SecurityBaseClass {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TimeRecordRepository timeRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private ProjectTypeRepository projectTypeRepository;
    
    private User user;
    private ProjectType projectType;
    private Status status;
    private Client client;
    private Project project;
    private TimeRecord timeRecord;

    @BeforeEach
    public void setup() {

        user = new User();
        user.setName("Some User");
        user.setEmail("user@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(user);

        
        projectType = new ProjectType();
        projectType.setName("Type1");
        projectTypeRepository.save(projectType);

        status = new Status();
        status.setName("Active");
        statusRepository.save(status);

        client = new Client();
        client.setName("Tiago");
        clientRepository.save(client);

        project = new Project();
        project.setName("Proj1");
        project.setProjectType(projectType);
        project.setManager(dummyUser);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        project.setStatus(status);
        projectRepository.save(project);

        timeRecord = new TimeRecord();
        timeRecord.setUser(user);
        timeRecord.setProject(project);
        timeRecord.setHours(100.0);
        timeRecord.setDescription("description");
        timeRecord.setStatus(status);
        timeRecord.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        timeRecord.setEndDate(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        timeRecord.setApprovedAt(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        timeRecord.setApprovedBy(user);
        timeRecordRepository.save(timeRecord);

        savePermission("Project", project.getId(),
                Project.ProjectPermission.RECORD_TIME_PROJECTS.toString(), "user", dummyUser.getId());

        savePermission("Project", project.getId(),
                Project.ProjectPermission.EDIT_PROJECTS.toString(), "user", dummyUser.getId());

    }

    // ------------------------------ GET ------------------------------
    @Test
    @WithMockUser
    void testGetAllTimeRecords() throws Exception {

        mockMvc.perform(get("/api/time-records")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].user.name").value("Some User"))
                .andExpect(jsonPath("$.content[0].project.name").value("Proj1"))
                .andExpect(jsonPath("$.content[0].hours").value("100.0"))
                .andExpect(jsonPath("$.content[0].status.name").value("Active"))
                .andExpect(jsonPath("$.content[0].startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].endDate").value("2020-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser
    void testGetTimeRecordById() throws Exception {
        mockMvc.perform(get("/api/time-records/" + timeRecord.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Some User"))
                .andExpect(jsonPath("$.project.name").value("Proj1"))
                .andExpect(jsonPath("$.hours").value("100.0"))
                .andExpect(jsonPath("$.description").value("description"))
                .andExpect(jsonPath("$.status.name").value("Active"))
                .andExpect(jsonPath("$.startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.endDate").value("2020-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.approvedAt").value("2020-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.approvedBy.name").value("Some User"));
    }

    @Test
    @WithMockUser
    void testGetTimeRecordNotFound() throws Exception {
        mockMvc.perform(get("/api/time-records/333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.TIME_RECORD_NOT_FOUND))
                .andExpect(status().isNotFound());
    }

    // ------------------------------ POST ------------------------------
    @Test
    @WithMockUser
    void testPostTimeRecord() throws Exception {

        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setUser(user);
        newTimeRecord.setProject(project);
        newTimeRecord.setHours(100.0);
        newTimeRecord.setDescription("description");
        newTimeRecord.setStatus(status);
        newTimeRecord.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        newTimeRecord.setEndDate(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        newTimeRecord.setApprovedAt(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        newTimeRecord.setApprovedBy(user);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(post("/api/time-records")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTimeRecord)))
                .andDo(print())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.TIME_RECORD_CREATED_OK))
                .andExpect(status().isCreated()).andReturn();
    }

    /*
    // ------------------------------ DELETE ------------------------------

    @Test
    void testDeleteTimeRecord() throws Exception {
        mockMvc.perform(delete("/api/timeRecords/" + timeRecord.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_DELETED_OK))
                .andExpect(status().isAccepted());
    }

    @Test

    void testDeleteTimeRecordNotFound() throws Exception {
        mockMvc.perform(delete("/api/timeRecords/777")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND))
                .andExpect(status().isNotFound());
    }
     */
    // ------------------------------ UPDATE ------------------------------
    @Test
    @WithMockUser
    void testUpdateTimeRecordById() throws Exception {
        user.setName("Some User");
        user.setEmail("user@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(user);

        project.setName("Proj1");
        project.setProjectType(projectType);
        project.setManager(user);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        project.setStatus(status);

        timeRecord.setUser(user);
        timeRecord.setProject(project);
        timeRecord.setHours(100.0);
        timeRecord.setDescription("description");
        timeRecord.setStatus(status);
        timeRecord.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        timeRecord.setEndDate(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        timeRecord.setApprovedAt(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        timeRecord.setApprovedBy(user);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/time-records/" + timeRecord.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timeRecord)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.TIME_RECORD_UPDATED_OK))
                .andExpect(jsonPath("$.data.user.name").value("Some User"))
                .andExpect(jsonPath("$.data.project.name").value("Proj1"))
                .andExpect(jsonPath("$.data.hours").value("100.0"))
                .andExpect(jsonPath("$.data.description").value("description"))
                .andExpect(jsonPath("$.data.status.name").value("Active"))
                .andExpect(jsonPath("$.data.startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2020-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.approvedAt").value("2020-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.approvedBy.name").value("Some User"));
    }

    @Test
    @WithMockUser
    void testUpdateTimeRecordNotFound() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/time-records/3333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timeRecord)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.TIME_RECORD_NOT_FOUND));
    }

}
