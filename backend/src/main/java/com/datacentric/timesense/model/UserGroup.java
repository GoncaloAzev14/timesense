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
import jakarta.persistence.Table;

@Entity
@Table(name = "user_groups")
@SQLDelete(sql = "UPDATE user_groups SET deleted = true WHERE id = ?")
@SQLRestriction(value = "deleted=false")
public class UserGroup extends AuditableTable {

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

    @JsonView(Views.List.class)
    @Column(name = "tokenId")
    private String tokenId;

    @JsonView(Views.Minimal.class)
    @Column(name = "name", nullable = false)
    private String name;

    @JsonView(Views.Complete.class)
    @ManyToMany
    @JoinTable(name = "user_group_roles", joinColumns = @JoinColumn(name = "user_group_id", 
        referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role_id", 
        referencedColumnName = "id"))
    List<UserRole> roles;

    @Column(name = "deleted")
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserRole> getUserRoles() {
        return roles;
    }

    public void setUserRoles(List<UserRole> roles) {
        this.roles = roles;
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
        UserGroup that = (UserGroup) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
