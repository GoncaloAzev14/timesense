package com.datacentric.timesense.utils;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class ScheduleRuntimeService implements AutoCloseable {

    private static Logger log = LoggerFactory.getLogger(ScheduleRuntimeService.class);
    private static final int ELEVEN = 23;
    private static final int ZERO = 0;
    private static final String TZ = "Europe/Lisbon";

    private Scheduler scheduler;

    @Autowired
    private AutowiringSpringBeanJobFactory jobFactory;  

    @PostConstruct
    public void start() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            scheduler.setJobFactory(jobFactory);
            
            log.info("Starting the scheduler");
            log.info("System time at startup: {}", LocalDateTime.now());
            scheduler.start();

            scheduleAbsenceCheckerJob();
            scheduleTimeRecordsJob();
            
        } catch (SchedulerException e) {
            log.error("Scheduler failed to initialize.", e);
        }
    }

    // Quartz job set to run once every day that marks as DONE the absences 
    // whose endDate has already passed a week ago
    public void scheduleAbsenceCheckerJob() throws SchedulerException {
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Done Absences Trigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(ELEVEN, ZERO)
                .inTimeZone(TimeZone.getTimeZone(TZ)))
                .build();
        JobDetail quartzJob = JobBuilder.newJob()
                .withIdentity("Done Absences Checker Job").ofType(JobQuartzWrapper.class)
                .build();
        
        scheduler.scheduleJob(quartzJob, trigger);
        log.info("Quartz job scheduled to run daily at {}:{}", ELEVEN, ZERO);
    }

    public void scheduleTimeRecordsJob() throws SchedulerException {
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("Time Records Trigger")
                // .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * ? * *") // every hour at0
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(ELEVEN, ZERO)
                        .inTimeZone(TimeZone.getTimeZone(TZ)))
                .build();

        JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity("Remove Time Records")
                .ofType(TimeRecordsJobWrapper.class)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Quartz job scheduled to run at {}:{}", ELEVEN, ZERO);
    }

    @Override
    public void close() throws Exception {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            log.error("Failed to stop the scheduler.", e);
        }
    }
}
