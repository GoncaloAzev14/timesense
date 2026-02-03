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
@Table(name = "absences")
@SQLDelete(sql = "UPDATE absences SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class Absence extends AuditableTable {

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_type_id_fk"))
    private AbsenceType type;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_type_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_sub_types_fk"))
    private AbsenceSubType subType;

    @JsonView(Views.Public.class)
    @Column(name = ("record_type"))
    private String recordType;

    @JsonView(Views.Public.class)
    @Column(name = ("absence_hours"))
    private Double absenceHours;

    @JsonView(Views.Basic.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_user_id_fk"))
    private User user;

    @JsonView(Views.Basic.class)
    @Column(name = ("name"), nullable = false)
    private String name;

    @JsonView(Views.Public.class)
    @Column(name = ("approved_date"))
    private Timestamp approvedDate;

    @JsonView(Views.Public.class)
    @Column(name = ("start_date"), nullable = false)
    private Timestamp startDate;

    @JsonView(Views.Public.class)
    @Column(name = ("end_date"), nullable = false)
    private Timestamp endDate;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_approver_fk"))
    private User approver;

    @JsonView(Views.Public.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_approved_by_fk"))
    private User approvedBy;

    @JsonView(Views.Basic.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "absences_status_fk"))
    private Status status;

    @JsonView(Views.Public.class)
    @Column(name = ("reason"))
    private String reason;

    @JsonView(Views.Public.class)
    @Column(name = ("work_days"))
    private Double workDays;

    @JsonView(Views.Basic.class)
    @Column(name = ("business_year"))
    private String businessYear;

    @JsonView(Views.Public.class)
    @Column(name = ("observations"))
    private String observations;

    @JsonView(Views.Public.class)
    @Column(name = ("has_attachments"))
    private boolean hasAttachments;

    @Column(name = "deleted")
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AbsenceType getType() {
        return type;
    }

    public void setType(AbsenceType type) {
        this.type = type;
    }

    public AbsenceSubType getSubType() {
        return subType;
    }

    public void setSubType(AbsenceSubType subType) {
        this.subType = subType;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public Double getAbsenceHours() {
        return absenceHours;
    }

    public void setAbsenceHours(Double absenceHours) {
        this.absenceHours = absenceHours;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User userId) {
        this.user = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Timestamp approvedDate) {
        this.approvedDate = approvedDate;
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

    public User getApprover() {
        return approver;
    }

    public void setApprover(User approver) {
        this.approver = approver;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getWorkDays() {
        return workDays;
    }

    public void setWorkDays(Double workDays) {
        this.workDays = workDays;
    }

    public String getBusinessYear() {
        return businessYear;
    }

    public void setBusinessYear(String businessYear) {
        this.businessYear = businessYear;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public boolean getHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
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
        ValidationUtils.checkFieldFilled(type, "type_id", failedValidations);
        ValidationUtils.checkFieldFilled(user, "user_id", failedValidations);
        ValidationUtils.checkFieldFilled(startDate, "start_date", failedValidations);
        ValidationUtils.checkFieldFilled(endDate, "end_date", failedValidations);
        ValidationUtils.checkFieldFilled(approver, "approver", failedValidations);
        ValidationUtils.checkFieldFilled(status, "status", failedValidations);
        ValidationUtils.checkFieldFilled(name, "name", failedValidations);

        if (type != null && "ABSENCES".equals(type.getName())) {
            ValidationUtils.checkFieldFilled(absenceHours, "absence_hours", failedValidations);
            ValidationUtils.checkFieldFilled(subType, "sub_type", failedValidations);
            ValidationUtils.checkFieldFilled(reason, "reason", failedValidations);
        }

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
        Absence that = (Absence) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
