
package com.datacentric.timesense;

import java.util.Arrays;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.controller.ResourcePermissionController.PermissionsMap;
import com.datacentric.timesense.model.ResourcePermission;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.repository.ResourcePermissionRepository;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.repository.UserGroupRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissionsControllerTests extends SecurityBaseClass {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private ResourcePermissionRepository resourcePermissionRepository;

    private User user;
    private UserGroup userGroup;
    private UserRole role;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setName("Tiago Gouveia");
        user.setEmail("email@email.com");
        user.setCurrentYearVacationDays(23.0);
        user.setPrevYearVacationDays(23.0);
        userRepository.save(user);

        role = new UserRole();
        role.setName("Admin");
        roleRepository.save(role);

        userGroup = new UserGroup();
        userGroup.setName("Admins");
        userGroupRepository.save(userGroup);

        resourcePermissionRepository
                .saveAll(Arrays.asList(
                        new ResourcePermission("app", 1L, "read", "user", user.getId(), user),
                        new ResourcePermission("app", 1L, "write", "group", userGroup.getId(),
                                user),
                        new ResourcePermission("app", 1L, "delete", "role", role.getId(), user)));

        savePermission("System", 0L, "MANAGE_SECURITY", "user", dummyUser.getId());                        
    }

    // ------------------------------ GET ------------------------------

    @Test
    @WithMockUser
    void testGetResourcePermissions() throws Exception {

        mockMvc.perform(get("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read.user[0].id").value(user.getId()))
                .andExpect(jsonPath("$.read.user[0].name").value(user.getName()))
                .andExpect(jsonPath("$.write.group[0].id").value(userGroup.getId()))
                .andExpect(jsonPath("$.write.group[0].name").value(userGroup.getName()))
                .andExpect(jsonPath("$.delete.role[0].id").value(role.getId()))
                .andExpect(jsonPath("$.delete.role[0].name").value(role.getName()));
    }

    @Test
    @WithMockUser
    void testPostEmptyPermissionsReplacesExistingPermissions() throws Exception {
        PermissionsMap value = new PermissionsMap();
        mockMvc.perform(post("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(value)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void testPostNewPermissionsReplacesExistingPermissions() throws Exception {
        String value = "{\"read\":" +
                "{\"group\":[{\"id\":" + userGroup.getId() + ",\"name\":\"" + userGroup.getName()
                + "\"}]}," +
                "\"write\":" +
                "{\"user\":[{\"id\":" + user.getId() + ",\"name\":\"" + user.getName() + "\"}]}," +
                "\"delete\":{\"role\":[{\"id\":" + role.getId() + ",\"name\":\"" + role.getName()
                + "\"}]}}";
        mockMvc.perform(post("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(value))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read.group[0].id").value(userGroup.getId()))
                .andExpect(jsonPath("$.read.group[0].name").value(userGroup.getName()))
                .andExpect(jsonPath("$.read.user").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.write.user[0].id").value(user.getId()))
                .andExpect(jsonPath("$.write.user[0].name").value(user.getName()))
                .andExpect(jsonPath("$.write.group").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.delete.role[0].id").value(role.getId()))
                .andExpect(jsonPath("$.delete.role[0].name").value(role.getName()));
    }

    @Test
    @WithMockUser
    void testPostNewPermissionsWithMultipleValues() throws Exception {
        UserRole role2 = new UserRole();
        role2.setName("Manager");
        roleRepository.save(role2);
        String value = "{\"read\":" +
                "{\"group\":[{\"id\":" + userGroup.getId() + ",\"name\":\"" + userGroup.getName()
                + "\"}]}," +
                "\"write\":" +
                "{\"user\":[{\"id\":" + user.getId() + ",\"name\":\"" + user.getName() + "\"}]}," +
                "\"delete\":{\"role\":[{\"id\":" + role.getId() + ",\"name\":\"" + role.getName()
                + "\"},{\"id\":" + role2.getId() + ",\"name\":\"" + role2.getName()
                + "\"}]}}";
        mockMvc.perform(post("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(value))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/permissions/app/1")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "read")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read.group[0].id").value(userGroup.getId()))
                .andExpect(jsonPath("$.read.group[0].name").value(userGroup.getName()))
                .andExpect(jsonPath("$.read.user").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.write.user[0].id").value(user.getId()))
                .andExpect(jsonPath("$.write.user[0].name").value(user.getName()))
                .andExpect(jsonPath("$.write.group").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.delete.role[0].id").value(role.getId()))
                .andExpect(jsonPath("$.delete.role[0].name").value(role.getName()))
                .andExpect(jsonPath("$.delete.role[1].id").value(role2.getId()))
                .andExpect(jsonPath("$.delete.role[1].name").value(role2.getName()));
    }
}
