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
package de.wacodis.codede;

import de.wacodis.codede.sentinel.*;
import de.wacodis.codede.sentinel.exception.HttpConnectionException;
import de.wacodis.codede.sentinel.exception.ParsingException;
import de.wacodis.observer.core.TemporalCoverageConstants;
import de.wacodis.observer.decode.DecodingException;
import de.wacodis.observer.model.CopernicusDataEnvelope;
import de.wacodis.observer.publisher.PublisherChannel;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class CodeDeJob implements Job {

    // Class variables for Request
    public static final String PARENT_IDENTIFIER_KEY = "parentIdentifier";
    public static final String START_DATE_KEY = "startDate";
    public static final String END_DATE_KEY = "endDate";

    public static final String BBOX_KEY = "bbox";
    public static final String CLOUD_COVER_KEY = "cloudCover";

    //Class variables for Job
    public static final String TEMPORAL_COVERAGE_KEY = "temporalCoverage";
    public static final String SATTELITE_KEY = "satellite";
    public static final String LATEST_REQUEST_END_DATE = "endDate";
    public static final String MAXIMUM_RECORDS_KEY = "maximumRecords";
    public static final String RECORD_SCHEMA_KEY = "recordSchema";
    public static final String START_PAGE_KEY = "startPage";
    public static final String INSTRUMENT_KEY = "instrument";
    public static final String PRODUCT_TYPE_KEY = "productType";
    public static final String PROCESSING_LEVEL_KEY = "processingLevel";
    public static final String SENSOR_MODE_KEY = "sensorMode";

    public static final String EXECUTION_INTERVAL_KEY = "executionInterval";


    private static final Logger LOG = LoggerFactory.getLogger(CodeDeJob.class);

    @Autowired
    private PublisherChannel pub;

    @Autowired
    private CodeDeRequestor requestor;

    /**
     * executes the job
     *
     * @param context all necessary parameters for a request
     */
    @Override
    public void execute(JobExecutionContext context) {
        LOG.debug("Start CodeDeJob's execute()");
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        DateTime endDate;
        DateTime startDate;        

        // If there was a Job execution before, consider the latest request
        // end date as start date for the current request.
        // Else, use the factory level based generic configuration of the first 
        if (dataMap.get(LATEST_REQUEST_END_DATE) != null) {
        	startDate = (DateTime) dataMap.get(LATEST_REQUEST_END_DATE);
        	endDate = DateTime.now();
        }
            
        else {
        	startDate = DateTime.parse((String)dataMap.get(TemporalCoverageConstants.START_DATE));
        	endDate = DateTime.parse((String)dataMap.get(TemporalCoverageConstants.END_DATE));
        }
        dataMap.put(LATEST_REQUEST_END_DATE, endDate);

        CodeDeRequestParamsEncoder encoder = new CodeDeRequestParamsEncoder();
        CodeDeRequestParams params = encoder.encode(dataMap, startDate, endDate);

        List<CopernicusDataEnvelope> dataEnvelopes = requestCodeDE(params);
        LOG.info("Found {} datasets of type: {}", dataEnvelopes.size(), CopernicusDataEnvelope.class.getSimpleName());
        this.publishDataEnvelopes(dataEnvelopes);
    }

    /**
     * Publish the {@link CopernicusDataEnvelope} for a set of request parameters
     *
     * @param dataEnvelopes list of {@link CopernicusDataEnvelope} objects that will be published
     */
    private void publishDataEnvelopes(List<CopernicusDataEnvelope> dataEnvelopes) {

        for (CopernicusDataEnvelope copDE : dataEnvelopes) {
            pub.sendDataEnvelope().send(MessageBuilder.withPayload(copDE).build());
            LOG.info("Published new CodeDeDataEnvelope for Copernicus dataset: {}", copDE.getDatasetId());
            LOG.debug("Published CodeDeDataEnvelope: {}", copDE);
        }

    }

    /**
     * Request CODE-DE metadata and create a {@link CopernicusDataEnvelope}
     *
     * @param params parameters that will be used for requesting the CODE-DE Finder API
     * @return all requested dataEnvelopes
     */
    private List<CopernicusDataEnvelope> requestCodeDE(CodeDeRequestParams params) {
        ArrayList<CopernicusDataEnvelope> dataEnvelopes = new ArrayList<>();
        // Request CodeDe Opensearch API with request paramaters
        List<CodeDeProductsMetadata> metadata = new ArrayList<>();
        try {
            metadata = requestor.request(params);
        } catch (ParsingException ex) {
            LOG.error("Error while parsing CODE-DE response: {}", ex.getMessage());
            LOG.debug("Decoding error cause: ", ex);
        } catch (HttpConnectionException ex) {
            LOG.error("Error while requesting CODE-DE: {}", ex.getMessage());
            LOG.debug("Requesting error cause: ", ex);
        }
        // Decode CodeDeProductsMetadata to CopernicusDataEnvelope
        for (CodeDeProductsMetadata object : metadata) {
            CopernicusDataEnvelope dataEnvelope = CodeDeProductsMetadataDecoder.decode(object);
            dataEnvelopes.add(dataEnvelope);
        }
        return dataEnvelopes;
    }
}
