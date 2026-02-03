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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
@SQLDelete(sql = "UPDATE projects SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class Project extends AuditableTable {

    public static final class Views {

        public interface Basic {
        }

        public interface Public extends Basic {
        }

        public interface Complete extends Public {
        }
    }

    public enum ProjectPermission {
        EDIT_PROJECTS, RECORD_TIME_PROJECTS, TIME_APPROVAL;

        @Override
        public String toString() {
            return name();
        }
    }

    @JsonView(Views.Basic.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Basic.class)
    @Column(name = ("name"), nullable = false)
    private String name;

    @JsonView(Views.Public.class)
    @Column(name = ("description"))
    private String description;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "projects_type_fk"))
    private ProjectType type;

    @JsonView(Views.Public.class)
    @ManyToOne
    @JoinColumn(name = "manager", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "projects_manager_fk"))
    private User manager;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "projects_client_fk"))
    private Client client;

    @JsonView(Views.Public.class)
    @Column(name = ("start_date"))
    private Timestamp startDate;

    @JsonView(Views.Public.class)
    @Column(name = ("expected_due_date"))
    private Timestamp expectedDueDate;

    @JsonView(Views.Public.class)
    @Column(name = ("end_date"))
    private Timestamp endDate;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "projects_status_fk"))
    private Status status;

    @JsonView(Views.Public.class)
    @Column(name = "real_budget")
    private Double realBudget;

    @JsonView(Views.Public.class)
    @ManyToMany
    @JoinTable(
        name = "project_tasks",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "project_task_id")
    )
    private List<ProjectTask> tasks = new ArrayList<>();

    @Column(name = "deleted")
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectType getProjectType() {
        return type;
    }

    public void setProjectType(ProjectType type) {
        this.type = type;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getExpectedDueDate() {
        return expectedDueDate;
    }

    public void setExpectedDueDate(Timestamp expectedDueDate) {
        this.expectedDueDate = expectedDueDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Double getRealBudget() {
        return realBudget;
    }

    public void setRealBudget(Double realBudget) {
        this.realBudget = realBudget;
    }

    public List<ProjectTask> getProjectTasks() {
        return tasks;
    }

    public void setProjectTasks(List<ProjectTask> tasks) {
        this.tasks = tasks;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @JsonIgnore
    public List<ValidationFailure> getValidationFailures() {
        List<ValidationFailure> failedValidations = new ArrayList<>();
        ValidationUtils.checkFieldFilled(name, "name", failedValidations);
        ValidationUtils.checkFieldFilled(type, "type", failedValidations);
        ValidationUtils.checkFieldFilled(manager, "manager", failedValidations);
        ValidationUtils.checkFieldFilled(client, "client", failedValidations);
        ValidationUtils.checkFieldFilled(status, "status", failedValidations);

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
        Project that = (Project) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
