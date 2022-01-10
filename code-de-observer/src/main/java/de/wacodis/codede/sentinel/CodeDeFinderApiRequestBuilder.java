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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@Component
public class CodeDeFinderApiRequestBuilder {

    private static final String SERVICE_URL = "https://finder.code-de.org/resto/api/collections/";
    private static final String CONTENT_TYPE_PARAM = "search.json";

    static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String MAXIMUM_RECORDS = "50";

    private static final String PARAM_DELIMITER = "=";
    private static final String COORD_DELIMITER = "+";
    private static final String MAX_RECORD_PARAM = "maxRecords";
    private static final String START_DATE_PARAM = "startDate";
    private static final String COMPLETION_DATE_PARAM = "completionDate";
    private static final String CLOUD_COVER_PARAM = "cloudCover";
    private static final String INSTRUMENT_PARAM = "instrument";
    private static final String PRODUCT_TYPE_PARAM = "productType";
    private static final String PROCESSING_LEVEL_PARAM = "processingLevel";
    private static final String GEOMETRY_PARAM = "geometry";
    private static final String SENSOR_MODE_PARAM = "sensorMode";
    public static final String PAGE_PARAM = "page";

    /**
     * Builds the String containing the URL of the GET request.
     *
     * @param params all necessary parameters for the OpenSearch request
     * @param page   number of page, first request should be 1
     * @return url of the GET request
     */
    public String buildGetRequestUrl(CodeDeRequestParams params, int page) {
        String reqUrl = SERVICE_URL + params.getSatellite() + "/" + CONTENT_TYPE_PARAM + "?"
                + String.join("&",
                getInstrumentParam(params),
                getProductTypeParam(params),
                getProcessingParam(params),
                getStartDateParam(params),
                getCompletionDateParam(params),
                getGeometryParam(params),
                getMaxRecordsParam(),
                getPageParam(page));
        reqUrl = getCloudCoverParam(params).isEmpty() ? reqUrl : reqUrl + "&" + getCloudCoverParam(params);
        reqUrl = getSensorMode(params).isEmpty() ? reqUrl : reqUrl + "&" + getSensorMode(params);
        return reqUrl;
    }

    private String getInstrumentParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER, INSTRUMENT_PARAM, params.getInstrument());
    }

    private String getProductTypeParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER, PRODUCT_TYPE_PARAM, params.getProductType());
    }

    private String getProcessingParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER, PROCESSING_LEVEL_PARAM, params.getProcessingLevel());
    }

    private String getSensorMode(CodeDeRequestParams params) {
        return (params.getSensorMode() == null || params.getSensorMode().isEmpty())
                ? ""
                : String.join(PARAM_DELIMITER, SENSOR_MODE_PARAM, params.getSensorMode());
    }

    private String getStartDateParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER, START_DATE_PARAM, FORMATTER.print(params.getStartDate()));
    }

    private String getCompletionDateParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER, COMPLETION_DATE_PARAM, FORMATTER.print(params.getEndDate()));
    }

    private String getCloudCoverParam(CodeDeRequestParams params) {
        if (params.getCloudCover() == null || params.getCloudCover().length < 2) {
            return "";
        } else {
            return String.join(PARAM_DELIMITER, CLOUD_COVER_PARAM, String.format("[%.0f,%.0f]", params.getCloudCover()));
        }
    }

    private String getGeometryParam(CodeDeRequestParams params) {
        return String.join(PARAM_DELIMITER,
                GEOMETRY_PARAM,
                String.format("POLYGON((%s,%s,%s,%s,%s))",
                        params.getBbox()[0] + COORD_DELIMITER + params.getBbox()[1],
                        params.getBbox()[0] + COORD_DELIMITER + params.getBbox()[3],
                        params.getBbox()[2] + COORD_DELIMITER + params.getBbox()[3],
                        params.getBbox()[2] + COORD_DELIMITER + params.getBbox()[1],
                        params.getBbox()[0] + COORD_DELIMITER + params.getBbox()[1])
        );
    }

    private String getMaxRecordsParam() {
        return String.join(PARAM_DELIMITER, MAX_RECORD_PARAM, MAXIMUM_RECORDS);
    }

    private String getPageParam(int page) {
        return String.join(PARAM_DELIMITER, PAGE_PARAM, String.valueOf(page));
    }
}
