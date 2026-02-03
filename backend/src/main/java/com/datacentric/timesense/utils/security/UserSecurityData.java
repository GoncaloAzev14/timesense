package com.datacentric.timesense.utils.security;

import java.util.Collections;
import java.util.List;

import com.datacentric.timesense.model.AuditableTable;
import com.datacentric.timesense.model.User;

/**
 * This class holds only the minimal information related to the user security
 * to support our security cache.
 *
 * This is an immutable class.
 */
public class UserSecurityData {

    private long loadedTimestamp = System.currentTimeMillis();
    private long userId;
    private List<Long> userGroups;
    private List<Long> roles;

    public UserSecurityData(long userId, List<Long> userGroups, List<Long> roles) {
        this.userId = userId;
        this.userGroups = Collections.unmodifiableList(userGroups);
        this.roles = Collections.unmodifiableList(roles);
    }

    public long getId() {
        return userId;
    }

    public List<Long> getUserGroups() {
        return userGroups;
    }

    public List<Long> getRoles() {
        return roles;
    }

    public long getLoadedTimestamp() {
        return loadedTimestamp;
    }

    /**
     * Returns an instance of User containing only the key of the user for use
     * when creating the auditing relationships
     */
    public User toUserPlaceholder() {
        return new User(userId);
    }

    /**
     * Utility method to update the given table field createdBy to this user
     */
    public void markCreatedBy(AuditableTable table) {
        table.setCreatedBy(toUserPlaceholder());
    }

    /**
     * Utility method to update the given table field updatedBy to this user
     */
    public void markUpdatedBy(AuditableTable table) {
        table.setUpdatedBy(toUserPlaceholder());
    }

}
