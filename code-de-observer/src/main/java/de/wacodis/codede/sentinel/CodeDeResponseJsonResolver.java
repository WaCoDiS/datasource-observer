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
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.wacodis.codede.sentinel.exception.ParsingException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.decode.json.GeoJSONDecoder;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class CodeDeResponseJsonResolver {

    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final int ITEMS_PER_PAGE = 50;

    private static final String SATELLITE_PATH = "/properties/collection";
    private static final String FEATURES_PATH = "/features";
    private static final String IDENTIFIER_PATH = "/id";
    private static final String PRODUCT_IDENTIFIER_PATH = "/properties/productIdentifier";
    private static final String DOWNLOAD_LINK_PATH = "/properties/services/download/url";
    private static final String START_DATE_PATH = "/properties/startDate";
    private static final String COMPLETION_DATE_PATH = "/properties/completionDate";
    private static final String CLOUD_COVERAGE_PATH = "/properties/cloudCover";
    private static final String GEOMETRY_PATH = "/geometry";
    private static final String TOTAL_RESULTS_PATH = "/properties/totalResults";

    private GeoJSONDecoder decoder;

    public CodeDeResponseJsonResolver() {
        decoder = new GeoJSONDecoder();
    }

    public CodeDeProductsMetadata resolveMetadata(JsonNode node) throws ParsingException {
        CodeDeProductsMetadata metadata = new CodeDeProductsMetadata();
        metadata.setDatasetId(getIdentifier(node));
        metadata.setSatellite(getSatellite(node));
        metadata.setAreaOfInterest(Arrays.asList(getBbox(node)));
        metadata.setStartDate(getTimeFrame(node)[0]);
        metadata.setEndDate(getTimeFrame(node)[1]);
        metadata.setCloudCover(getCloudCoverage(node));
        metadata.setDownloadLink(getDownloadLink(node));
        metadata.setProductIdentifier(getProductIdentifier(node));
        return metadata;
    }

    public ArrayNode resolveFeatures(JsonNode node) {
        return (ArrayNode) node.at(FEATURES_PATH);
    }

    String getIdentifier(JsonNode node) throws ParsingException {
        JsonNode idNode = node.at(IDENTIFIER_PATH);
        if (idNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", IDENTIFIER_PATH));
        }
        return idNode.asText();
    }

    String getSatellite(JsonNode node) throws ParsingException {
        JsonNode idNode = node.at(SATELLITE_PATH);
        if (idNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", SATELLITE_PATH));
        }
        return idNode.asText();
    }

    String getProductIdentifier(JsonNode node) throws ParsingException {
        JsonNode prodIdNode = node.at(PRODUCT_IDENTIFIER_PATH);
        if (prodIdNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", PRODUCT_IDENTIFIER_PATH));
        }
        return prodIdNode.asText();
    }

    String getDownloadLink(JsonNode node) throws ParsingException {
        JsonNode downloadNode = node.at(DOWNLOAD_LINK_PATH);
        if (downloadNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", DOWNLOAD_LINK_PATH));
        }
        return downloadNode.asText();
    }

    DateTime[] getTimeFrame(JsonNode node) throws ParsingException {
        JsonNode startDateNode = node.at(START_DATE_PATH);
        if (startDateNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", START_DATE_PATH));
        }
        JsonNode endDateNode = node.at(COMPLETION_DATE_PATH);
        if (endDateNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", COMPLETION_DATE_PATH));
        }
        return new DateTime[]{
                DateTime.parse(startDateNode.asText(), FORMATTER),
                DateTime.parse(endDateNode.asText(), FORMATTER)
        };
    }

    Float getCloudCoverage(JsonNode node) {
        JsonNode cloudCoverNode = node.at(CLOUD_COVERAGE_PATH);
        if (cloudCoverNode.isMissingNode() || cloudCoverNode.asText().equals("-1")) {
            return null;
        } else {
            return Float.parseFloat(cloudCoverNode.asText());
        }
    }

    Float[] getBbox(JsonNode node) throws ParsingException {
        JsonNode geomNode = node.at(GEOMETRY_PATH);
        if (geomNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", GEOMETRY_PATH));
        }
        Geometry geom;
        try {
            geom = decoder.decode(geomNode);
        } catch (DecodingException ex) {
            throw new ParsingException("Error while decoding geomerty.", ex);
        }
        return new Float[]{
                ((Double) geom.getEnvelope().getCoordinates()[0].getX()).floatValue(),
                ((Double) geom.getEnvelope().getCoordinates()[0].getY()).floatValue(),
                ((Double) geom.getEnvelope().getCoordinates()[2].getX()).floatValue(),
                ((Double) geom.getEnvelope().getCoordinates()[2].getY()).floatValue()
        };
    }


    public int getNumberOfPages(JsonNode node) throws ParsingException {
        JsonNode idNode = node.at(TOTAL_RESULTS_PATH);
        if (idNode.isMissingNode()) {
            throw new ParsingException(String.format("Missing node: '%s'", TOTAL_RESULTS_PATH));
        }
        return numberOfPagesCalculation(idNode.asInt());
    }

    /**
     * Calculates the number of pages. Each page contains 50 <entry>-Tags
     *
     * @param totalResults number of total <entry>-Tags
     * @return number of pages (int)
     */
    private int numberOfPagesCalculation(int totalResults) {
        int modulo = totalResults % ITEMS_PER_PAGE;
        int nop = (totalResults - modulo) / ITEMS_PER_PAGE;
        if (modulo > 0)
            nop += 1;
        return nop;
    }
}
