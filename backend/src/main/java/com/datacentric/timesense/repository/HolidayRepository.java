package com.datacentric.timesense.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long>, 
        JpaSpecificationExecutor<Holiday> {
    
    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.holidayDate IN ?1 AND h.deleted = false")
    List<LocalDate> findAllExistingDates(List<LocalDate> dates);

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate = ?1")
    Holiday findByHolidayDate(LocalDate holidayDate);
    
    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.deleted = false")
    List<LocalDate> findAllDates();

    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.holidayDate BETWEEN ?1 AND ?2 ")
    List<LocalDate> findAllHolidaysDatesByDateInterval(LocalDate start, LocalDate end);

    @Modifying
    @Transactional
    @Query("UPDATE Holiday h " + 
            "SET h.deleted = true " + 
            "WHERE h.id = ?1 ")
    void deleteHolidayById(Long id);
}
