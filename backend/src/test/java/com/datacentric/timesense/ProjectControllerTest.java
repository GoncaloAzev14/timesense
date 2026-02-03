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
import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ClientRepository;
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
class ProjectControllerTests extends SecurityBaseClass {

    @Autowired
    private MockMvc mockMvc;

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
    
    private User managerProject;
    private ProjectType projectType;
    private Status status;
    private Client client;
    private Project project;

    @BeforeEach
    public void setup() {

        savePermission("System", 0L,
                SystemAccessTypes.CREATE_PROJECTS, "user", dummyUser.getId());

        managerProject = new User();
        managerProject.setName("Some Manager");
        managerProject.setEmail("manager@email.com");
        managerProject.setCurrentYearVacationDays(23.0);
        managerProject.setPrevYearVacationDays(23.0);
        managerProject.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(managerProject);

        projectType = new ProjectType();
        projectType.setName("type1");
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
        project.setManager(managerProject);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2018-12-12 01:02:03.123456789"));
        project.setStatus(status);
        projectRepository.save(project);

    }

    // ------------------------------ GET ------------------------------
    @Test
    @WithMockUser
    void testGetAllProjects() throws Exception {

        mockMvc.perform(get("/api/projects")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Proj1"))
                .andExpect(jsonPath("$.content[0].type.name").value("type1"))
                .andExpect(jsonPath("$.content[0].manager.name").value("Some Manager"))
                .andExpect(jsonPath("$.content[0].client.name").value("Tiago"))
                .andExpect(jsonPath("$.content[0].startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.content[0].status.name").value("Active"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser
    void testGetProjectById() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Proj1"))
                .andExpect(jsonPath("$.type.name").value("type1"))
                .andExpect(jsonPath("$.manager.name").value("Some Manager"))
                .andExpect(jsonPath("$.client.name").value("Tiago"))
                .andExpect(jsonPath("$.startDate").value("2018-12-12T01:02:03.123+00:00"))
                .andExpect(jsonPath("$.status.name").value("Active"));
    }

    @Test
    @WithMockUser
    void testGetProjectNotFound() throws Exception {
        mockMvc.perform(get("/api/projects/333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_NOT_FOUND))
                .andExpect(status().isNotFound());
    }

    // ------------------------------ POST ------------------------------
    @Test
    @WithMockUser
    void testPostProject() throws Exception {

        Project newProject = new Project();
        newProject.setName("Proj2");
        newProject.setProjectType(projectType);
        newProject.setManager(managerProject);
        newProject.setClient(client);
        newProject.setStartDate(Timestamp.valueOf("2022-12-12 01:02:03.123456789"));
        newProject.setStatus(status);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(post("/api/projects")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProject)))
                .andDo(print())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_CREATED_OK))
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
    void testUpdateProjectById() throws Exception {
        managerProject.setName("Some Manager");
        managerProject.setEmail("manager@email.com");
        managerProject.setCurrentYearVacationDays(23.0);
        managerProject.setPrevYearVacationDays(23.0);
        managerProject.setBirthdate(LocalDate.of(2007, 12, 03));

        projectType.setName("type1");

        status.setName("Sleep");

        client.setName("Austino");

        project.setName("Proj3");
        project.setProjectType(projectType);
        project.setManager(managerProject);
        project.setClient(client);
        project.setStartDate(Timestamp.valueOf("2025-12-12 01:02:03"));
        project.setStatus(status);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/projects/" + project.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_UPDATED_OK))
                .andExpect(jsonPath("$.data.name").value("Proj3"))
                .andExpect(jsonPath("$.data.type.name").value("type1"))
                .andExpect(jsonPath("$.data.manager.name").value("Some Manager"))
                .andExpect(jsonPath("$.data.client.name").value("Austino"))
                .andExpect(jsonPath("$.data.startDate").value("2025-12-12T01:02:03.000+00:00"))
                .andExpect(jsonPath("$.data.status.name").value("Sleep")).andReturn();
    }

    @Test
    @WithMockUser
    void testUpdateProjectNotFound() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/projects/3333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.PROJECT_NOT_FOUND));
    }

}
