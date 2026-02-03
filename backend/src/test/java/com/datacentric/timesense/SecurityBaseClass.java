package com.datacentric.timesense;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.datacentric.timesense.model.JobTitle;
import com.datacentric.timesense.model.ResourcePermission;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.JobTitleRepository;
import com.datacentric.timesense.repository.ResourcePermissionRepository;
import com.datacentric.timesense.repository.UserGroupRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;

/**
 * This class is the base for all tests that require users to be setup in the
 * system.
 *
 */
public class SecurityBaseClass {
    @MockBean
    protected UserUtils userUtils;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private ResourcePermissionRepository permissionRepository;

    @Autowired
    private JobTitleRepository jobTitleRepository;

    protected User dummyUser;

    @BeforeEach
    public void setupUsers() {
        UserRole role = new UserRole();
        role.setName("User");
        roleRepository.save(role);

        UserGroup userGroup = new UserGroup();
        userGroup.setName("group1");
        userGroupRepository.save(userGroup);

        JobTitle jobTitle = new JobTitle();
        jobTitle.setName("Internship");
        jobTitleRepository.save(jobTitle);

        List<UserRole> roles = new ArrayList<>();
        List<UserGroup> userGroups = new ArrayList<>();
        roles.add(role);
        userGroups.add(userGroup);
        dummyUser = new User();
        dummyUser.setName("test");
        dummyUser.setEmail("test@somewhere");
        dummyUser.setCurrentYearVacationDays(23.0);
        dummyUser.setPrevYearVacationDays(23.0);
        dummyUser.setUserRoles(roles);
        dummyUser.setUserGroups(userGroups);
        dummyUser.setJobTitle(jobTitle);
        userRepository.save(dummyUser);

        Mockito.when(userUtils.getOrCreateUser()).thenReturn(
                new UserSecurityData(dummyUser.getId(), Arrays.asList(userGroup.getId()),
                        Arrays.asList(role.getId())));
    }

    protected void savePermission(String resourceType, Long resourceId, String accessType,
            String subjectType, Long subjectId) {
        ResourcePermission permission = new ResourcePermission();
        permission.setResourceType(resourceType);
        permission.setResourceId(resourceId);
        permission.setAccessType(accessType);
        permission.setSubjectType(subjectType);
        permission.setSubject(subjectId);
        permissionRepository.save(permission);
    }
}
