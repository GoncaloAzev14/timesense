package com.datacentric.timesense.utils.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.datacentric.timesense.model.SystemSetting;
import com.datacentric.timesense.model.SystemSettings;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.SystemSettingRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.timesense.repository.UserRoleRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.security.UserSecurityCache;
import com.datacentric.timesense.utils.security.UserSecurityData;

@Service
public class UserUtils {

    private static final String DEFAULT_ADMIN_USERS = "server.initial.admin.allowed-users";

    private static final Double DEFAULT_VACATION_DAYS = 23.0;

    Logger logger = LoggerFactory.getLogger(UserUtils.class);

    private Environment env;
    private UserRepository userRepository;
    private UserRoleRepository roleRepository;
    private SecurityUtils securityUtils;
    private UserSecurityCache userSecurityCache;
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    public UserUtils(Environment env, UserRepository userRepository,
            UserRoleRepository roleRepository, SecurityUtils securityUtils,
            UserSecurityCache userSecurityCache,
            SystemSettingRepository systemSettingRepository) {
        this.env = env;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.securityUtils = securityUtils;
        this.userSecurityCache = userSecurityCache;
        this.systemSettingRepository = systemSettingRepository;
    }

    /**
     * Get the minimal information of the user to support access control and
     * auditing in the Controllers.
     *
     * If the user is not present in the database it will be created using the
     * definitions provided in the access token.
     *
     * It also ensures that, if the user groups of the user as reported in the
     * access token, are reflected in the database as well.
     *
     * @return UserSecurityData
     */
    public UserSecurityData getOrCreateUser() {
        Map<String, Object> userDetails = securityUtils.getUserAttributesFromJwt();
        String userName = (String) userDetails.get("userName");
        String userEmail = (String) userDetails.get("userEmail");
        @SuppressWarnings("unchecked")
        List<String> userGroups = (List<String>) userDetails.get("userGroups");

        UserSecurityData userData = userSecurityCache.getOrLoad(userEmail);
        if (userData != null) {

            // re-check the userGroups that came from the jwt to catch differences
            try {
                User user = userRepository.findById(userData.getId()).get();
                securityUtils.updateUserGroups(user, userGroups);

            } catch (Exception e) {
                logger.error("Error updating user groups", e);
            }

            return userData;
        }

        // The user was not found in the database. Create it nad then return it
        return createUser(userName, userEmail, userGroups);
    }

    private synchronized UserSecurityData createUser(String userName, String userEmail,
            List<String> userGroups) {

        // Just in case it has already been created in a different thread while
        // we were waiting for synchronization
        UserSecurityData userData = userSecurityCache.getOrLoad(userEmail);
        if (userData != null) {
            return userData;
        }

        // if user does not exist create new one
        logger.info("Creating user with name: {} and email: {}", userName, userEmail);
        User createUser = new User();
        createUser.setName(userName);
        createUser.setEmail(userEmail);

        // Only set the vacation days on user creation if the system setting
        // is enabled, otherwise leave them null
        SystemSetting setVacationDaysOnCreation = systemSettingRepository.findByName(
                SystemSettings.SET_VACATION_DAYS_ON_USER_CREATION);
        if (setVacationDaysOnCreation != null &&
                setVacationDaysOnCreation.getValue().equalsIgnoreCase("true")) {
            SystemSetting vacsDaysSetting = systemSettingRepository.findByName(
                    SystemSettings.DEFAULT_VACATION_DAYS);

            if (vacsDaysSetting != null) {
                Double vacationDays = Double.valueOf(vacsDaysSetting.getValue());
                createUser.setPrevYearVacationDays(vacationDays);
                createUser.setCurrentYearVacationDays(vacationDays);
            } else {
                createUser.setPrevYearVacationDays(DEFAULT_VACATION_DAYS);
                createUser.setCurrentYearVacationDays(DEFAULT_VACATION_DAYS);
            }
        } else {
            createUser.setPrevYearVacationDays(0.0);
            createUser.setCurrentYearVacationDays(0.0);
        }

        if (userEmail.matches(env.getProperty(DEFAULT_ADMIN_USERS, ""))) {
            UserRole adminRole = roleRepository.findByName("Admin");
            List<UserRole> roles = new ArrayList<>();
            roles.add(adminRole);
            createUser.setUserRoles(roles);
        } else {
            UserRole userRole = roleRepository.findByName("User");
            List<UserRole> defaultUserRole = new ArrayList<>();
            defaultUserRole.add(userRole);

            createUser.setUserRoles(defaultUserRole);
        }

        try {
            securityUtils.updateUserGroups(createUser, userGroups);
            userRepository.save(createUser);

            // Now that we created the user, we can load it from the database
            return userSecurityCache.getOrLoad(userEmail);
        } catch (Exception e) {
            logger.error("Error creating user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.USER_CREATED_ERROR);
        }
    }
}
