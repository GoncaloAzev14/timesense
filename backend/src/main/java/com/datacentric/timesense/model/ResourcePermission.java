package com.datacentric.timesense.model;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource_permissions")
public class ResourcePermission extends AuditableTable {

    public final class Views {
        public interface Minimal extends AuditableTable.Views.List {
        }

        public interface Complete extends Minimal {
        }
    }

    public ResourcePermission() {
    }

    public ResourcePermission(String resourceType, Long resourceId, String accessType,
            String subjectType,
            Long subject, User createdBy) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accessType = accessType;
        this.subjectType = subjectType;
        this.subject = subject;
        this.setCreatedBy(createdBy);
    }

    @JsonView(Views.Minimal.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Minimal.class)
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @JsonView(Views.Minimal.class)
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @JsonView(Views.Minimal.class)
    @Column(name = "access_type", nullable = false)
    private String accessType;

    @JsonView(Views.Minimal.class)
    @Column(name = "subject_type", nullable = false)
    private String subjectType;

    @JsonView(Views.Minimal.class)
    @Column(name = "subject", nullable = false)
    private Long subject;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long id) {
        this.resourceId = id;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public Long getSubject() {
        return subject;
    }

    public void setSubject(Long subject) {
        this.subject = subject;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (id == null) {
            return false;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ResourcePermission that = (ResourcePermission) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
