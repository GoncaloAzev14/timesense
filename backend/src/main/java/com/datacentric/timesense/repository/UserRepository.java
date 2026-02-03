package com.datacentric.timesense.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datacentric.timesense.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM User a WHERE a.jobTitle.id = ?1")
    boolean existsInUserByJobTitleId(Long jobTitleId);

    @Query("SELECT u.id FROM User u WHERE u.lineManagerId = ?1 AND u.deleted = false")
    List<Long> findManagerTeam(Long managerId);

    @Query("SELECT u FROM User u WHERE u.name = ?1 ")
    Optional<User> findByName(String name);

    // TODO: Add flag to JobTitle instead of using job title names
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE users u
            SET 
                prev_year_vacation_days = u.current_year_vacation_days,
                current_year_vacation_days = ?1
            FROM job_titles jt
            WHERE u.job_title = jt.id
                AND u.deleted = false
                AND jt.name NOT IN ('Internship', 'Outsourcer')
                AND (
                    u.exit_date IS NULL           
                    OR u.exit_date >= CURRENT_DATE 
                )
        """,
        nativeQuery = true)
    void newBusinessYearVacations(Double vacationDays);

}
