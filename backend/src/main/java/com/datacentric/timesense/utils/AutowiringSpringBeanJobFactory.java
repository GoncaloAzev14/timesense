package com.datacentric.timesense.utils;

import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;


/*
 * Because Quartz isnt a Spring bean, autowired and other Spring features wont work.
 * Therefore Quartz will instantiate JobQuartzWrapper outside of Spring, causing any
 * Autowired fields or constructors to be null.
 * 
 * This class will ensure the quartz triggering of the job calls JobFactory to create an instance 
 *  of the job, using a custom factory AutowiringSpringBeanJobFactory that uses the 
 *  Spring ApplicationContext injecting the desired beans into the job enabling the use 
 *  of @Autowired to access repositories
 */
@Component
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    private static Logger log = LoggerFactory.getLogger(AutowiringSpringBeanJobFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
        log.info("Autowiring job: {}", job.getClass().getName());
        return job;
    }
    
}
