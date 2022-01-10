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
package de.wacodis.observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.springframework.test.context.junit4.SpringRunner;

import de.wacodis.observer.core.BboxHelper;
import de.wacodis.observer.core.JobFactory;
import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.AbstractSubsetDefinition.SourceTypeEnum;
import de.wacodis.observer.model.AbstractSubsetDefinitionTemporalCoverage;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent.EventTypeEnum;
import de.wacodis.observer.model.CopernicusSubsetDefinition;
import de.wacodis.observer.model.SingleJobExecutionEvent;
import de.wacodis.observer.model.WacodisJobDefinition;
import de.wacodis.observer.model.WacodisJobDefinitionExecution;
import de.wacodis.observer.model.WacodisJobDefinitionTemporalCoverage;

@RunWith(SpringRunner.class)
//@SpringBootTest(classes = WacodisBboxHelperIT.class)
//@ExtendWith(SpringExtension.class)
public class WacodisBboxHelperIT {

	private static final String AOI_KEY = "areaOfInterest";
	WacodisJobDefinition wacodisJob1;
	WacodisJobDefinition wacodisJob2_intersecting;
	WacodisJobDefinition wacodisJob3_disjoint;

	AbstractDataEnvelopeAreaOfInterest areaOfInterest1;
	AbstractDataEnvelopeAreaOfInterest areaOfInterest2;
	AbstractDataEnvelopeAreaOfInterest areaOfInterest3;
	AbstractDataEnvelopeAreaOfInterest areaOfInterest_merged;

	BboxHelper bboxHelper = new BboxHelper();

	class JobFactoryImpl implements JobFactory {

		@Override
		public boolean supportsJobDefinition(WacodisJobDefinition job) {
			long count = job.getInputs().stream().filter(i -> i instanceof CopernicusSubsetDefinition).count();
			return count > 0;
		}

		@Override
		public JobBuilder initializeJobBuilder(WacodisJobDefinition job, JobDataMap data,
				AbstractSubsetDefinition subsetDefinition) {
			// this should always be the case
			if (subsetDefinition instanceof CopernicusSubsetDefinition) {

				data.put("executionInterval", 3600);

			}

			// create the quartz object
			return JobBuilder.newJob(Job.class).usingJobData(data);

		}

		@Override
		public Stream<AbstractSubsetDefinition> filterJobInputs(WacodisJobDefinition job) {
			return job.getInputs().stream().filter((i -> i instanceof CopernicusSubsetDefinition));
		}

		@Override
		public String generateSubsetSpecificIdentifier(AbstractSubsetDefinition subsetDefinition) {
			StringBuilder builder = new StringBuilder("");

			if (subsetDefinition instanceof CopernicusSubsetDefinition) {
				CopernicusSubsetDefinition copDef = (CopernicusSubsetDefinition) subsetDefinition;
				builder.append(copDef.getSourceType());

				if (copDef.getSatellite() != null) {
					builder.append("_" + copDef.getSatellite());
				}
			}

			return builder.toString();
		}

		@Override
		public Class getQuartzJobClass() {
			return Job.class;
		}

		@Override
		public JobDetail modifyBboxParameter(JobDetail jobDetail, String expandedBbox) {
			// here we must do nothing as this Job dies not specify BBOX parameter
			return jobDetail;
		}

	};

	@Before
	public void prepareWacodisJobs() {
		bboxHelper = new BboxHelper(Arrays.asList(new JobFactoryImpl()));
		prepareAOIs();
		prepareJob1();
		prepareJob2();
		prepareJob3();
	}

	private void prepareAOIs() {
		areaOfInterest1 = new AbstractDataEnvelopeAreaOfInterest();
		Float[] extent = new Float[] { 7.0f, 50.0f, 8.0f, 51.0f };
		areaOfInterest1.setExtent(Arrays.asList(extent));

		areaOfInterest2 = new AbstractDataEnvelopeAreaOfInterest();
		Float[] extent2 = new Float[] { 7.5f, 50.5f, 8.5f, 51.5f };
		areaOfInterest2.setExtent(Arrays.asList(extent2));

		areaOfInterest3 = new AbstractDataEnvelopeAreaOfInterest();
		Float[] extent3 = new Float[] { 9.0f, 52.0f, 10.0f, 53.0f };
		areaOfInterest3.setExtent(Arrays.asList(extent3));
		
		areaOfInterest_merged = new AbstractDataEnvelopeAreaOfInterest();
		Float[] extent_merged = new Float[] { 7.0f, 50.0f, 8.5f, 51.5f };
		areaOfInterest_merged.setExtent(Arrays.asList(extent_merged));
	}

	private void prepareJob1() {
		wacodisJob1 = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		wacodisJob1.setId(id);
		wacodisJob1.setCreated(new DateTime());
		wacodisJob1.setName("Job1");
		wacodisJob1.setProcessingTool("de.wacodis.wps.landclassification");
		wacodisJob1.setTemporalCoverage(
				new WacodisJobDefinitionTemporalCoverage().duration("P10D").offset("P5D").previousExecution(false));

		AbstractSubsetDefinition subsetDef1 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_1")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION)
				.temporalCoverage(new AbstractSubsetDefinitionTemporalCoverage().duration("P14D").offset("P14D"));

		((CopernicusSubsetDefinition) subsetDef1).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef1).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef1).setMaximumCloudCoverage(10f);

		AbstractSubsetDefinition subsetDef2 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_2")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION);

		((CopernicusSubsetDefinition) subsetDef2).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef2).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef2).setMaximumCloudCoverage(10f);

		List<AbstractSubsetDefinition> inputList = new ArrayList<AbstractSubsetDefinition>();
		inputList.add(subsetDef1);
		inputList.add(subsetDef2);
		wacodisJob1.setInputs(inputList);

		wacodisJob1.setAreaOfInterest(areaOfInterest1);

		AbstractWacodisJobExecutionEvent eventType = new SingleJobExecutionEvent()
				.temporalCoverageEndDate(DateTime.parse("2020-12-24")).eventType(EventTypeEnum.SINGLEJOBEXECUTIONEVENT);
		;
		WacodisJobDefinitionExecution wacodisJobDefinitionExecution = new WacodisJobDefinitionExecution();
		wacodisJobDefinitionExecution.setPattern("*/1 * * * *");
		wacodisJob1.setExecution(wacodisJobDefinitionExecution);
	}

	private void prepareJob2() {
		wacodisJob2_intersecting = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		wacodisJob2_intersecting.setId(id);
		wacodisJob2_intersecting.setCreated(new DateTime());
		wacodisJob2_intersecting.setName("Job2");
		wacodisJob2_intersecting.setProcessingTool("de.wacodis.wps.landclassification");
		wacodisJob2_intersecting.setTemporalCoverage(
				new WacodisJobDefinitionTemporalCoverage().duration("P10D").offset("P5D").previousExecution(false));

		AbstractSubsetDefinition subsetDef1 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_1")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION)
				.temporalCoverage(new AbstractSubsetDefinitionTemporalCoverage().duration("P14D").offset("P14D"));

		((CopernicusSubsetDefinition) subsetDef1).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef1).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef1).setMaximumCloudCoverage(10f);

		AbstractSubsetDefinition subsetDef2 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_2")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION);

		((CopernicusSubsetDefinition) subsetDef2).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef2).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef2).setMaximumCloudCoverage(10f);

		List<AbstractSubsetDefinition> inputList = new ArrayList<AbstractSubsetDefinition>();
		inputList.add(subsetDef1);
		inputList.add(subsetDef2);
		wacodisJob2_intersecting.setInputs(inputList);

		wacodisJob2_intersecting.setAreaOfInterest(areaOfInterest2);

		AbstractWacodisJobExecutionEvent eventType = new SingleJobExecutionEvent()
				.temporalCoverageEndDate(DateTime.parse("2020-12-24")).eventType(EventTypeEnum.SINGLEJOBEXECUTIONEVENT);
		;
		WacodisJobDefinitionExecution wacodisJobDefinitionExecution = new WacodisJobDefinitionExecution();
		wacodisJobDefinitionExecution.setPattern("*/1 * * * *");
		wacodisJob2_intersecting.setExecution(wacodisJobDefinitionExecution);
	}

	private void prepareJob3() {
		wacodisJob3_disjoint = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		wacodisJob3_disjoint.setId(id);
		wacodisJob3_disjoint.setCreated(new DateTime());
		wacodisJob3_disjoint.setName("Job3");
		wacodisJob3_disjoint.setProcessingTool("de.wacodis.wps.landclassification");
		wacodisJob3_disjoint.setTemporalCoverage(
				new WacodisJobDefinitionTemporalCoverage().duration("P10D").offset("P5D").previousExecution(false));

		AbstractSubsetDefinition subsetDef1 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_1")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION)
				.temporalCoverage(new AbstractSubsetDefinitionTemporalCoverage().duration("P14D").offset("P14D"));

		((CopernicusSubsetDefinition) subsetDef1).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef1).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef1).setMaximumCloudCoverage(10f);

		AbstractSubsetDefinition subsetDef2 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_2")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION);

		((CopernicusSubsetDefinition) subsetDef2).setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
		((CopernicusSubsetDefinition) subsetDef2).setProductType("L2A");
		((CopernicusSubsetDefinition) subsetDef2).setMaximumCloudCoverage(10f);

		List<AbstractSubsetDefinition> inputList = new ArrayList<AbstractSubsetDefinition>();
		inputList.add(subsetDef1);
		inputList.add(subsetDef2);
		wacodisJob3_disjoint.setInputs(inputList);

		wacodisJob3_disjoint.setAreaOfInterest(areaOfInterest1);

		AbstractWacodisJobExecutionEvent eventType = new SingleJobExecutionEvent()
				.temporalCoverageEndDate(DateTime.parse("2020-12-24")).eventType(EventTypeEnum.SINGLEJOBEXECUTIONEVENT);
		;
		WacodisJobDefinitionExecution wacodisJobDefinitionExecution = new WacodisJobDefinitionExecution();
		wacodisJobDefinitionExecution.setPattern("*/1 * * * *");
		wacodisJob3_disjoint.setExecution(wacodisJobDefinitionExecution);
	}

	private Collection<JobDetail> generateQuartzJobDefinitions(WacodisJobDefinition job, JobFactory factory) {
		JobDataMap data = new JobDataMap();
		data.put(AOI_KEY, job.getAreaOfInterest());

		return factory.initializeJobs(job, data);
	}

	@Test
	public void test() throws Exception {

		JobFactory factory = new JobFactoryImpl();

		// has only one value
		Collection<JobDetail> quartzJobDefinitions = generateQuartzJobDefinitions(wacodisJob1, factory);
		Collection<JobDetail> quartzJobDefinitions2 = generateQuartzJobDefinitions(wacodisJob2_intersecting, factory);
		Collection<JobDetail> quartzJobDefinitions3 = generateQuartzJobDefinitions(wacodisJob3_disjoint, factory);

		JobDetail refJobDetail = (JobDetail) quartzJobDefinitions.toArray()[0];
		JobDetail jobDetail_intersecting = (JobDetail) quartzJobDefinitions2.toArray()[0];
		JobDetail jobDetail_disjoint = (JobDetail) quartzJobDefinitions3.toArray()[0];

		JobKey JobKey2 = jobDetail_intersecting.getKey();
		JobKey JobKey3 = jobDetail_disjoint.getKey();

		List<JobKey> jobKeyCandidates = new ArrayList<JobKey>();
		jobKeyCandidates.add(JobKey2);
		jobKeyCandidates.add(JobKey3);

		boolean anyQuartzJobIntersectsNewWacodisJobBBOX = bboxHelper
				.anyQuartzJobIntersectsNewWacodisJobBBOX(jobKeyCandidates, refJobDetail);
		Assert.assertTrue(anyQuartzJobIntersectsNewWacodisJobBBOX);

		JobKey intersectingQuartzJob = bboxHelper.findQuartzJobKeyIntersectingNewWacodisJobBBOX(jobKeyCandidates,
				refJobDetail);
		Assert.assertThat(intersectingQuartzJob, CoreMatchers.equalTo(jobDetail_intersecting.getKey()));

		// if successfully expanded then the refJobDetail was overwritten and now uses
		// the same BBOX as job_intersecting
		JobDetail expandedBBoxJob = bboxHelper.expandBboxOfExistingQuartzJob(refJobDetail, jobDetail_intersecting,
				AOI_KEY);
		Assert.assertThat(refJobDetail.getJobDataMap().get(AOI_KEY),
				CoreMatchers.not(expandedBBoxJob.getJobDataMap().get(AOI_KEY)));
		Assert.assertThat(expandedBBoxJob.getJobDataMap().get(AOI_KEY),
				CoreMatchers.equalTo(areaOfInterest_merged));
		
		// add and remove ids nd bboxes to job data map
		// and check resulting bbox of new job
		
		bboxHelper.addWacodisJobIdAndBBOXToJobDataMap(expandedBBoxJob, wacodisJob1.getId(), wacodisJob1.getAreaOfInterest());
		bboxHelper.addWacodisJobIdAndBBOXToJobDataMap(expandedBBoxJob, wacodisJob2_intersecting.getId(), wacodisJob2_intersecting.getAreaOfInterest());
		
		Assert.assertThat(expandedBBoxJob.getJobDataMap().getString(wacodisJob1.getId().toString()), CoreMatchers.equalTo(bboxHelper.getBboxStringFromAreaOfInterest(wacodisJob1.getAreaOfInterest().getExtent())));
		Assert.assertThat(expandedBBoxJob.getJobDataMap().getString(wacodisJob2_intersecting.getId().toString()), CoreMatchers.equalTo(bboxHelper.getBboxStringFromAreaOfInterest(wacodisJob2_intersecting.getAreaOfInterest().getExtent())));
		
		bboxHelper.removeWacodisJobIdAndBBOXFromJobDataMap(expandedBBoxJob, wacodisJob1.getId());		
		HashSet<UUID> remainingWacodisJobIds = new HashSet<UUID>();
		remainingWacodisJobIds.add(wacodisJob2_intersecting.getId());
		JobDetail newJob = bboxHelper.regenerateBboxForQuartzJob(expandedBBoxJob, wacodisJob1.getId(), remainingWacodisJobIds, AOI_KEY);
		String bbox_newJob = bboxHelper.getBboxSubstringFromQuartzJobKey(newJob.getKey());
		Assert.assertThat(bbox_newJob, CoreMatchers.equalTo(bboxHelper.getBboxStringFromAreaOfInterest(wacodisJob2_intersecting.getAreaOfInterest().getExtent())));
		Assert.assertThat(newJob.getJobDataMap().get(AOI_KEY), CoreMatchers.equalTo(areaOfInterest2));
	
	}

}
