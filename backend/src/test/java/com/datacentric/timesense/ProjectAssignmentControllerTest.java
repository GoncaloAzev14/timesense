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
import com.datacentric.timesense.model.ProjectAssignment;
import com.datacentric.timesense.model.ProjectType;
import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ClientRepository;
import com.datacentric.timesense.repository.ProjectAssignmentRepository;
import com.datacentric.timesense.repository.ProjectRepository;
import com.datacentric.timesense.repository.ProjectTypeRepository;
import com.datacentric.timesense.repository.StatusRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectAssignmentControllerTests extends SecurityBaseClass {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    private User user;
    private Status status;
    private Client client;
    private ProjectType projectType;
    private Project project;
    private ProjectAssignment projectAssignment;

    @BeforeEach
    public void setup() {

        savePermission("System", 0L,
                SystemAccessTypes.CREATE_PROJECTS, "user", dummyUser.getId());

        user = new User();
        user.setName("Some Manager");
        user.setEmail("manager@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(user);

        projectType = new ProjectType();
        projectType.setName("type1");
        projectTypeRepository.save(projectType);


        status = new Status();
        status.setName("STATUS");
        statusRepository.save(status);

        client = new Client();
        client.setName("client1");
        clientRepository.save(client);

        project = new Project();
        project.setName("Proj1");
        project.setProjectType(projectType);
        project.setManager(user);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        project.setStatus(status);
        projectRepository.save(project);

        projectAssignment = new ProjectAssignment();
        projectAssignment.setUser(user);
        projectAssignment.setProject(project);
        projectAssignment.setAllocation(50.0);
        projectAssignment.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        projectAssignment.setEndDate(Timestamp.valueOf("2019-12-12 01:02:03.123456789"));
        projectAssignmentRepository.save(projectAssignment);

    }

    // ------------------------------ GET ------------------------------
    @Test
    @WithMockUser
    void testGetAllProjectAssignments() throws Exception {

        mockMvc.perform(get("/api/project-assignments")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].user.name").value("Some Manager"))
                .andExpect(jsonPath("$.content[0].project.name").value("Proj1"))
                .andExpect(jsonPath("$.content[0].allocation").value("50.0"))
                .andExpect(jsonPath("$.content[0].startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].endDate").value("2019-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser
    void testGetProjectAssignmentsById() throws Exception {
        mockMvc.perform(get("/api/project-assignments/" + projectAssignment.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Some Manager"))
                .andExpect(jsonPath("$.project.name").value("Proj1"))
                .andExpect(jsonPath("$.allocation").value("50.0"))
                .andExpect(jsonPath("$.startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.endDate").value("2019-12-12T01:02:03.123+00:00")).andReturn();

    }

    @Test
    @WithMockUser
    void testGetProjectAssignmentsNotFound() throws Exception {
        mockMvc.perform(get("/api/project-assignments/333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND))
                .andExpect(status().isNotFound());
    }

    // ------------------------------ POST ------------------------------
    @Test
    @WithMockUser
    void testPostProjectAssignments() throws Exception {

        ProjectAssignment newProjectAssignment = new ProjectAssignment();
        newProjectAssignment = new ProjectAssignment();
        newProjectAssignment.setUser(user);
        newProjectAssignment.setProject(project);
        newProjectAssignment.setAllocation(50.0);
        newProjectAssignment.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        newProjectAssignment.setEndDate(Timestamp.valueOf("2019-12-12 01:02:03.123456789"));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(post("/api/project-assignments")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProjectAssignment)))
                .andDo(print())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_ASSIGNMENT_CREATED_OK))
                .andExpect(status().isCreated()).andReturn();
    }

    /*
    // ------------------------------ DELETE ------------------------------

    @Test
    void testDeleteProject() throws Exception {
        mockMvc.perform(delete("/api/projects/" + project.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_DELETED_OK))
                .andExpect(status().isAccepted());
    }

    @Test

    void testDeleteProjectNotFound() throws Exception {
        mockMvc.perform(delete("/api/projects/777")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND))
                .andExpect(status().isNotFound());
    }
     */
    // ------------------------------ UPDATE ------------------------------
    @Test
    @WithMockUser
    void testUpdateProjectAssignmentById() throws Exception {
        user.setName("Some Manager1");
        user.setEmail("manager@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(user);

        status.setName("STATUS");
        statusRepository.save(status);

        client.setName("client1");
        clientRepository.save(client);

        projectType.setName("type1");
        projectTypeRepository.save(projectType);

        project.setName("Proj2");
        project.setProjectType(projectType);
        project.setManager(user);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        project.setStatus(status);
        projectRepository.save(project);

        projectAssignment = new ProjectAssignment();
        projectAssignment.setUser(user);
        projectAssignment.setProject(project);
        projectAssignment.setAllocation(50.0);
        projectAssignment.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        projectAssignment.setEndDate(Timestamp.valueOf("2020-12-12 01:02:03.123456789"));
        projectAssignmentRepository.save(projectAssignment);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/project-assignments/" + projectAssignment.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectAssignment)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_ASSIGNMENT_UPDATED_OK))
                .andExpect(jsonPath("$.data.user.name").value("Some Manager1"))
                .andExpect(jsonPath("$.data.project.name").value("Proj2"))
                .andExpect(jsonPath("$.data.allocation").value("50.0"))
                .andExpect(jsonPath("$.data.startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2020-12-12T01:02:03.123+00:00")).andReturn();
    }

    @Test
    @WithMockUser
    void testUpdateProjectAssignmentNotFound() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/project-assignments/3333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_ASSIGNMENT_NOT_FOUND));
    }

}
