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
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDataMap;
import org.springframework.test.context.junit4.SpringRunner;

import de.wacodis.observer.core.TemporalCoverageConstants;
import de.wacodis.observer.core.WacodisJobConfiguration;
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
public class WacodisObserverTemporalCoverageIT {

	@Test
	public void testSingleExecutionJobWith2Inputs() {
		WacodisJobDefinition j = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		j.setId(id);
		j.setCreated(new DateTime());
		j.setName("SingleJobexecution");
		j.setProcessingTool("de.wacodis.wps.landclassification");
		j.setTemporalCoverage(
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
		j.setInputs(inputList);

		AbstractWacodisJobExecutionEvent eventType = new SingleJobExecutionEvent()
				.temporalCoverageEndDate(DateTime.parse("2020-12-24")).eventType(EventTypeEnum.SINGLEJOBEXECUTIONEVENT);
		;
		j.setExecution(new WacodisJobDefinitionExecution().event(eventType));

		JobDataMap dataMap1 = new JobDataMap();
		dataMap1 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap1, subsetDef1);
		
		DateTime startDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.END_DATE));

		//should match subsetDef
		Assert.assertNotNull(startDate1);
		Assert.assertNotNull(endDate1);
		Assert.assertThat(startDate1.toLocalDate().toString(), CoreMatchers.equalTo("2020-11-26"));
		Assert.assertThat(endDate1.toLocalDate().toString(), CoreMatchers.equalTo("2020-12-24"));
		
		
		
		JobDataMap dataMap2 = new JobDataMap();
		dataMap2 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap2, subsetDef2);
		
		DateTime startDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.END_DATE));

		// should match job level
		Assert.assertNotNull(startDate2);
		Assert.assertNotNull(endDate2);
		Assert.assertThat(startDate2.toLocalDate().toString(), CoreMatchers.equalTo("2020-12-09"));
		Assert.assertThat(endDate2.toLocalDate().toString(), CoreMatchers.equalTo("2020-12-24"));

	}
	
	@Test
	public void testRegularExecutionJobWith2InputsAndStartAt_previousExecutionFalse() {
		WacodisJobDefinition j = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		j.setId(id);
		j.setCreated(new DateTime());
		j.setName("RegularJobExecutionWithStartAt");
		j.setProcessingTool("de.wacodis.wps.landclassification");
		j.setTemporalCoverage(
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
		j.setInputs(inputList);

		j.setExecution(new WacodisJobDefinitionExecution().startAt(DateTime.parse("2022-01-01")).pattern("0 0 5 * *"));

		JobDataMap dataMap1 = new JobDataMap();
		dataMap1 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap1, subsetDef1);
		
		DateTime startDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.END_DATE));

		//should match subsetDef
		Assert.assertNotNull(startDate1);
		Assert.assertNotNull(endDate1);
		Assert.assertThat(startDate1.toLocalDate().toString(), CoreMatchers.equalTo("2021-12-08"));
		Assert.assertThat(endDate1.toLocalDate().toString(), CoreMatchers.equalTo("2022-01-05"));
		
		
		
		JobDataMap dataMap2 = new JobDataMap();
		dataMap2 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap2, subsetDef2);
		
		DateTime startDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.END_DATE));

		// should match job level
		Assert.assertNotNull(startDate2);
		Assert.assertNotNull(endDate2);
		Assert.assertThat(startDate2.toLocalDate().toString(), CoreMatchers.equalTo("2021-12-21"));
		Assert.assertThat(endDate2.toLocalDate().toString(), CoreMatchers.equalTo("2022-01-05"));

	}
	
	@Test
	public void testRegularExecutionJobWith2InputsAndStartAt_previousExecutionTrue() {
		WacodisJobDefinition j = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		j.setId(id);
		j.setCreated(new DateTime());
		j.setName("RegularJobExecutionWithStartAt");
		j.setProcessingTool("de.wacodis.wps.landclassification");
		j.setTemporalCoverage(
				new WacodisJobDefinitionTemporalCoverage().previousExecution(true));

		AbstractSubsetDefinition subsetDef1 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_1")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION);

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
		j.setInputs(inputList);

		j.setExecution(new WacodisJobDefinitionExecution().startAt(DateTime.parse("2022-01-01")).pattern("0 0 5 * *"));

		JobDataMap dataMap1 = new JobDataMap();
		dataMap1 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap1, subsetDef1);
		
		DateTime startDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.END_DATE));

		//should match subsetDef
		Assert.assertNotNull(startDate1);
		Assert.assertNotNull(endDate1);
		Assert.assertThat(startDate1.toLocalDate().toString(), CoreMatchers.equalTo("2021-12-05"));
		Assert.assertThat(endDate1.toLocalDate().toString(), CoreMatchers.equalTo("2022-01-05"));
		
		
		
		JobDataMap dataMap2 = new JobDataMap();
		dataMap2 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap2, subsetDef2);
		
		DateTime startDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.END_DATE));

		// should match job level
		Assert.assertNotNull(startDate2);
		Assert.assertNotNull(endDate2);
		Assert.assertThat(startDate2.toLocalDate().toString(), CoreMatchers.equalTo("2021-12-05"));
		Assert.assertThat(endDate2.toLocalDate().toString(), CoreMatchers.equalTo("2022-01-05"));

	}
	
	@Test
	public void testRegularExecutionJobWith2Inputs_previousExecutionTrue() {
		WacodisJobDefinition j = new WacodisJobDefinition();
		UUID id = UUID.randomUUID();
		j.setId(id);
		j.setCreated(new DateTime());
		j.setName("RegularJobExecutionWithStartAt");
		j.setProcessingTool("de.wacodis.wps.landclassification");
		j.setTemporalCoverage(
				new WacodisJobDefinitionTemporalCoverage().previousExecution(true));

		AbstractSubsetDefinition subsetDef1 = new CopernicusSubsetDefinition().identifier("OPTICAL_IMAGES_SOURCES_1")
				.sourceType(SourceTypeEnum.COPERNICUSSUBSETDEFINITION);

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
		j.setInputs(inputList);

		j.setExecution(new WacodisJobDefinitionExecution().pattern("0 0 5 * *"));

		JobDataMap dataMap1 = new JobDataMap();
		dataMap1 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap1, subsetDef1);
		
		DateTime startDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate1 = DateTime.parse((String)dataMap1.get(TemporalCoverageConstants.END_DATE));

		//should match subsetDef
		Assert.assertNotNull(startDate1);
		Assert.assertNotNull(endDate1);
		Assert.assertThat(startDate1.toLocalDate().toString(), CoreMatchers.equalTo("2020-12-05"));
		Assert.assertThat(endDate1.toLocalDate().toString(), CoreMatchers.equalTo("2021-01-05"));
		
		
		
		JobDataMap dataMap2 = new JobDataMap();
		dataMap2 = WacodisJobConfiguration.configureFirstDataQueryPeriod(j, dataMap2, subsetDef2);
		
		DateTime startDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.START_DATE));
		DateTime endDate2 = DateTime.parse((String)dataMap2.get(TemporalCoverageConstants.END_DATE));

		// should match job level
		Assert.assertNotNull(startDate2);
		Assert.assertNotNull(endDate2);
		Assert.assertThat(startDate2.toLocalDate().toString(), CoreMatchers.equalTo("2020-12-05"));
		Assert.assertThat(endDate2.toLocalDate().toString(), CoreMatchers.equalTo("2021-01-05"));

	}
}
