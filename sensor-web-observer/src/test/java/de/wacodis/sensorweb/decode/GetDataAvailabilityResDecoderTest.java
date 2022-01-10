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
package de.wacodis.sensorweb.decode;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.junit.Test;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.DataAvailability;
import org.n52.svalbard.decode.exception.DecodingException;

import de.wacodis.sensorweb.decode.GetDataAvailabilityResDecoder;
import de.wacodis.sensorweb.util.SimpleFileReader;

public class GetDataAvailabilityResDecoderTest {

	@Test
	public void decodeShouldGiveValidDateOfLastObservation() {

		String response = SimpleFileReader.readFile("src/test/resources/GetDataAvailabilityResponse.txt");
		String assertion = SimpleFileReader.readFile("src/test/resources/GetDataAvailability.txt");
		
		GetDataAvailabilityResDecoder decoder = new GetDataAvailabilityResDecoder();
		List<DataAvailability> result = null;
		try {
			result = decoder.decode(response);
		} catch (DecodingException | XmlException e) {
			e.printStackTrace();
		}
		assertEquals(assertion, result.get(0).getPhenomenonTime().getEnd().toString());
	}
}
