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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import de.wacodis.observer.core.BboxHelper;
import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.publisher.PublisherChannel;

@Component
public class QuartzServer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(QuartzServer.class);

    public static final String PUBLISHER = "PUBLISHER";
    public static final String WACODIS_JOB_ID_STORAGE = "wacodisJobIds";


    private Scheduler scheduler;

    @Autowired
    private PublisherChannel publisher;
    
    @Autowired
    private BboxHelper bboxHelper;

    @Autowired
    private SchedulerFactoryBean schedulerBean;

    @Override
    public void afterPropertiesSet() throws SchedulerException {
        scheduler = schedulerBean.getScheduler();
        scheduler.getContext().put(PUBLISHER, publisher);

        log.info("QuartzServer initialized");
    }

    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        return this.scheduler.scheduleJob(jobDetail, trigger);    //runs QuartzJob's execute()
    }

    public void removeJobByKey(JobKey key) throws SchedulerException {
        if (this.scheduler.checkExists(key)) {
            log.debug("Deleting quartz job for key '{}'", key);
            this.scheduler.deleteJob(key);
        }

    }

    public void removeJobsByKeys(List<JobKey> jobKeys) throws SchedulerException {
        for (JobKey jobKey : jobKeys) {
            this.removeJobByKey(jobKey);
        }

    }

    public boolean jobForSameDatasourceAndTypeAndBBOXAlreadyExists(JobDetail jobDetail) throws SchedulerException {
        // this should work, as the jobKey is generated from WACODIS SubsetDefinition
        // specific properties

        // hence the same key is guaranteed to be generated for equal SubsetDefinitions
        return this.scheduler.checkExists(jobDetail.getKey());
    }
    
    public boolean jobForSameDatasourceAndTypeWithIntersectingBBOXAlreadyExists(JobDetail jobDetailCandidate) throws SchedulerException {
		String keyPart_withoutBBOX = bboxHelper.getQuartzKeyPartWithoutBbox(jobDetailCandidate);
		
		List<JobKey> possibleQuartzJobKeys = findQuartzJobsWithSameJobKeyPart(keyPart_withoutBBOX);
		
		// analyze possible quartz job keys to find one whose BBOX part intersects the BBOX part of the new Wacodis Job
		JobKey jobKey = bboxHelper.findQuartzJobKeyIntersectingNewWacodisJobBBOX(possibleQuartzJobKeys, jobDetailCandidate);
		return jobKey != null;
    }

	private List<JobKey> findQuartzJobsWithSameJobKeyPart(String keyPart_withoutBBOX) throws SchedulerException {
		List<JobKey> possibleQuartzJobKeys = new ArrayList<JobKey>();
		
		for (String groupName : this.scheduler.getJobGroupNames()) {

		     for (JobKey jobKey : this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
		                
		      String jobName = jobKey.getName();
		      String jobGroup = jobKey.getGroup();
		                
		      if (jobName.contains(keyPart_withoutBBOX)) {
		    	  // found a Quartz jobDetail that observes the same Wacodis input as a new Wacodis job
		    	  possibleQuartzJobKeys.add(jobKey);
		      }

		    }		
		}
		return possibleQuartzJobKeys;
	}
	
	public JobDetail getQuartzJobForWacodisInputDefinition_withIntersectingBBOXString(JobDetail jobDetailCandidate) throws SchedulerException {
		String keyPart_withoutBBOX = bboxHelper.getQuartzKeyPartWithoutBbox(jobDetailCandidate);
		
		List<JobKey> possibleQuartzJobKeys = findQuartzJobsWithSameJobKeyPart(keyPart_withoutBBOX);
		
		// analyze possible quartz job keys to find one whose BBOX part intersects the BBOX part of the new Wacodis Job
		JobKey jobKey = bboxHelper.findQuartzJobKeyIntersectingNewWacodisJobBBOX(possibleQuartzJobKeys, jobDetailCandidate);
		return this.scheduler.getJobDetail(jobKey);	
	}

    public JobDetail getQuartzJobForWacodisInputDefinition_includingIdenticalBBOXString(JobDetail jobDetail) throws SchedulerException {
        JobKey key = jobDetail.getKey();

        return this.scheduler.getJobDetail(key);
    }

    public void addWacodisJobIdAndBBOXToQuartzJobDataMap(JobDetail quartzJob, UUID wacodisJobId, AbstractDataEnvelopeAreaOfInterest areaOfInterest, boolean replaceExistingQuartzJob) throws SchedulerException {
        addWacodisJobIdToJobDataMap(quartzJob, wacodisJobId);
        
        bboxHelper.addWacodisJobIdAndBBOXToJobDataMap(quartzJob, wacodisJobId, areaOfInterest);

        // now we must replace the existing job innscheduler in order to apply the updated jobDataMap !
        // otherwise the initial jobDataMa will still be used
        if (replaceExistingQuartzJob) {
            this.scheduler.addJob(quartzJob, true);
        }
    }

	private void addWacodisJobIdToJobDataMap(JobDetail quartzJob, UUID wacodisJobId) {
		log.info("Associated WACODIS job management: add WACODIS job ID '{}' to the associated jobs of quartz job with key '{}' .", wacodisJobId, quartzJob.getKey());

		JobDataMap jobDataMap = quartzJob.getJobDataMap();
        // implement a storage for associated WACODIS job ids than require the quartz job

        // implement as HashSet in order to ensure that WACODIS jobIds cannot be inserted twice
        HashSet<UUID> wacodisJobIds = null;

        if (jobDataMap.containsKey(WACODIS_JOB_ID_STORAGE)) {
            Object object = jobDataMap.get(WACODIS_JOB_ID_STORAGE);
            if (object instanceof HashSet<?>) {
                wacodisJobIds = (HashSet<UUID>) object;

                wacodisJobIds.add(wacodisJobId);
            }
        } else {
            wacodisJobIds = new HashSet<UUID>();
            wacodisJobIds.add(wacodisJobId);
        }

        log.info("Associated WACODIS job management: the quartz job with key '{}' now has a total number of '{}' associated WACODIS jobs. The WACODIS job IDs are: '{}'", quartzJob.getKey(), wacodisJobIds.size(), wacodisJobIds);


        // modify element in quartz job data map
        jobDataMap.put(WACODIS_JOB_ID_STORAGE, wacodisJobIds);
	}

    public Trigger getAssociatedTrigger(JobDetail existingQuartzJob) throws SchedulerException {

        /*
         * Currently only one trigger shall exist for the same job
         *
         * so we can simply return the first value
         */
        List<? extends Trigger> triggersOfJob = this.scheduler.getTriggersOfJob(existingQuartzJob.getKey());

        return triggersOfJob.get(0);
    }

    public JobDetail removeWacodisJobIdAndBboxFromQuartzJobDataMap(JobDetail quartzJob, UUID wacodisJobId, TriggerKey triggerKey, boolean completelyRemoveQuartzJob) throws SchedulerException {
        HashSet<UUID> wacodisJobIds = removeWacodisJobIdFromJobDataMap(quartzJob, wacodisJobId);
        
        quartzJob = scheduler.getJobDetail(quartzJob.getKey());

        // now check if there is any remaining WACODIS job ID in JobDataMap
        // if not, we must remove/pause the quartz job
        if (wacodisJobIds.size() == 0) {
            // depending on boolean parameter remove whole job or just unschedule it
            if (completelyRemoveQuartzJob) {
                this.scheduler.deleteJob(quartzJob.getKey());
                log.info("Associated WACODIS job management: the quartz job with key '{}' was deleted.", quartzJob.getKey());

            } else {
                this.scheduler.unscheduleJob(triggerKey);
                log.info("Associated WACODIS job management: the quartz job with key '{}' was unscheduled. Hence the trigger with key '{}' was removed.", quartzJob.getKey(), triggerKey);
            }
            return null;
        }
        else {
        	// if there is at least one remaining entry, we must reduce the BBOX observation area
        	JobDetail newQuartzJob = removeWacodisJobBboxFromJobDataMap(quartzJob, wacodisJobId, wacodisJobIds);
        	return newQuartzJob;
        }

    }

	private JobDetail removeWacodisJobBboxFromJobDataMap(JobDetail quartzJob, UUID wacodisJobId,
			HashSet<UUID> remainingWacodisJobIds) throws SchedulerException {
		String bboxOfDeletedWacodisJob = bboxHelper.removeWacodisJobIdAndBBOXFromJobDataMap(quartzJob, wacodisJobId);
		
		// now we must replace the existing job innscheduler in order to apply the updated jobDataMap !
        // otherwise the initial jobDataMa will still be used
        this.scheduler.addJob(quartzJob, true);
        quartzJob = this.scheduler.getJobDetail(quartzJob.getKey());
        
		JobDetail newQuartzJob = bboxHelper.regenerateBboxForQuartzJob(quartzJob, wacodisJobId, remainingWacodisJobIds, JobScheduler.AOI_KEY);
		
		return newQuartzJob;		
	}

	private HashSet<UUID> removeWacodisJobIdFromJobDataMap(JobDetail quartzJob, UUID wacodisJobId) throws SchedulerException {
		log.info("Associated WACODIS job management: try to remove WACODIS job ID '{}' from the associated jobs of quartz job with key '{}' .", wacodisJobId, quartzJob.getKey());

		JobDataMap jobDataMap = quartzJob.getJobDataMap();
        // implement a storage for associated WACODIS job ids than require the quartz job

        // implement as HashSet in order to ensure that WACODIS jobIds cannot be inserted twice
        HashSet<UUID> wacodisJobIds = null;

        if (jobDataMap.containsKey(WACODIS_JOB_ID_STORAGE)) {
            Object object = jobDataMap.get(WACODIS_JOB_ID_STORAGE);
            if (object instanceof HashSet<?>) {
                wacodisJobIds = (HashSet<UUID>) object;

                if (wacodisJobIds.contains(wacodisJobId)) {
                    log.info("Associated WACODIS job management: Removal of WACODIS job ID '{}' from the associated jobs of quartz job with key '{}' was successful.", wacodisJobId, quartzJob.getKey());

                    wacodisJobIds.remove(wacodisJobId);
                } else {
                    log.info("Associated WACODIS job management: Removal of WACODIS job ID '{}' from the associated jobs of quartz job with key '{}' failed, as there is no such WACODIS job id in quartz jobDataMap '{}' .", wacodisJobId, quartzJob.getKey(), wacodisJobIds);
                }
            }
        } else {
            log.info("Associated WACODIS job management: Removal of WACODIS job ID '{}' from the associated jobs of quartz job with key '{}' failed, as there is no WACODIS job id storage in quartz jobDataMap. Will now create a new empty WACODIS job ID storage.", wacodisJobId, quartzJob.getKey());
            wacodisJobIds = new HashSet<>();
        }

        log.info("Associated WACODIS job management: the quartz job with key '{}' now has a total number of '{}' associated WACODIS jobs. The job ids are: {}", quartzJob.getKey(), wacodisJobIds.size(), wacodisJobIds);

        // modify element in quartz job data map
        jobDataMap.put(WACODIS_JOB_ID_STORAGE, wacodisJobIds);
        // now we must replace the existing job innscheduler in order to apply the updated jobDataMap !
        // otherwise the initial jobDataMa will still be used
        this.scheduler.addJob(quartzJob, true);
		return wacodisJobIds;
	}

	public void replaceExistingJob_byKey(JobKey quartzKey_old, JobDetail quartzJob, Trigger trigger) throws SchedulerException {
		this.scheduler.deleteJob(quartzKey_old);
		this.scheduler.scheduleJob(quartzJob, trigger);
	}

}
