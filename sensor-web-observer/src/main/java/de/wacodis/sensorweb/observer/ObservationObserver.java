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
package de.wacodis.sensorweb.observer;

import java.io.Serializable;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.exception.EncodingException;

import de.wacodis.observer.http.SimpleHttpPost;
import de.wacodis.sensorweb.decode.GetDataAvailabilityResDecoder;
import de.wacodis.sensorweb.decode.GetObservationResDecoder;
import de.wacodis.sensorweb.encode.GetDataAvailabilityReqEncoder;
import de.wacodis.sensorweb.encode.GetObservationReqEncoder;

public class ObservationObserver implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<String> procedures;
	private List<String> observedProperties;
	private List<String> offerings;
	private List<String> featureIdentifiers;
	private String url;
	
	private List<OmObservation> observations;
	private DateTime dateOfLastObs;
	private DateTime dateOfFirstObs;
	
	private GetObservationReqEncoder observationEncoder;
	private GetDataAvailabilityReqEncoder availabilityEncoder;
	private GetObservationResDecoder observationDecoder;
	private GetDataAvailabilityResDecoder availabilityDecoder;
	
	private SimpleHttpPost post;


	public ObservationObserver(String url, List<String> procedures, List<String> observedProperties, 
							List<String> offerings, List<String> featureIdentifiers) {
		this.url = url;
		this.procedures = procedures;
		this.observedProperties = observedProperties;
		this.offerings = offerings;
		this.featureIdentifiers = featureIdentifiers;
		
		this.observationEncoder = new GetObservationReqEncoder();
		this.availabilityEncoder = new GetDataAvailabilityReqEncoder();
		this.observationDecoder = new GetObservationResDecoder();
		this.availabilityDecoder = new GetDataAvailabilityResDecoder();
		this.post = new SimpleHttpPost("application/soap+xml", "application/soap+xml");
	}
	
	/**
	 * initialize dates of last observations with valid values.
	 * @return 
	 */
	public DateTime initalizeDatesOfObservation() {
		try {
			String request = availabilityEncoder.encode(procedures, observedProperties, offerings, featureIdentifiers);
			String response = post.doPost(url, request);
			dateOfFirstObs = availabilityDecoder.decode(response).get(0).getPhenomenonTime().getStart();
			dateOfLastObs = availabilityDecoder.decode(response).get(0).getPhenomenonTime().getEnd();
			return dateOfLastObs;
		} catch (EncodingException | DecodingException | XmlException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param dateOfLastObservation 
	 * @return boolean true if date of last observation is before current observation.
	 * @throws EncodingException
	 * @throws DecodingException
	 * @throws XmlException
	 */
	public boolean checkForAvailableUpdates(DateTime dateOfLastObservation) throws EncodingException, DecodingException, XmlException {
		String request;
		if(dateOfLastObservation == null) {
			throw new NullPointerException("date of last observation must not be null!");
		}
		request = availabilityEncoder.encode(procedures, observedProperties, offerings, featureIdentifiers);
		String response = post.doPost(url, request);
		DateTime responseDate = availabilityDecoder.decode(response).get(0).getPhenomenonTime().getEnd();
		dateOfFirstObs = availabilityDecoder.decode(response).get(0).getPhenomenonTime().getStart();
		if(responseDate != null && dateOfLastObservation.isBefore(responseDate)) {
			dateOfLastObs = responseDate;
			return true;
		}
		return false;
	}
	
	/**
	 * updates the list of observations for a given time period
	 * @param start 
	 * @param end
	 */
	public void updateObservations(DateTime start, DateTime end) {
		if (start == null || end == null) {
			throw new NullPointerException("date of start and end must not be null!");
		}
		try {
			String request = observationEncoder.encode(procedures, observedProperties, offerings,
					featureIdentifiers, start.minusSeconds(1), end.plusSeconds(1)); 
			String response = post.doPost(url, request);
			setObservations(observationDecoder.decode(response));
		} catch (EncodingException | DecodingException | XmlException | OwsExceptionReport e) {
			e.printStackTrace();
		}
	}


	// setters/getters
	public List<String> getProcedures() {
		return procedures;
	}

	public void setProcedures(List<String> procedures) {
		this.procedures = procedures;
	}

	public List<String> getObservedProperties() {
		return observedProperties;
	}

	public void setObservedProperties(List<String> observedProperties) {
		this.observedProperties = observedProperties;
	}

	public List<String> getOfferings() {
		return offerings;
	}

	public void setOfferings(List<String> offerings) {
		this.offerings = offerings;
	}

	public List<String> getFeatureIdentifiers() {
		return featureIdentifiers;
	}

	public void setFeatureIdentifiers(List<String> featureIdentifiers) {
		this.featureIdentifiers = featureIdentifiers;
	}

	public DateTime getDateOfLastObs() {
		return dateOfLastObs;
	}

	public void setDateOfLastObs(DateTime dateOfLastObs) {
		this.dateOfLastObs = dateOfLastObs;
	}

	public List<OmObservation> getObservations() {
		return observations;
	}

	public void setObservations(List<OmObservation> observations) {
		this.observations = observations;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public SimpleHttpPost getPost() {
		return post;
	}
	
	public void setPost(SimpleHttpPost post) {
		this.post = post;
	}

	public DateTime getDateOfFirstObs() {
		return dateOfFirstObs;
	}

	public void setDateOfFirstObs(DateTime dateOfFirstObs) {
		this.dateOfFirstObs = dateOfFirstObs;
	}	
	

}
