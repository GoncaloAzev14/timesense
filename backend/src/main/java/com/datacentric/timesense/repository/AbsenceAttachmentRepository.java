package com.datacentric.timesense.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datacentric.timesense.model.AbsenceAttachment;

@Repository
public interface AbsenceAttachmentRepository extends JpaRepository<AbsenceAttachment, Long>,
        JpaSpecificationExecutor<AbsenceAttachment> {
    boolean existsByAbsenceId(Long absenceId);
    
    @Query("SELECT a FROM AbsenceAttachment a WHERE a.absence.id = ?1 ")
    List<AbsenceAttachment> findByAbsenceId(@Param("absenceId") Long absenceId);

    @Query("SELECT a FROM AbsenceAttachment a WHERE a.absence.id = ?1 AND a.id = ?2 ")
    Optional<AbsenceAttachment> findByAbsenceIdAndAttachmentId(Long absenceId, Long id);
}
