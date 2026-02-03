package com.datacentric.timesense.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.datacentric.timesense.model.AbsenceType;

@Repository
public interface AbsenceTypeRepository extends JpaRepository<AbsenceType, Long>,
        JpaSpecificationExecutor<AbsenceType> {

    Optional<AbsenceType> findByName(String name);
}
