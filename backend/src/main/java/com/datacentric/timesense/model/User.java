package com.datacentric.timesense.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class User {

    public static final class Views {
        public interface Basic {
        }

        public interface Public extends Basic {
        }

        public interface Complete extends Public {
        }
    }

    @JsonView(Views.Basic.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Basic.class)
    @Column(name = "name", nullable = false)
    private String name;

    @JsonView(Views.Public.class)
    @Column(name = "birthdate")
    private LocalDate birthdate;

    @JsonView(Views.Public.class)
    @Column(name = "email", nullable = false)
    private String email;

    @JsonView(Views.Public.class)
    @Transient
    @ManyToOne
    @JoinColumn(name = "line_manager", referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "user_line_manager_fk"))
    private User lineManager;

    @JsonView(Views.Public.class)
    @Column(name = "line_manager")
    private Long lineManagerId;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_title", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "user_job_title_fk"))
    private JobTitle jobTitle;

    @JsonView(Views.Basic.class)
    @Column(name = "current_year_vacation_days", nullable = false)
    private Double currentYearVacationDays;

    @JsonView(Views.Basic.class)
    @Column(name = "prev_year_vacation_days")
    private Double prevYearVacationDays;

    @JsonView(Views.Basic.class)
    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @JsonView(Views.Basic.class)
    @Column(name = "exit_date")
    private LocalDate exitDate;

    @JsonView(Views.Complete.class)
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Timestamp createdAt;

    @JsonView(Views.Complete.class)
    @Column(name = "updated_at")
    @UpdateTimestamp
    private Timestamp updatedAt;

    @JsonView(Views.Complete.class)
    @ManyToMany
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
               inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    List<UserRole> userRoles;

    @JsonView(Views.Complete.class)
    @ManyToMany
    @JoinTable(name = "user_user_groups",
               joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
               inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"))
    List<UserGroup> userGroups;

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

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User getLineManager() {
        return lineManager;
    }

    public void setLineManager(User lineManager) {
        this.lineManager = lineManager;
    }

    public Long getLineManagerId() {
        return lineManagerId;
    }

    public void setLineManagerId(Long lineManagerId) {
        this.lineManagerId = lineManagerId;
    }

    public Double getCurrentYearVacationDays() {
        return currentYearVacationDays;
    }

    public void setCurrentYearVacationDays(Double currentYearVacationDays) {
        this.currentYearVacationDays = currentYearVacationDays;
    }

    public Double getPrevYearVacationDays() {
        return prevYearVacationDays;
    }

    public void setPrevYearVacationDays(Double prevYearVacationDays) {
        this.prevYearVacationDays = prevYearVacationDays;
    }

    public JobTitle getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(JobTitle jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public List<UserGroup> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }

    public User() {
    }

    public User(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public List<ValidationFailure> getValidationFailures() {
        List<ValidationFailure> failedValidations = new ArrayList<>();
        ValidationUtils.checkFieldFilled(name, "name", failedValidations);
        ValidationUtils.checkFieldFilled(email, "email", failedValidations);
        // ValidationUtils.checkFieldFilled(birthdate, "birthdate", failedValidations);

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
        User that = (User) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
