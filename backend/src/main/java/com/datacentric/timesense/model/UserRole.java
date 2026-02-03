package com.datacentric.timesense.model;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
@SQLDelete(sql = "UPDATE roles SET deleted = true WHERE id = ?")
@SQLRestriction(value = "deleted=false")
public class UserRole extends AuditableTable {

    public interface Views {
        interface Minimal {
        }

        interface List extends Minimal, AuditableTable.Views.List {
        }

        interface Complete extends List {
        }
    }

    @JsonView(Views.Minimal.class)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JsonView(Views.Minimal.class)
    @Column(name = "name", nullable = false)
    private String name;

    @JsonView(Views.Minimal.class)
    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id", updatable = false, 
        foreignKey = @ForeignKey(name = "roles_parent_fk"))
    private UserRole parentRole;
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

    public UserRole getParentRole() {
        return parentRole;
    }

    public void setParentRole(UserRole parentRole) {
        this.parentRole = parentRole;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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
        UserRole that = (UserRole) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
