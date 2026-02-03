package com.datacentric.timesense.utils.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.datacentric.timesense.model.User;
import com.datacentric.timesense.model.UserGroup;
import com.datacentric.timesense.model.UserRole;
import com.datacentric.timesense.repository.UserGroupRepository;
import com.datacentric.timesense.repository.UserRepository;
import com.datacentric.utils.CollectionUtils;

import jakarta.annotation.PostConstruct;

/**
 * The user security cache holds the minimal information required to honor
 * security for the users. Privileges are not cached here, only the roles and
 * user groups that the user has access to.
 */
@Service
public class UserSecurityCache {

    private static final long DEFAULT_CACHE_LIFETIME_MILLIS = 60_000L;

    private Environment env;
    private UserRepository userRepository;
    private UserGroupRepository userGroupRepository;

    private long cacheTimeToLive = DEFAULT_CACHE_LIFETIME_MILLIS;

    private ConcurrentHashMap<String, UserSecurityData> userCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, List<Long>> rolesPerUserGroup = new ConcurrentHashMap<>();

    @Autowired
    public UserSecurityCache(Environment env, UserRepository userRepository,
            UserGroupRepository userGroupRepository) {
        this.env = env;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
    }

    @PostConstruct
    public void init() {
        String cacheTimeToLiveStr = env.getProperty("server.security.cache.time-to-live");
        if (cacheTimeToLiveStr != null) {
            cacheTimeToLive = Long.parseLong(cacheTimeToLiveStr);
        }
    }

    /**
     * Get the user security data from the cache.
     *
     * If the data is not in the cache or it's stale, it will load it from the
     * database. The roles data returned from this method also includes the roles
     * that are associated with the user groups that the user belongs to.
     *
     * @param userEmail
     *                  Email of the user
     */
    public UserSecurityData getOrLoad(String userEmail) {
        UserSecurityData userData = userCache.get(userEmail);
        if (userData != null
                && System.currentTimeMillis() - userData.getLoadedTimestamp() < cacheTimeToLive) {
            return userData;
        }

        // The user is not in the cache, or it's stale so we just load it from
        // the database.
        Optional<User> userOptional = userRepository.findByEmail(userEmail);
        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();
        List<UserGroup> userGroupsList = CollectionUtils.nullSafeList(user.getUserGroups());
        List<UserRole> rolesList = CollectionUtils.nullSafeList(user.getUserRoles());
        List<Long> userGroups = userGroupsList.stream().map(UserGroup::getId).toList();
        Set<Long> roles = new TreeSet<>(rolesList.stream().map(UserRole::getId).toList());
        for (long userGroupId : userGroups) {
            // Expand the roles associated with the user groups
            List<Long> rolesPerGroup = getRolesPerUserGroup(userGroupId);
            roles.addAll(rolesPerGroup);
        }
        UserSecurityData userSecurityData = new UserSecurityData(user.getId(), userGroups,
                new ArrayList<>(roles));
        userCache.put(userEmail, userSecurityData);
        return userSecurityData;
    }

    public void invalidateUser(String userEmail) {
        userCache.remove(userEmail);
    }

    public void invalidateAllUsers() {
        userCache.clear();
    }

    public void invalidateUserGroup(long userGroupId) {
        rolesPerUserGroup.remove(userGroupId);
    }

    public void invalidateAllUserGroups() {
        rolesPerUserGroup.clear();
    }

    private List<Long> getRolesPerUserGroup(long userGroupId) {
        List<Long> roles = rolesPerUserGroup.get(userGroupId);
        if (roles != null) {
            return roles;
        }

        Optional<UserGroup> group = userGroupRepository.findById(userGroupId);
        if (group.isEmpty() || group.get().getUserRoles() == null) {
            roles = List.of();
        } else {
            roles = group.get().getUserRoles().stream().map(UserRole::getId).toList();
        }
        rolesPerUserGroup.put(userGroupId, roles);
        return roles;
    }

}
