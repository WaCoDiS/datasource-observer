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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.wacodis.dwd.cdc.model.DwdRequestParamsEncoder;
import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DwdWfsRequestParamsEncoderTest {

	private static String version;
	private static String typeName;
	private static List<Float> extent = new ArrayList<Float>();
	private static DateTime startDate;
	private static DateTime endDate;
	private static DwdRequestParamsEncoder encoder;

	@BeforeAll
	static void setup() throws ParseException {
		version = "2.0.0";
		typeName = "CDC:VGSL_FX_MN003";

		extent.add(6.966f);
		extent.add(51.402f);
		extent.add(6.969f);
		extent.add(51.405f);
		
		startDate = DateTime.parse("2019-04-24T01:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		endDate = DateTime.parse("2019-04-25T10:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		encoder = new DwdRequestParamsEncoder();

	}

	@DisplayName("Test DWD Params Encoder Method")
	@Test
	void testEncodeParams() throws Exception {
		DwdWfsRequestParams params = encoder.encode(version, typeName, extent, startDate, endDate);

		Assertions.assertEquals(version, params.getVersion());
		Assertions.assertEquals(typeName, params.getTypeName());
		Assertions.assertEquals(startDate, params.getStartDate());
		Assertions.assertEquals(endDate, params.getEndDate());
		Assertions.assertEquals(extent.get(0), params.getEnvelope().getMinLon());
		Assertions.assertEquals(extent.get(1), params.getEnvelope().getMinLat());
		Assertions.assertEquals(extent.get(2), params.getEnvelope().getMaxLon());
		Assertions.assertEquals(extent.get(3), params.getEnvelope().getMaxLat());
	}


}