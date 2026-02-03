package com.datacentric.timesense.model;

import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
@SQLDelete(sql = "UPDATE tasks SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class ProjectTask {

    public static final class Views {

        public interface Basic {
        }
    }

    @JsonView(Views.Basic.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Basic.class)
    @Column(name = ("name"))
    private String name;

    @JsonView(Views.Basic.class)
    @Column(name = ("description"))
    private String description;

    @JsonView(Views.Basic.class)
    @ManyToOne
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    private User updatedBy;

    @Column(name = "deleted")
    private boolean deleted;

    @JsonView(Views.Basic.class)
    @ManyToMany
    @JoinTable(name = "project_type_tasks",
                joinColumns = @JoinColumn(name = "project_task_id"),
                inverseJoinColumns = @JoinColumn(name = "project_type_id")
    )
    List<ProjectType> projectTypes;

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

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<ProjectType> getProjectTypes() {
        return projectTypes;
    }

    public void setProjectTypes(List<ProjectType> projectTypes) {
        this.projectTypes = projectTypes;
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
        ProjectTask that = (ProjectTask) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
}
