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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wacodis.codede.sentinel.exception.ParsingException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class CodeDeResponseJsonResolverTest {

    private static JsonNode resultNode;
    private static JsonNode featureNode;
    private static JsonNode failingFeatureNode;
    private static CodeDeResponseJsonResolver resolver;

    @BeforeAll
    static void init() throws IOException {
        InputStream input = CodeDeResponseResolverTest.class.getClassLoader().getResourceAsStream("code-de-test.json");
        ObjectMapper mapper = new ObjectMapper();
        resultNode = mapper.readTree(input);
        featureNode = resultNode.at("/features/0");
        failingFeatureNode = resultNode.at("/features/1");
        resolver = new CodeDeResponseJsonResolver();
    }

    @Test
    void testGetIdentifier() throws ParsingException {
        String expId = "716581ca-6e69-5ab0-bea8-05ad87d5af11";
        String identifier = resolver.getIdentifier(featureNode);

        Assertions.assertEquals(expId, identifier);
    }

    @Test
    void testGetIdentifierShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getIdentifier(failingFeatureNode));
    }

    @Test
    void testGetSatellite() throws ParsingException {
        String expId = "Sentinel2";
        String identifier = resolver.getSatellite(featureNode);

        Assertions.assertEquals(expId, identifier);
    }

    @Test
    void testGetSatlliteShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getSatellite(failingFeatureNode));
    }

    @Test
    void testGetProductIdentifier() throws ParsingException {
        String expId = "/codede/Sentinel-2/MSI/L1C/2020/03/30/S2B_MSIL1C_20200330T102609_N0209_R108_T31UGS_20200330T124538.SAFE";
        String identifier = resolver.getProductIdentifier(featureNode);

        Assertions.assertEquals(expId, identifier);
    }

    @Test
    void testGetProductIdentifierShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getProductIdentifier(failingFeatureNode));
    }

    @Test
    void testGetDownloadLink() throws ParsingException {
        String expLink = "https://zipper.prod.cloud.code-de.org/download/716581ca-6e69-5ab0-bea8-05ad87d5af11";
        String downloadLink = resolver.getDownloadLink(featureNode);

        Assertions.assertEquals(expLink, downloadLink);
    }

    @Test
    void testGetDownloadLinkShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getDownloadLink(failingFeatureNode));
    }

    @Test
    void testGetTimeFrame() throws ParsingException {
        String expStartDate = "2020-03-30T10:26:09.024Z";
        String expCompletionDate = "2020-03-30T10:26:09.024Z";
        DateTime[] timeFrame = resolver.getTimeFrame(featureNode);

        Assertions.assertEquals(expStartDate, timeFrame[0].toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        Assertions.assertEquals(expCompletionDate, timeFrame[1].toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    @Test
    void testGetTimeFrameShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getTimeFrame(failingFeatureNode));
    }

    @Test
    void testGetCloudCoverage() {
        float expCloudCover = 12.6535f;
        float cloudCover = resolver.getCloudCoverage(featureNode);

        Assertions.assertEquals(expCloudCover, cloudCover);
    }

    @Test
    void testGetCloudCoverageShouldReturnNullForNonValidValue() throws ParsingException {
        Assertions.assertNull(resolver.getCloudCoverage(failingFeatureNode));
    }

    @Test
    void testGetBbox() throws ParsingException {
        Float[] expBbox = {6.574941793f, 50.382122251f, 7.450589699f, 51.38167062f};
        Float[] bbox = resolver.getBbox(featureNode);

        Assertions.assertArrayEquals(expBbox, bbox);
    }

    @Test
    void testGetBboxShouldThrowPArsingExceptionForMissingNode() {
        Assertions.assertThrows(ParsingException.class, () -> resolver.getBbox(failingFeatureNode));
    }
}
