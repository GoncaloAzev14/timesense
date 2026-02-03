package com.datacentric.timesense.model;

import java.time.LocalDate;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "holidays")
@SQLDelete(sql = "UPDATE holidays SET deleted = true WHERE id=?")
@SQLRestriction(value = "deleted=false")
public class Holiday {

    public static final class Views {

        public interface Public {
        }
    }

    @JsonView(Views.Public.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(Views.Public.class)
    @Column(name = "holiday_date")
    private LocalDate holidayDate;

    @JsonView(Views.Public.class)
    @Column(name = ("name"), nullable = false)
    private String name;

    @Column(name = "deleted")
    private boolean deleted;

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getDeleted() {
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
        if (holidayDate == null) {
            return false;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Holiday that = (Holiday) other;
        return holidayDate.equals(that.holidayDate);
    }

    @Override
    public int hashCode() {
        return holidayDate != null ? holidayDate.hashCode() : 0;
    }
}
