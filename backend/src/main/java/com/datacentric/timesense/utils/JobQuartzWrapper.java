package com.datacentric.timesense.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.datacentric.timesense.model.Absence;
import com.datacentric.timesense.model.Status;
import com.datacentric.timesense.repository.AbsenceRepository;
import com.datacentric.timesense.repository.StatusRepository;

public class JobQuartzWrapper implements Job {

    private static Logger log = LoggerFactory.getLogger(ScheduleRuntimeService.class);

    private AbsenceRepository absenceRepository;
    private StatusRepository statusRepository;

    public JobQuartzWrapper() {
        // Required by Quartz
    }   

    @Autowired
    public JobQuartzWrapper(AbsenceRepository absenceRepository, 
            StatusRepository statusRepository) {
        this.absenceRepository = absenceRepository;
        this.statusRepository = statusRepository;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Quartz Job started. Checking if there are absences to mark as done.");

        try {
            LocalDateTime nextWeek = LocalDateTime.now().minusWeeks(1);
            Timestamp timestampToCheck = Timestamp.valueOf(nextWeek);

            List<Absence> passedAbsences = absenceRepository.getPassedAbsences(timestampToCheck);
            Status doneStatus = statusRepository.findByName("DONE");
            List<Absence> doneAbsences = new ArrayList<>();

            for (Absence abs : passedAbsences) {
                log.info("Marking absence {} as done.", abs.getName());
                abs.setStatus(doneStatus);
                doneAbsences.add(abs);  
            }

            absenceRepository.saveAll(doneAbsences);
        } catch (Exception e) {
            log.error("Error trying to mark absence as done!", e);
        }
        
    }
    
}
