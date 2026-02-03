package com.datacentric.timesense;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.ResourcePermission;
import com.datacentric.timesense.model.SystemAccessTypes;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ResourcePermissionRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserUtils userUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourcePermissionRepository resourcePermissionRepository;

    private User adminUser;
    private User user;

    @BeforeEach
    public void setup() {

        adminUser = new User();
        adminUser.setName("Some Admin");
        adminUser.setEmail("admin@email.com");
        adminUser.setCurrentYearVacationDays(23.0);
        adminUser.setPrevYearVacationDays(23.0);
        adminUser.setBirthdate(LocalDate.of(2007, 12, 03));
        userRepository.save(adminUser);

        user = new User();
        user.setName("Tiago Gouveia");
        user.setEmail("email@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        user.setLineManagerId(adminUser.getId());
        userRepository.save(user);

        ResourcePermission permission = new ResourcePermission();
        permission.setResourceType("System");
        permission.setResourceId(0L);
        permission.setAccessType(SystemAccessTypes.MANAGE_SECURITY);
        permission.setSubjectType("user");
        permission.setSubject(adminUser.getId());
        resourcePermissionRepository.save(permission);

        Mockito.when(userUtils.getOrCreateUser()).thenReturn(
                new UserSecurityData(adminUser.getId(), Collections.emptyList(),
                        Collections.emptyList()));
    }

    // ------------------------------ GET ------------------------------
    @Test
    @WithMockUser
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Some Admin"))
                .andExpect(jsonPath("$.content[0].email").value("admin@email.com"))
                .andExpect(jsonPath("$.content[0].birthdate").value("2007-12-03"))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + user.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tiago Gouveia"))
                .andExpect(jsonPath("$.email").value("email@email.com"))
                .andExpect(jsonPath("$.birthdate").value("2007-12-03"));
    }

    @Test
    @WithMockUser
    void testGetUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND))
                .andExpect(status().isNotFound());
    }

    // ------------------------------ POST ------------------------------
    @Test
    @WithMockUser
    void testPostUser() throws Exception {
        User newUser = new User();
        newUser.setName("Teste User");
        newUser.setEmail("test@email.com");
        newUser.setCurrentYearVacationDays(23.0);
        newUser.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));

        mockMvc.perform(post("/api/users")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(newUser)))
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_CREATED_OK))
                .andExpect(status().isCreated());
    }

    // ------------------------------ DELETE ------------------------------
//     @Test
//     @WithMockUser
//     void testDeleteUser() throws Exception {
//         mockMvc.perform(delete("/api/users/" + user.getId())
//                 .with(jwt().jwt(jwt -> jwt.claim("scope", "write")))
//                 .contentType(MediaType.APPLICATION_JSON))
//                 .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_DELETED_OK))
//                 .andExpect(status().isAccepted());
//     }
//     @Test
//     @WithMockUser
//     void testDeleteUserNotFound() throws Exception {
//         mockMvc.perform(delete("/api/users/777")
//                 .with(jwt().jwt(jwt -> jwt.claim("scope", "write")))
//                 .contentType(MediaType.APPLICATION_JSON))
//                 .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND))
//                 .andExpect(status().isNotFound());
//     }
    // ------------------------------ UPDATE ------------------------------
    @Test
    @WithMockUser
    void testUpdateUserById() throws Exception {
        user.setName("Tiago Updated");
        user.setEmail("updated@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        user.setBirthdate(LocalDate.of(2007, 12, 03));
        user.setAdmissionDate(LocalDate.of(2020, 1, 15));
        user.setExitDate(LocalDate.of(2020, 1, 20));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/users/" + user.getId())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_UPDATED_OK))
                .andExpect(jsonPath("$.data.name").value("Tiago Updated"))
                .andExpect(jsonPath("$.data.email").value("updated@email.com"))
                .andExpect(jsonPath("$.data.admissionDate").value("2020-01-15"))
                .andExpect(jsonPath("$.data.exitDate").value("2020-01-20"));
    }

    @Test
    @WithMockUser
    void testUpdateUserNotFound() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(put("/api/users/3333")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageCode").value(MessagesCodes.USER_NOT_FOUND));
    }

}
