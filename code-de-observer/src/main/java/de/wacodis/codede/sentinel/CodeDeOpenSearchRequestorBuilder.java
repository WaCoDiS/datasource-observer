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

import de.wacodis.codede.CodeDeJob;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Helps building the CODE-DE OpenSeaurch request for certain request parameters.
 *
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
@Deprecated
public class CodeDeOpenSearchRequestorBuilder {

    public static final String SERVICE_URL = "https://catalog.code-de.org/opensearch/request?";
    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final String MAXIMUM_RECORDS= "50";
    public static final String RECORD_SCHEMA = "om";

    /**
     * Builds the String containing the URL of the GET request.
     * @param params all necessary parameters for the OpenSearch request
     * @param page number of page, first request should be 1
     * @return url of the GET request
     */
    public static String buildGetRequestUrl(CodeDeRequestParams params, int page){
        // parentIdentifier
        String parentIdentifier = CodeDeJob.PARENT_IDENTIFIER_KEY + "=" +  params.getParentIdentifier();
        // dates
        String startDate = CodeDeJob.START_DATE_KEY + "=" + params.getStartDate().toString(FORMATTER);
        String endDate = CodeDeJob.END_DATE_KEY + "=" + params.getEndDate().toString(FORMATTER);
        // bbox
        String bboxContent = params.getBbox().toString();
        bboxContent = bboxContent.replace("[", "");
        bboxContent =  bboxContent.replace("]", "");
        bboxContent = bboxContent.replace(" ", "");
        String bbox = CodeDeJob.BBOX_KEY + "=" + bboxContent;
        // cloud coverage
        String cloudCoverContent = params.getCloudCover().toString();
        cloudCoverContent = cloudCoverContent.replace(" ", "");
        String cloudCover = CodeDeJob.CLOUD_COVER_KEY + "=" + cloudCoverContent;

        // maximum records
        String maxRecords = CodeDeJob.MAXIMUM_RECORDS_KEY + "=" + MAXIMUM_RECORDS;
        // record schema
        String recordSchema = CodeDeJob.RECORD_SCHEMA_KEY + "=" + RECORD_SCHEMA;
        // start page
        String startPage = CodeDeJob.START_PAGE_KEY + "=" + page;

        // put together
        return SERVICE_URL
                //+ HTTP_ACCEPT + "&"
                + parentIdentifier + "&"
                + startDate + "&"
                + endDate + "&"
                + bbox + "&"
                + cloudCover + "&"
                + maxRecords + "&"
                + recordSchema + "&"
                + startPage;
    }

}
