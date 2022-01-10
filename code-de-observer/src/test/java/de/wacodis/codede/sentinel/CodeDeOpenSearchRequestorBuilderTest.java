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
package de.wacodis.codede.sentinel;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class CodeDeOpenSearchRequestorBuilderTest {

    private static CodeDeRequestParams params;
    private static final String SATELLITE = "Sentinel2";
    private static final String INSTRUMENT = "MSI";
    private static final String PRODUCT_TYPE = "L2A";
    private static final String PROCESSING_LEVEL = "LEVEL2A";
    private static final String SENSOR_MODE = null;
    private static final String START_DATE = "2019-01-01T00:00:00Z";
    private static final String COMPLETION_DATE = "2020-01-17T00:00:00Z";
    private static final Float[] BBOX = new Float[]{6.96f, 50.9f, 8.02f, 51.2f};
    private static final Float[] CLOUD_COVER = new Float[]{0.f, 40.f};

    @BeforeAll
    public static void setup() {
        params = new CodeDeRequestParams(SATELLITE, INSTRUMENT, PRODUCT_TYPE, PROCESSING_LEVEL,
                DateTime.parse(START_DATE, CodeDeFinderApiRequestBuilder.FORMATTER),
                DateTime.parse(COMPLETION_DATE, CodeDeFinderApiRequestBuilder.FORMATTER), BBOX, SENSOR_MODE, CLOUD_COVER);
    }

    @Test
    public void testBuilder() {
        CodeDeOpenSearchRequestorBuilder requestorBuilder = new CodeDeOpenSearchRequestorBuilder();
        String actualRequestUrl = requestorBuilder.buildGetRequestUrl(params, 1);
        String expectedRequestUrl = "https://catalog.code-de.org/opensearch/request?" +
                "parentIdentifier=EOP:CODE-DE:S2_MSI_L2A&" +
                "startDate=2019-01-01T00:00:00Z&" +
                "endDate=2020-01-17T00:00:00Z&" +
                "bbox=6.96,50.9,8.02,51.2&" +
                "cloudCover=[0.0,40.0]&" +
                "maximumRecords=50&" +
                "recordSchema=om&" +
                "startPage=1";
        Assertions.assertEquals(expectedRequestUrl, actualRequestUrl);
    }

}