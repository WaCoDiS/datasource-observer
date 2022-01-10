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

import static org.junit.Assert.*;

import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.junit.Test;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.exception.EncodingException;

import com.google.common.collect.Lists;

import de.wacodis.sensorweb.http.DummyHttpPost;
import de.wacodis.sensorweb.observer.ObservationObserver;
import de.wacodis.sensorweb.util.SimpleFileReader;

public class ObservationObserverTest {

	@Test
	public void checkForAvailableUpdatesShouldReturnTrue() {
		// parameters to identify data
		String url = "http://fluggs.wupperverband.de/sos2/sos/soap";
		List<String> procedures = Lists.newArrayList("Einzelwert");
		List<String> observedProperties = Lists.newArrayList("Wassertemperatur");
		List<String> offerings = Lists.newArrayList("Zeitreihen_Einzelwert");
		List<String> featureIdentifiers = Lists.newArrayList("Laaken");
		
		DummyHttpPost post = new DummyHttpPost("application/soap+xml", "application/soap+xml");
		post.setGetDataAvailability(true);

		ObservationObserver observer = new ObservationObserver(url, procedures, observedProperties, offerings, featureIdentifiers);
		observer.setDateOfLastObs(new DateTime(2018, 8, 1, 0, 0, 0));
		observer.setPost(post);
		
		try {
			boolean value = observer.checkForAvailableUpdates(new DateTime(2018, 8, 3, 0, 0, 0));
			assertTrue(value);
		} catch (EncodingException | DecodingException | XmlException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void updateObservationsShouldReturnValidObservations() {
		String url = "http://fluggs.wupperverband.de/sos2/sos/soap";
		List<String> procedures = Lists.newArrayList("Einzelwert");
		List<String> observedProperties = Lists.newArrayList("Wassertemperatur");
		List<String> offerings = Lists.newArrayList("Zeitreihen_Einzelwert");
		List<String> featureIdentifiers = Lists.newArrayList("Laaken");
		DateTime start = new DateTime(2018, 3, 3, 0, 0, 0);
		DateTime end = new DateTime(2018, 3, 3, 12, 0, 0);
		
		DummyHttpPost post = new DummyHttpPost("application/soap+xml", "application/soap+xml");
		post.setGetObservation(true);

		ObservationObserver observer = new ObservationObserver(url, procedures, observedProperties, offerings,
				featureIdentifiers);
		observer.setPost(post);
		observer.updateObservations(start, end);


		String assertion = SimpleFileReader.readFile("src/test/resources/GetObservation.txt");
		String[] obs = assertion.split(",");

		List<OmObservation> results = observer.getObservations();
		TimeInstant tFirst = (TimeInstant) results.get(0).getPhenomenonTime();
		TimeInstant tLast = (TimeInstant) results.get(results.size() - 1).getPhenomenonTime();

		assertEquals(obs[0], tFirst.getValue().toString());
		assertEquals(obs[1], results.get(0).getValue().getValue().getValue().toString());
		assertEquals(obs[2], results.get(0).getValue().getValue().getUnit());
		assertEquals(obs[3], tLast.getValue().toString());
		assertEquals(obs[4], results.get(results.size() - 1).getValue().getValue().getValue().toString());
		assertEquals(obs[5], results.get(results.size() - 1).getValue().getValue().getUnit());

	}

}
