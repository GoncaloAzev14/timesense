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
@Table(name = "time_records")
@SQLDelete(sql = "UPDATE time_records SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class TimeRecord extends AuditableTable {


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
            foreignKey = @ForeignKey(name = "time_records_user_id_fk"))
    private User user;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "time_records_project_id_fk"))
    private Project project;

    @JsonView(Views.Public.class)
    @Column(name = ("hours"), nullable = false)
    private Double hours;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "time_record_tasks_fk"))
    private ProjectTask task;

    @JsonView(Views.Public.class)
    @Column(name = ("description"))
    private String description;

    @JsonView(Views.Public.class)
    @Column(name = ("reason"))
    private String reason;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "time_records_status_id_fk"))
    private Status status;

    @JsonView(Views.Public.class)
    @Column(name = ("start_date"), nullable = false)
    private Timestamp startDate;

    @JsonView(Views.Public.class)
    @Column(name = ("end_date"), nullable = false)
    private Timestamp endDate;

    @JsonView(Views.Complete.class)
    @Column(name = ("approved_at"))
    private Timestamp approvedAt;

    @JsonView(Views.Complete.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "time_records_approved_by_fk"))
    private User approvedBy;

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

    public ProjectTask getTask() {
        return task;
    }

    public void setTask(ProjectTask task) {
        this.task = task;
    }

    public Double getHours() {
        return hours;
    }

    public void setHours(Double hours) {
        this.hours = hours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
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
        ValidationUtils.checkFieldFilled(hours, "hours", failedValidations);
        ValidationUtils.checkFieldFilled(status, "status", failedValidations);
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
        TimeRecord that = (TimeRecord) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
