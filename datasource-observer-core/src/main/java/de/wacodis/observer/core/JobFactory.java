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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.validator.internal.util.privilegedactions.GetClassLoader;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.WacodisJobDefinition;

/**
 * Factory-Pattern Interface for SubsetDefinitionJobs
 */
public interface JobFactory {

    static final Logger LOG = LoggerFactory.getLogger(JobFactory.class);

    /**
     * Checks if a Job's inputDefinition is supported
     *
     * @param job - Instance of a new Job
     * @return true if job's inputDefinition is supported
     */
    boolean supportsJobDefinition(WacodisJobDefinition job);

    /**
     * Pack business logic data into JobDataMap's data package
     *
     * @param job  - Instance of a new Job
     * @param data - Contains the business logic
     * @deprecated replaced by #initializeJob
     */
    @Deprecated
    default void initializeParameters(WacodisJobDefinition job, JobDataMap data) {
    }

    /**
     * Build new JobDetail
     *
     * @param job  - Instance of a new Job
     * @param data - Contains the business logic
     * @return JobDetail using job's data within JobDataMap
     * @deprecated replaced by #initializeJob
     */
    @Deprecated
    default JobDetail prepareJob(WacodisJobDefinition job, JobDataMap data) {
        return null;
    }

    /**
     * Pack business logic data into JobDataMap's data package and build new JobDetail
     *
     * @param job  - Instance of a new Job
     * @param data - Contains the business logic
     * @return JobDetail using job's data within JobDataMap
     */
    @Deprecated
    default JobDetail initializeJob(WacodisJobDefinition job, JobDataMap data) {
        initializeParameters(job, data);
        return prepareJob(job, data);
    }

    /**
     * Pack business logic data into JobDataMap's data package and build new JobDetail object for each associated
     * input of the WACODIS job
     *
     * @param job  - Instance of a new Job
     * @param data - Contains the business logic
     * @return Collection of JobDetail objects using job's data within JobDataMap
     */
    default Collection<JobDetail> initializeJobs(WacodisJobDefinition job, JobDataMap data) {
        Stream<AbstractSubsetDefinition> wacodisJobInputs = filterJobInputs(job);

        List<AbstractSubsetDefinition> inputList = wacodisJobInputs.collect(Collectors.toList());

        LOG.info("Build and inspect a total number of {} quartz jobs for WACODIS job with id {} and SubsetDefinition of type {}", inputList.size(), job.getId(), inputList.get(0).getClass().getSimpleName());

        Collection<JobDetail> quartzJobs = new ArrayList<>(inputList.size());

        for (AbstractSubsetDefinition subsetDefinition : inputList) {

            JobDataMap data_cloned = (JobDataMap) data.clone();
            String jobId = generateSubsetSpecificIdentifier(subsetDefinition);
            String jobGroupName = generateGroupName(job, subsetDefinition);

            // TODO FIXME take care of areaOfInterest from JobDataMap --> maybe as part of the job key
            BboxHelper bboxHelper = new BboxHelper();
            jobId = bboxHelper.appendAreaOfInterestToQuartzJobKey(job, jobId);

            LOG.debug("Initialize a potential quartz job with jobId '{}' and groupName '{}' for input subsetDefinition \n{}", jobId, jobGroupName, subsetDefinition);
            data_cloned = WacodisJobConfiguration.configureFirstDataQueryPeriod(job, data_cloned, subsetDefinition);
            JobBuilder jobBuilder = initializeJobBuilder(job, data_cloned, subsetDefinition);
            // store durably is required in order to replace an already existing job in scheduler to update its jobDataMap
            JobDetail quartzJob = jobBuilder.withIdentity(jobId, jobGroupName).storeDurably(true).build();
            quartzJobs.add(quartzJob);
        }

        return quartzJobs;
    }

	/**
     * Pack business logic data into JobDataMap's data package and build new JobDetail
     *
     * @param job  - Instance of a new Job
     * @param data - Contains the business logic
     * @return JobBuilder using job's data within JobDataMap
     */
    JobBuilder initializeJobBuilder(WacodisJobDefinition job, JobDataMap data, AbstractSubsetDefinition subsetDefinition);

    /**
     * Method to filter the WACODIS job inputs relevant for the respective JobFactory instance
     *
     * @param job - instance of a WACODIS job
     * @return Stream of {@link AbstractSubsetDefinition}
     */
    Stream<AbstractSubsetDefinition> filterJobInputs(WacodisJobDefinition job);

    /**
     * Generates an identifier that encodes a set of important SubsetDefinition specific propeties.
     * It may be used as quartz job identifier in order to ensure that only one quartz job per unique data source type exists
     *
     * @param subsetDefinition - the concrete subsetDefinition, for which the identifier shall be generated
     * @return identifier encoding SubsetDefinition specific properties
     */
    String generateSubsetSpecificIdentifier(AbstractSubsetDefinition subsetDefinition);

    /**
     * Generates an group name for quartz job definition
     *
     * @param job              job - instance of a WACODIS job
     * @param subsetDefinition - the concrete subsetDefinition, for which the identifier shall be generated
     * @return quartz job group name
     */
    default String generateGroupName(WacodisJobDefinition job, AbstractSubsetDefinition subsetDefinition) {

        // we only return a constant value here as we want to construct quartz job keys that are build only from subsetDefinition specific properties

        // that is necessary in order to compare different jobKeys fr different WACODIS jobs with same subsetDefinitions

        // the overall goal is to only have one quartz job per unique subsetDefinition

        // a WACODIS job specific group name (e.g. job.getName()) would destroy the possibility to only have one quartz job per unique subsetDefinition
        return "GROUP";
    }

    /**
     * Information whether the JobFactory has generated certain jobDetail
     * @param jobDetail
     * @return true, if jobDetail was generated by jobFactory
     */
	default boolean supportsJobDetail(JobDetail jobDetail) {
		Class<? extends Job> jobClass = jobDetail.getJobClass();
		
		Class factoryJobClass = getQuartzJobClass();
		
		if (jobClass.toString().equalsIgnoreCase(factoryJobClass.toString())) {
			return true;
		}
		return false;
	}

	Class getQuartzJobClass();

	/**
	 * Modifies job specific BBOX parameter values within JobDataMap
	 * @param jobDetail the Quartz jobDetail
	 * @param expandedBbox the updated BBOX as String "minLon,minLat,maxLon,maxLat"
	 * @return the modified jobDetail
	 */
	JobDetail modifyBboxParameter(JobDetail jobDetail, String expandedBbox);
}
