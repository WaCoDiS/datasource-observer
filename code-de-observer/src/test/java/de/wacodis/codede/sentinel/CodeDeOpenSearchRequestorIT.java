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

import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CodeDeOpenSearchRequestorIT {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String SATELLITE = "Sentinel2";
    private static final String INSTRUMENT = "MSI";
    private static final String PRODUCT_TYPE = "L2A";
    private static final String PROCESSING_LEVEL = "LEVEL2A";
    private static final String SENSOR_MODE = null;
    private static final String START_DATE = "2019-01-01T00:00:00Z";
    private static final String COMPLETION_DATE = "2020-01-17T00:00:00Z";
    private static final Float[] BBOX = new Float[]{6.96f, 50.9f, 8.02f, 51.2f};
    private static final Float[] CLOUD_COVER = new Float[]{0.f, 40.f};

    private static CodeDeRequestParams params;
    private static CodeDeRequestor requestor;

    @BeforeAll
    public static void setUp() throws Exception {

        requestor = new CodeDeRequestor();
        requestor.setHttpClient(HttpClients.createDefault());
        requestor.setRequestBuilder(new CodeDeFinderApiRequestBuilder());

        params = new CodeDeRequestParams(SATELLITE, INSTRUMENT, PRODUCT_TYPE, PROCESSING_LEVEL,
                DateTime.parse(START_DATE, FORMATTER), DateTime.parse(COMPLETION_DATE, FORMATTER),
                BBOX, SENSOR_MODE, CLOUD_COVER);


    }

    @Test
    public void request() throws Exception {
        // expected metadata object
        List<CodeDeProductsMetadata> metadataList = requestor.request(params);
        CodeDeProductsMetadata actualMetadataObject = metadataList.get(0);
        Assertions.assertEquals("1aa97681-db20-5878-bc4b-17f3157c6b66", actualMetadataObject.getDatasetId());

//        Assertions.assertEquals(expectedMetadataObject.getDownloadLink(), actualMetadataObject.getDownloadLink());
//        Assertions.assertEquals(expectedMetadataObject.getCloudCover(), actualMetadataObject.getCloudCover(), 0.001);

//        Assertions.assertEquals(expectedMetadataObject.getStartDate(), actualMetadataObject.getStartDate());
//        Assertions.assertEquals(expectedMetadataObject.getEndDate(), actualMetadataObject.getEndDate());
//        Assertions.assertEquals(expectedMetadataObject.getAreaOfInterest(), actualMetadataObject.getAreaOfInterest());
    }

}