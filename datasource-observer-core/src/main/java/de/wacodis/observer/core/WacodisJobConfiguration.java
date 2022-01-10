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
package de.wacodis.observer.core;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.AbstractSubsetDefinitionTemporalCoverage;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent;
import de.wacodis.observer.model.SingleJobExecutionEvent;
import de.wacodis.observer.model.WacodisJobDefinition;
import de.wacodis.observer.model.WacodisJobDefinitionExecution;
import de.wacodis.observer.model.WacodisJobDefinitionTemporalCoverage;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent.EventTypeEnum;

public class WacodisJobConfiguration {

    static Logger logger = LoggerFactory.getLogger(WacodisJobConfiguration.class);

    public static JobDataMap configureFirstDataQueryPeriod(WacodisJobDefinition job, JobDataMap data_cloned,
            AbstractSubsetDefinition subsetDefinition) {
        /*
    	 * Analyse subsetDefinition and job information to detect and set the query start and end date for the very first data query
    	 * 
    	 * it depends on several aspects such as job type, execution time or job/input specific settings.
    	 * 
    	 * basic idea: determine execution date of job --> determine "backwards" observer settings according to specific WacodisJob type and settings
    	 * --> then consider observing backwards period from execution date and set it as startDate and endDate for first execution
    	 * 
    	 * later executions will consider endDate from previous query
         */
        logger.info("Begin initial configuration of initial temporalCoverage start and end date for subsetDefinition with id {}", subsetDefinition.getIdentifier());
        logger.info("Job execution is: \n{}", job.getExecution());
        logger.info("Job temporalCoverage is: \n{}", job.getTemporalCoverage());
        logger.info("subsetDefinition temporalCoverage is: \n{}", subsetDefinition.getTemporalCoverage());

        WacodisJobDefinitionExecution execution = job.getExecution();
        AbstractWacodisJobExecutionEvent event = execution.getEvent();
        if (event != null && event.getEventType().equals(EventTypeEnum.SINGLEJOBEXECUTIONEVENT)) {
            // specification of a job, that shall only be executed once
            // e.g. for on demand jobs

            data_cloned = configureFirstDataQueryPeriod_singleExecutionJob(job, data_cloned,
                    subsetDefinition, (SingleJobExecutionEvent) event);
        } else {
            data_cloned = configureFirstDataQueryPeriod_regularExecutionJob(job, data_cloned,
                    subsetDefinition);
        }

        return data_cloned;
    }

    private static JobDataMap configureFirstDataQueryPeriod_regularExecutionJob(WacodisJobDefinition job, JobDataMap data_cloned,
            AbstractSubsetDefinition subsetDefinition) {

        /*
		 * Regular Execution Job
		 * check if the startAt parameter is present, which might cause job to be executed in future
		 * 
		 * if not present then use created setting from job level
         */
        DateTime temporalCoverageEndDate = getTemporalCoverageEndDate_regularJob(job);

        String duration = getAssociatedDuration(job, subsetDefinition);
        String offset = getAssociatedDurationOffset(job, subsetDefinition);

        DateTime temporalCoverageStartDate = computeTemporalCoverageStartDate_regularJobs(temporalCoverageEndDate, duration, offset, job);

        data_cloned = setTemporalCoverage(data_cloned, temporalCoverageStartDate, temporalCoverageEndDate);

        return data_cloned;
    }

    ;

	private static DateTime computeTemporalCoverageStartDate_regularJobs(DateTime temporalCoverageEndDate, String duration,
            String offset, WacodisJobDefinition job) {
        Boolean previousExecution = job.getTemporalCoverage().getPreviousExecution();

        if (previousExecution != null && previousExecution) {

            DateTime startAt = job.getExecution().getStartAt();

            if (startAt == null) {
                startAt = job.getCreated();
            }

            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

            ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(job.getExecution().getPattern()));

            Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(startAt.toGregorianCalendar().toZonedDateTime());
            return new DateTime(lastExecution.get().toInstant().toEpochMilli());
        } else {
            return computeTemporalCoverageStartDate(temporalCoverageEndDate, duration, offset);
        }
    }

    ;

	private static DateTime getTemporalCoverageEndDate_regularJob(WacodisJobDefinition job) {

        /*
		 * consider previous execution 
		 * if present no duration is available
		 * 
		 * derive first execution time from cron pattern
		 * 
		 * (e.g. regular job executes on the first day of each month, created on 25. November --> first execution will be on 01. December)
		 * 
		 * using cron-utils
         */
        DateTime startAt = job.getExecution().getStartAt();

        if (startAt == null) {
            startAt = job.getCreated();
        }

        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

        ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(job.getExecution().getPattern()));
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(startAt.toGregorianCalendar().toZonedDateTime());

        return new DateTime(nextExecution.get().toInstant().toEpochMilli());

    }

    ;

	private static JobDataMap configureFirstDataQueryPeriod_singleExecutionJob(WacodisJobDefinition job, JobDataMap data_cloned,
            AbstractSubsetDefinition subsetDefinition, SingleJobExecutionEvent event) {

        /*
		 * Single Execution Job
		 * Event --> temporalCoverageEndDate  --> always end date to be considered --> duration backwards from that date
         */
        DateTime temporalCoverageEndDate = event.getTemporalCoverageEndDate();
        
        if (temporalCoverageEndDate == null) {
        	temporalCoverageEndDate = job.getCreated(); 
        	
        	if(temporalCoverageEndDate == null) {
        		temporalCoverageEndDate = DateTime.now();
        	}
        }

        String duration = getAssociatedDuration(job, subsetDefinition);
        String offset = getAssociatedDurationOffset(job, subsetDefinition);

        DateTime temporalCoverageStartDate = computeTemporalCoverageStartDate(temporalCoverageEndDate, duration, offset);

        data_cloned = setTemporalCoverage(data_cloned, temporalCoverageStartDate, temporalCoverageEndDate);

        return data_cloned;
    }

    ;

	private static JobDataMap setTemporalCoverage(JobDataMap data_cloned, DateTime temporalCoverageStartDate,
            DateTime temporalCoverageEndDate) {
        logger.debug("Set initial temporalCoverage startDate to {}", temporalCoverageStartDate.toString());
        logger.debug("Set initial temporalCoverage endDate to {}", temporalCoverageEndDate.toString());
        data_cloned.put(TemporalCoverageConstants.START_DATE, temporalCoverageStartDate.toString());
        data_cloned.put(TemporalCoverageConstants.END_DATE, temporalCoverageEndDate.toString());

        return data_cloned;
    }

    ;

	private static DateTime computeTemporalCoverageStartDate(DateTime temporalCoverageEndDate, String duration,
            String offset) {
        DateTime temporalCoverageStartDate;
        Period durationPeriod = Period.parse(duration, ISOPeriodFormat.standard());

        temporalCoverageStartDate = temporalCoverageEndDate.withPeriodAdded(durationPeriod, -1);

        // apply additional offset if present
        if (offset != null) {
            Period offsetPeriod = Period.parse(offset, ISOPeriodFormat.standard());

            temporalCoverageStartDate = temporalCoverageStartDate.withPeriodAdded(offsetPeriod, -1);
        }

        return temporalCoverageStartDate;
    }

    ;

	private static String getAssociatedDurationOffset(WacodisJobDefinition job, AbstractSubsetDefinition subsetDefinition) {
        // use input specific details if available; else use job level details
        String offset = null;
        AbstractSubsetDefinitionTemporalCoverage temporalCoverage_subsetDef = subsetDefinition.getTemporalCoverage();
        WacodisJobDefinitionTemporalCoverage temporalCoverage_job = job.getTemporalCoverage();
        if (temporalCoverage_subsetDef != null) {
            offset = temporalCoverage_subsetDef.getOffset();
        } else {
            offset = temporalCoverage_job.getOffset();
        }

        return offset;
    }

    ;

	private static String getAssociatedDuration(WacodisJobDefinition job, AbstractSubsetDefinition subsetDefinition) {
        // use input specific details if available; else use job level details
        String duration = null;
        AbstractSubsetDefinitionTemporalCoverage temporalCoverage_subsetDef = subsetDefinition.getTemporalCoverage();
        WacodisJobDefinitionTemporalCoverage temporalCoverage_job = job.getTemporalCoverage();
        if (temporalCoverage_subsetDef != null) {
            duration = temporalCoverage_subsetDef.getDuration();
        } else {
            duration = temporalCoverage_job.getDuration();
        }

        return duration;
    }
;

}
