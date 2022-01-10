/*
 * Copyright 2018-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wacodis.observer.quartz;

import java.util.Collection;
import java.util.UUID;

import org.joda.time.DateTime;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.wacodis.observer.core.BboxHelper;
import de.wacodis.observer.core.JobFactory;
import de.wacodis.observer.core.TemporalCoverageConstants;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent.EventTypeEnum;
import de.wacodis.observer.model.WacodisJobDefinition;
import de.wacodis.observer.model.WacodisJobDefinitionExecution;

@Component
public class JobScheduler {

    private static final String SINGLE_TIME_EXECUTION_SUFFIX = "_ONCE";
    protected static final String AOI_KEY = "areaOfInterest";
    private static final String EXEC_INTERVAL_KEY = "executionInterval";
    private static final int DEFAULT_EXEC_INTERVAL = 3600; // in seconds


    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);


    @Autowired
    private QuartzServer wacodisQuartz;
    
    @Autowired
    private BboxHelper bboxHelper;

    public JobScheduler() {
    }

    public void scheduleJob(WacodisJobDefinition job, JobFactory factory) {
        try {

            // first validate temporalCoverage
            WacodisJobValidator.validateJobParameters(job);

            // generate all Quartz job definitions for each WACODIS job input (SubsetDefinition)

            Collection<JobDetail> quartzJobDefinitions = generateQuartzJobDefinitions(job, factory);
            
            /*
             * execute each job once in order to ensure that queried data is available.
             */

            executeQuartzJobsOnce(quartzJobDefinitions, job);

            // then for each Quartz job definition
            /*
             * manage Quartz job instances;
             *
             * if is a single time execution, then skip
             *
             * else
             * 		check if existing jobs already watch the same set of queried data
             * 		if so just update JobDataMap with WACODIS ID
             * 		else generate and execute new job & trigger for scheduling
             */
            WacodisJobDefinitionExecution execution = job.getExecution();
            AbstractWacodisJobExecutionEvent event = execution.getEvent();
            if (event != null && event.getEventType().equals(EventTypeEnum.SINGLEJOBEXECUTIONEVENT)) {
                // specification of a job, that shall only be executed once
                // e.g. for on demand jobs

                // in this case, the job was already executed and thus, we may skip the rest

                //TODO what if single time job shall start in the future?
            } else {
                manageQuartzJobDefinitions_onAddNewWacodisJob(job, quartzJobDefinitions);
            }
        } catch (Exception e) {
            LOG.warn(e.getClass() + ": " + e.getMessage());
            LOG.debug(e.getClass() + ": " + e.getMessage(), e);
            e.printStackTrace();
        }
    }


    public void onDeleteWacodisJob(WacodisJobDefinition wacodisJob, JobFactory factory) {
        /*
         * when a WACODIS job is deleted we must check if there are running quartz jobs
         * for each input of the WACODIS job
         * --> within those eisting quartz jobs, we must remove the WACODIS job ID from quartz JobDataMap
         *
         * if there is no remaining WACODIS job ID in quartz JobDataMap --> then pause/unschedule job (remove trigger!)!
         */

        // generate all Quartz job definitions for each WACODIS job input (SubsetDefinition)
        // they will be used to inspect existing quartz jobs

        try {
            Collection<JobDetail> quartzJobDefinitions = generateQuartzJobDefinitions(wacodisJob, factory);
            manageQuartzJobDefinitions_onDeleteWacodisJob(wacodisJob, quartzJobDefinitions);
        } catch (SchedulerException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }
    }

    private void manageQuartzJobDefinitions_onDeleteWacodisJob(WacodisJobDefinition wacodisJob,
                                                               Collection<JobDetail> quartzJobDefinitions) throws SchedulerException {

        for (JobDetail jobDetail : quartzJobDefinitions) {             
            if (wacodisQuartz.jobForSameDatasourceAndTypeWithIntersectingBBOXAlreadyExists(jobDetail)) {
            	LOG.info("Existing quartz job with the same input definition parameters and an intersecting BBOX was identified. Will modify the existing Quartz job, removing the Wacodis jobId and its BBOX.");
            	
            	JobDetail existingQuartzJob = wacodisQuartz.getQuartzJobForWacodisInputDefinition_withIntersectingBBOXString(jobDetail);
            	
                // get Trigger, which has associated Key in case we only want to unschedule job
                Trigger trigger = prepareTrigger(jobDetail, wacodisJob);
                JobDetail newQuartzJob = wacodisQuartz.removeWacodisJobIdAndBboxFromQuartzJobDataMap(existingQuartzJob, wacodisJob.getId(), trigger.getKey(), true);
            	
                /*
                 * now we must replace the old quartz job and reschedule the new quartzJob
                 */                
                if(newQuartzJob != null) {
                	JobKey quartzKey_old = existingQuartzJob.getKey();                
    				Object bbox_old = existingQuartzJob.getJobDataMap().get(AOI_KEY);
                    Object bbox_expanded = newQuartzJob.getJobDataMap().get(AOI_KEY);
                    Trigger trigger_new = prepareTrigger(newQuartzJob, wacodisJob);
                    wacodisQuartz.replaceExistingJob_byKey(quartzKey_old, newQuartzJob, trigger_new);                
                    LOG.info("Replaced quartz job with ID {} by similar quartz job with reduced BBOX and new ID {}. Previous BBOX {} was reduced to {}", quartzKey_old, newQuartzJob.getKey(), bbox_old, bbox_expanded);
                }                
            }
            else {
                LOG.warn("No existing quartz jobs found for deletion parameters identifier '{}'.", jobDetail.getKey());
            }
        }

    }

    private void manageQuartzJobDefinitions_onAddNewWacodisJob(WacodisJobDefinition job, Collection<JobDetail> quartzJobDefinitions) throws Exception {

        /*
         * check if existing jobs already watch the same set of queried data
         * 		if so just update JobDataMap with WACODIS ID
         * 		else generate and execute new job & trigger for scheduling
         */
        for (JobDetail jobDetail : quartzJobDefinitions) {
            if (wacodisQuartz.jobForSameDatasourceAndTypeAndBBOXAlreadyExists(jobDetail)) {
                LOG.info("Existing quartz job with the same parameters identified. Will add WACODIS job ID to its associated WACODIS jobs");
                JobDetail existingQuartzJob = wacodisQuartz.getQuartzJobForWacodisInputDefinition_includingIdenticalBBOXString(jobDetail);
                wacodisQuartz.addWacodisJobIdAndBBOXToQuartzJobDataMap(existingQuartzJob, job.getId(), job.getAreaOfInterest(), true);

                // if the execution-interval, specified in WACODIS job definition, is shorter than the already existing trigger
                // then we might want to adjust the trigger --> shorten execution interval
                adjustTriggerIfIntervalIsShorter(existingQuartzJob, job.getExecution().getPattern());
            }
            else if (wacodisQuartz.jobForSameDatasourceAndTypeWithIntersectingBBOXAlreadyExists(jobDetail)) {
            	LOG.info("Existing quartz job with the same input definition parameters and an intersecting BBOX was identified. Will modify the existing Quartz job expanding the BBOX to include the new BBOX of the new WACODIS job. Also will add WACODIS job ID to the associated WACODIS jobs of the modified Quartz job.");
            	
            	JobDetail existingQuartzJob = wacodisQuartz.getQuartzJobForWacodisInputDefinition_withIntersectingBBOXString(jobDetail);
                JobKey quartzKey_old = existingQuartzJob.getKey();                
				Object bbox_old = existingQuartzJob.getJobDataMap().get(AOI_KEY);
                existingQuartzJob = bboxHelper.expandBboxOfExistingQuartzJob(existingQuartzJob, jobDetail, AOI_KEY);
                Object bbox_expanded = existingQuartzJob.getJobDataMap().get(AOI_KEY);
                Trigger trigger = prepareTrigger(jobDetail, job);
                wacodisQuartz.replaceExistingJob_byKey(quartzKey_old, existingQuartzJob, trigger);                
                LOG.info("Replaced quartz job with ID {} by similar quartz job with expanded BBOX and new ID {}. Previous BBOX {} was expanded to {}", quartzKey_old, existingQuartzJob.getKey(), bbox_old, bbox_expanded);
            	wacodisQuartz.addWacodisJobIdAndBBOXToQuartzJobDataMap(existingQuartzJob, job.getId(), job.getAreaOfInterest(), true);

                // if the execution-interval, specified in WACODIS job definition, is shorter than the already existing trigger
                // then we might want to adjust the trigger --> shorten execution interval
                adjustTriggerIfIntervalIsShorter(existingQuartzJob, job.getExecution().getPattern());
            }
            else {
                // initialize wacodisJobIdStorage in jobDataMap
                LOG.info("No existing quartz job with the same parameters was found. Will create and schedule a new quartz job and add WACODIS job ID to its associated WACODIS jobs");
                wacodisQuartz.addWacodisJobIdAndBBOXToQuartzJobDataMap(jobDetail, job.getId(), job.getAreaOfInterest(), false);

                Trigger trigger = prepareTrigger(jobDetail, job);
                wacodisQuartz.scheduleJob(jobDetail, trigger);
            }
        }

    }

    private void adjustTriggerIfIntervalIsShorter(JobDetail existingQuartzJob, String pattern) {
        // TODO FIXME implement
        LOG.info("Adjustment of Triggers is currently not implemented.");

        /*
         * it might happen, that an existing job was just paused!
         *
         * TODO detect that and if it is state=paused/unscheduled, then we might just define a new trigger
         */

//		CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(pattern);
//		
//		
//		Trigger associatedTrigger = wacodisQuartz.getAssociatedTrigger(existingQuartzJob);

    }

    private void executeQuartzJobsOnce(Collection<JobDetail> quartzJobDefinitions, WacodisJobDefinition wacodisJob) throws SchedulerException {
        for (JobDetail jobDetail : quartzJobDefinitions) {
            // create a jonCopy in order to adjust the quartz job KEY definition for single time execution
            JobKey key = jobDetail.getKey();
            JobKey key_singleTime = generateUniqueSingleTimeExecutionJobKey(key);
            JobDetail jobDetail_singleTimeCopy = (JobDetail) jobDetail.clone();

            jobDetail_singleTimeCopy = jobDetail_singleTimeCopy.getJobBuilder().withIdentity(key_singleTime).build();

            Trigger oneTimeTrigger = prepareSingleExecutionTrigger(jobDetail, wacodisJob);

            LOG.info("Scheduling new one time execution job with jobID {} and groupName {}", key_singleTime.getName(), key_singleTime.getGroup());
            wacodisQuartz.scheduleJob(jobDetail_singleTimeCopy, oneTimeTrigger);
        }

    }

    private JobKey generateUniqueSingleTimeExecutionJobKey(JobKey key) {

        // make a single time job key unique by adding a unique random UUID

        /*
         * this can be necessary as a single time job might be startet immediately or
         * in future (i.e. 1 year in the future). Hence the job must exist that long in order to be
         * triggered!
         *
         * --> hence it must have a unique jobKey
         */
        UUID uuid = UUID.randomUUID();

        return new JobKey(key.getName() + "_" + uuid + SINGLE_TIME_EXECUTION_SUFFIX,
                key.getGroup() + "_" + uuid + SINGLE_TIME_EXECUTION_SUFFIX);
    }

    private Trigger prepareSingleExecutionTrigger(JobDetail jobDetail, WacodisJobDefinition wacodisJob) {

    	TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobDetail.getKey().getName() + SINGLE_TIME_EXECUTION_SUFFIX, jobDetail.getKey().getGroup() + SINGLE_TIME_EXECUTION_SUFFIX + "_" + UUID.randomUUID());
		
    	Trigger trigger = configureWacodisTrigger(jobDetail, wacodisJob, triggerBuilder);
        
		return trigger;
    }

    private Collection<JobDetail> generateQuartzJobDefinitions(WacodisJobDefinition job, JobFactory factory) {
        JobDataMap data = new JobDataMap();
        data.put(AOI_KEY, job.getAreaOfInterest());

        return factory.initializeJobs(job, data);
    }

    private Trigger prepareTrigger(JobDetail jobDetail, WacodisJobDefinition wacodisJob) {

        JobDataMap data = jobDetail.getJobDataMap();
        /*
         * set a default execution interval
         */
        if (!data.containsKey(EXEC_INTERVAL_KEY)) {
            data.put(EXEC_INTERVAL_KEY, DEFAULT_EXEC_INTERVAL);
        }

        LOG.info("Build new Trigger with execution interval: {} seconds", data.get(EXEC_INTERVAL_KEY));

        // use the jobDetails key information for the trigger to "group" them
        
        TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger()
        .withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
        .withSchedule(SimpleScheduleBuilder
                .simpleSchedule().repeatForever()
                .withIntervalInSeconds(data.getInt(EXEC_INTERVAL_KEY)));
        
    	Trigger trigger = configureWacodisTrigger(jobDetail, wacodisJob, triggerBuilder);
        
		return trigger;
    }

	private Trigger configureWacodisTrigger(JobDetail jobDetail, WacodisJobDefinition wacodisJob, TriggerBuilder triggerBuilder) {
		Trigger trigger = null;
		DateTime startDateTime = null;
		
		WacodisJobDefinitionExecution execution = wacodisJob.getExecution();
        AbstractWacodisJobExecutionEvent event = execution.getEvent();
        LOG.info("Start creation of job trigger");
        if (event != null && event.getEventType().equals(EventTypeEnum.SINGLEJOBEXECUTIONEVENT)) {
            // specification of a job, that shall only be executed once
            // e.g. for on demand jobs
        	
        	// only inspect optional startAt parameter
        	DateTime startAt = execution.getStartAt();
			
			// if startAt exists and points to future dateTime
			if(startAt != null && startAt.isAfterNow()) {
				LOG.info("SingleExecution job with startAt parameter detected: Hence use startAt dateTime {} to instantiate trigger.", startAt.toString());
				startDateTime = startAt;
			}
        } else {
        	// regular pattern based job
        	// inspect optional startAt and mandatory pattern properties
        	// to determine the next/first planned execution

        	// the next/first execution dateTime according to pattern and startDate is already computed for pattern based regular job within jobData map
        	LOG.info("Regular pattern based execution job detected: Hence analyse optional startAt '{}' and pattern '{}' to instantiate trigger.", execution.getStartAt(), execution.getPattern());			
        	startDateTime = DateTime.parse((String)jobDetail.getJobDataMap().get(TemporalCoverageConstants.END_DATE));
        }
		        
		if(startDateTime != null) {
			LOG.info("Finish quartz job trigger creation with startAt dateTime of: {}", startDateTime.toString());
			trigger = triggerBuilder
	                .startAt(startDateTime.toDate())
	                .build();
		}
			
		// fallback strategy, start trigger/job immediately
		if(trigger == null) {
			LOG.info("Finish quartz job trigger creation with immediate execution time.");
			trigger = triggerBuilder
	                .startNow()
	                .build();
		}
		return trigger;
	}
}
