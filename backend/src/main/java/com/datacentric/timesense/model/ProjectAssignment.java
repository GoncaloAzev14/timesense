package com.datacentric.timesense.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.datacentric.timesense.utils.rest.ValidationUtils;
import com.datacentric.utils.rest.ValidationFailure;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_assignments")
@SQLDelete(sql = "UPDATE project_assignments SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class ProjectAssignment extends AuditableTable {

    private static final double HUNDRED = 100.0;

    public static final class Views {

        public interface Public {
        }

        public interface Complete extends Public {
        }
    }

    @JsonView(Views.Public.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "project_assignments_user_id_fk"))
    private User user;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "project_assignments_project_id_fk"))
    private Project project;

    @JsonView(Views.Public.class)
    @Column(name = ("allocation"), nullable = false)
    private Double allocation;

    @JsonView(Views.Public.class)
    @Column(name = ("description"))
    private String description;

    @JsonView(Views.Public.class)
    @Column(name = ("start_date"), nullable = false)
    private Timestamp startDate;

    @JsonView(Views.Public.class)
    @Column(name = ("end_date"), nullable = false)
    private Timestamp endDate;

    @Column(name = "deleted")
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Double getAllocation() {
        return allocation;
    }

    public void setAllocation(Double allocation) {
        this.allocation = allocation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Double getAllocationPercentage() {
        return allocation != null ? allocation / HUNDRED : null;
    }

    @JsonIgnore
    public List<ValidationFailure> getValidationFailures() {
        List<ValidationFailure> failedValidations = new ArrayList<>();
        ValidationUtils.checkFieldFilled(user, "user_id", failedValidations);
        ValidationUtils.checkFieldFilled(project, "project_id", failedValidations);
        ValidationUtils.checkFieldFilled(allocation, "allocation", failedValidations);
        ValidationUtils.checkFieldFilled(startDate, "start_date", failedValidations);
        ValidationUtils.checkFieldFilled(endDate, "end_date", failedValidations);

        return failedValidations;
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
        ProjectAssignment that = (ProjectAssignment) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
