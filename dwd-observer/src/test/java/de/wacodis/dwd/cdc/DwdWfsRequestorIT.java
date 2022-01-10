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
package de.wacodis.dwd.cdc;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import de.wacodis.dwd.cdc.model.DwdProductsMetadata;
import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import de.wacodis.dwd.cdc.model.Envelope;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class DwdWfsRequestorIT {

	private static DwdWfsRequestParams params;
	private static DwdWfsRequestorBuilder reader;
	private static DwdWfsRequestor requestor;
	private static String SERVICE_URL = "https://cdc.dwd.de:443/geoserver/CDC/wfs?";
	

	@BeforeAll
	static void setup() throws Exception {
		
		params = new DwdWfsRequestParams();
		params.setVersion("2.0.0");
		params.setTypeName("FX_MN003");
		// params.setTypeName("CDC:VGSL_TT_TU_MN009");

		Envelope envelope = new Envelope();
		envelope.setMinLon(6.6000f);
		envelope.setMinLat(51.0000f);
		envelope.setMaxLon(7.3000f);
		envelope.setMaxLat(51.5000f);

		params.setEnvelope(envelope);

		DateTime startDate = DateTime.parse("2019-04-24T01:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		DateTime endDate = DateTime.parse("2019-04-25T10:00:00Z", DwdWfsRequestorBuilder.FORMATTER);

		params.setStartDate(startDate);
		params.setEndDate(endDate);

		reader = new DwdWfsRequestorBuilder(params);
		requestor = new DwdWfsRequestor();
		requestor.setHttpClient(HttpClients.createDefault());
		requestor.afterPropertiesSet();
	}

	@DisplayName("Test request Method")
	@Test
	void testRequest() throws IOException, ParserConfigurationException, SAXException {
		DwdProductsMetadata result = new DwdProductsMetadata();
		result = requestor.request(SERVICE_URL, params);

		// object of comparison
		DwdProductsMetadata metadata = new DwdProductsMetadata();

		metadata.setServiceUrl(SERVICE_URL);
		metadata.setLayerName("CDC:VGSL_FX_MN003");
		metadata.setParameter("Tägliche Stationsmessungen der maximalen Windspitze in ca. 10 m Höhe in m/s");

		Envelope envelope = new Envelope();
		envelope.setMinLon(6.7686f);
		envelope.setMinLat(51.2531f);
		envelope.setMaxLon(7.2156f);
		envelope.setMaxLat(51.4041f);

		metadata.setEnvelope(envelope);

		DateTime startDate = DateTime.parse("2010-04-24T02:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		DateTime endDate = DateTime.parse("2019-04-25T02:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		metadata.setStartDate(startDate);
		metadata.setEndDate(endDate);

		// Comparison
		// ServiceURL
		Assertions.assertEquals(metadata.getServiceUrl(), result.getServiceUrl());
		// LayerName
		Assertions.assertEquals(metadata.getLayerName(), result.getLayerName());
		// ClearName
		Assertions.assertEquals(metadata.getParameter(), result.getParameter());
		// bbox
		Assertions.assertEquals(metadata.getEnvelope().getMinLon(), result.getEnvelope().getMinLon());
		Assertions.assertEquals(metadata.getEnvelope().getMinLat(), result.getEnvelope().getMinLat());
		Assertions.assertEquals(metadata.getEnvelope().getMaxLon(), result.getEnvelope().getMaxLon());
		Assertions.assertEquals(metadata.getEnvelope().getMaxLat(), result.getEnvelope().getMaxLat());
		// time
		// is the startDate of the feature timeframe between the query startDate and
		// endDate
		int biggerThanStartDate = result.getStartDate().compareTo(metadata.getStartDate());
		int lowerThanEndDate = result.getStartDate().compareTo(metadata.getEndDate());
		Assertions.assertTrue(biggerThanStartDate >= 0 && lowerThanEndDate <= 0);
		// is the endDate of the feature timeframe between the query startDate and
		// endDate
		biggerThanStartDate = result.getEndDate().compareTo(metadata.getStartDate());
		lowerThanEndDate = result.getEndDate().compareTo(metadata.getEndDate());
		Assertions.assertTrue(biggerThanStartDate >= 0 && lowerThanEndDate <= 0);

	}

	@DisplayName("Test correct throwing of Exceptions")
	@Test
	void testExceptions() {
		// request
		Assertions.assertDoesNotThrow(() -> {
			requestor.request(SERVICE_URL, params);
		});

	}

	

}