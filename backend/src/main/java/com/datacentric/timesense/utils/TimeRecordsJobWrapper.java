package com.datacentric.timesense.utils;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.datacentric.timesense.repository.TimeRecordRepository;

public class TimeRecordsJobWrapper implements Job {

    private static Logger log = LoggerFactory.getLogger(ScheduleRuntimeService.class);

    private TimeRecordRepository timeRecordRepository;

    public TimeRecordsJobWrapper() {
        // Required by Quartz
    }   

    @Autowired
    public TimeRecordsJobWrapper(TimeRecordRepository timeRecordRepository) {
        this.timeRecordRepository = timeRecordRepository;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Quartz Job started. Deleting time records with hour value equal to zero.");

        try {

            timeRecordRepository.deleteByHourValue(0.0);
            
        } catch (Exception e) {
            log.error("Error trying to delete time records!", e);
        }
        
    }
    
}
