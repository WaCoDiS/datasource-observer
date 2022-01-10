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
package de.wacodis.sensorweb.encode;

import static org.junit.Assert.*;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.n52.svalbard.encode.exception.EncodingException;

import com.google.common.collect.Lists;

import de.wacodis.sensorweb.encode.GetObservationReqEncoder;
import de.wacodis.sensorweb.util.SimpleFileReader;

public class GetObservationReqEncoderTest {

	@Test
	public void encodeShouldBuildValidGetObservationRequest() {
		// parameters to identify data
		List<String> procedures = Lists.newArrayList("Einzelwert");
		List<String> observedProperties = Lists.newArrayList("Wassertemperatur");
		List<String> offerings = Lists.newArrayList("Zeitreihen_Einzelwert");
		List<String> featureIdentifiers = Lists.newArrayList("Laaken");
		
		DateTime start = DateTime.parse("2018-03-03T00:00:00+01:00");
		DateTime end = DateTime.parse("2018-03-03T12:00:00+01:00");
				
		GetObservationReqEncoder encoder = new GetObservationReqEncoder();
		String encoded = "";
		String assertion = "";
		try {
			assertion = SimpleFileReader.readFile("src/test/resources/GetObservationRequest.txt");
			encoded = encoder.encode(procedures, observedProperties, offerings, featureIdentifiers, start.minusSeconds(1), end.plusSeconds(1));
			
		} catch (EncodingException e) {
			e.printStackTrace();
		}
		assertion = assertion.replaceAll("id=\"tp_[0-9a-f]*\"", "id=\"tp_101010");
		encoded = encoded.replaceAll("id=\"tp_[0-9a-f]*\"", "id=\"tp_101010");
		assertEquals(assertion, encoded);
	}
}

