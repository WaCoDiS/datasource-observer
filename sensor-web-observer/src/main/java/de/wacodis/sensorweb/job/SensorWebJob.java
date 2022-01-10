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
package de.wacodis.sensorweb.job;

import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.exception.EncodingException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;

import de.wacodis.observer.model.AbstractDataEnvelope.SourceTypeEnum;
import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.model.AbstractDataEnvelopeTimeFrame;
import de.wacodis.observer.model.SensorWebDataEnvelope;
import de.wacodis.observer.publisher.PublisherChannel;
import de.wacodis.observer.quartz.QuartzServer;
import de.wacodis.sensorweb.observer.ObservationObserver;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SensorWebJob implements Job{
	
	
	private static final Logger log = LoggerFactory.getLogger(SensorWebJob.class);
		
	private List<String> procedures, observedProperties, offerings, featureIdentifiers;
	

	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		log.info("Start SensorWebJob's execute()");
		procedures = (List<String>) context.getMergedJobDataMap().get("procedures");
		observedProperties = (List<String>) context.getMergedJobDataMap().get("observedProperties");
		offerings = (List<String>) context.getMergedJobDataMap().get("offerings");
		featureIdentifiers = (List<String>) context.getMergedJobDataMap().get("featureIdentifiers");
		String serviceURL = (String) context.getMergedJobDataMap().get("serviceURL");
		ObservationObserver observer = new ObservationObserver(serviceURL, procedures, observedProperties, offerings, featureIdentifiers);
		if(context.getJobDetail().getJobDataMap().get("dateOfLastObs") == null) {
			context.getJobDetail().getJobDataMap().put("dateOfLastObs", observer.initalizeDatesOfObservation());
			context.getJobDetail().getJobDataMap().put("dateOfFirstObs", observer.getDateOfFirstObs());
		}

		try {
			DateTime dateOfLastObs = (DateTime) context.getJobDetail().getJobDataMap().get("dateOfLastObs");
			DateTime dateOfFirstObs = (DateTime) context.getJobDetail().getJobDataMap().get("dateOfFirstObs");

			if(observer.checkForAvailableUpdates(dateOfLastObs)) {	//if new data Available -> Publish new DataEnvelope
				dateOfLastObs = observer.getDateOfLastObs();
				dateOfFirstObs = observer.getDateOfFirstObs();
				
				//Build new SensorWebDataEnvelope
				SensorWebDataEnvelope dataEnvelope = new SensorWebDataEnvelope();
				
				dataEnvelope.setServiceUrl(context.getJobDetail().getJobDataMap().getString("serviceURL"));
				dataEnvelope.setOffering(((List<String>) context.getJobDetail().getJobDataMap().get("offerings")).get(0));
				dataEnvelope.setFeatureOfInterest(((List<String>) context.getJobDetail().getJobDataMap().get("featureIdentifiers")).get(0));
				dataEnvelope.setObservedProperty(((List<String>) context.getJobDetail().getJobDataMap().get("observedProperties")).get(0));
				dataEnvelope.setProcedure(((List<String>) context.getJobDetail().getJobDataMap().get("procedures")).get(0));
				dataEnvelope.setSourceType(SourceTypeEnum.SENSORWEBDATAENVELOPE);
				
				dataEnvelope.setAreaOfInterest((AbstractDataEnvelopeAreaOfInterest) context.getMergedJobDataMap().get("areaOfInterest"));
				dataEnvelope.setModified(dateOfLastObs);	// better done by ckan-connector?
				
				AbstractDataEnvelopeTimeFrame timeFrame = new AbstractDataEnvelopeTimeFrame();
				timeFrame.setStartTime(dateOfFirstObs);
				timeFrame.setEndTime(dateOfLastObs);
				dataEnvelope.setTimeFrame(timeFrame);
				
				
				log.info("New dataEnvelope:\n{}", dataEnvelope.toString());
				
				// publish DataEnvelope through PublishChannel to MessageBroker's Exchange
				
				PublisherChannel pub = (PublisherChannel) context.getScheduler().getContext().get(QuartzServer.PUBLISHER);
				publish(pub, dataEnvelope);

			}
			context.getJobDetail().getJobDataMap().put("dateOfLastObs", dateOfLastObs);
			context.getJobDetail().getJobDataMap().put("dateOfFirstObs", dateOfFirstObs);
			
			
		} catch (EncodingException | DecodingException | XmlException | SchedulerException e) {
			log.warn(e.getMessage(), e);
		}
		
	}
	
	public SensorWebDataEnvelope publish(PublisherChannel pub, SensorWebDataEnvelope data) {
		pub.sendDataEnvelope().send(MessageBuilder.withPayload(data).build());
		log.info("DataEnvelope published");
		return data;
	}

	public void setProcedures(List<String> procedures) {
		this.procedures = procedures;
	}

	public void setObservedProperties(List<String> observedProperties) {
		this.observedProperties = observedProperties;
	}

	public void setOfferings(List<String> offerings) {
		this.offerings = offerings;
	}

	public void setFeatureIdentifiers(List<String> featureIdentifiers) {
		this.featureIdentifiers = featureIdentifiers;
	}
	
}
