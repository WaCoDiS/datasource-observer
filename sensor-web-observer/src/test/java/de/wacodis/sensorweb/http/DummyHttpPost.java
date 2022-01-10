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
package de.wacodis.sensorweb.http;

import de.wacodis.observer.http.SimpleHttpPost;
import de.wacodis.sensorweb.util.SimpleFileReader;

public class DummyHttpPost extends SimpleHttpPost{
	
	public DummyHttpPost(String contentType, String accept) {
		super(contentType, accept);
	}

	private boolean isGetDataAvailability = false;
	private boolean isGetObservation = false;
	
	
	
	@Override
	public String doPost(String url, String payload) {
		if(isGetDataAvailability) {
			String resp = SimpleFileReader.readFile("src/test/resources/GetDataAvailabilityResponse.txt");
			return resp;
		}
		else if (isGetObservation) {
			String resp = SimpleFileReader.readFile("src/test/resources/GetObservationResponse.txt");
			return resp;
		}
		else return null;
	}

	public boolean isGetDataAvailability() {
		return isGetDataAvailability;
	}

	public void setGetDataAvailability(boolean isGetDataAvailability) {
		this.isGetDataAvailability = isGetDataAvailability;
	}

	public boolean isGetObservation() {
		return isGetObservation;
	}

	public void setGetObservation(boolean isGetObservation) {
		this.isGetObservation = isGetObservation;
	}
	
	

}
