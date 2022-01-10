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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.wacodis.dwd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.xml.sax.SAXException;

import de.wacodis.dwd.cdc.model.DwdProductsMetadata;
import de.wacodis.dwd.cdc.model.DwdProductsMetadataDecoder;
import de.wacodis.dwd.cdc.model.DwdRequestParamsEncoder;
import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import de.wacodis.dwd.cdc.DwdWfsRequestor;
import de.wacodis.observer.core.TemporalCoverageConstants;
import de.wacodis.observer.model.DwdDataEnvelope;
import de.wacodis.observer.publisher.PublisherChannel;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DwdJob implements Job {

    // identifiers
    public static final String VERSION_KEY = "version";
    public static final String LAYER_NAME_KEY = "layerName";
    public static final String SERVICE_URL_KEY = "serviceUrl";
    public static final String TEMPORAL_COVERAGE_KEY = "temporalCoverage";
    public static final String EXECUTION_INTERVAL_KEY = "executionInterval";
    public static final String EXECUTION_AREA_KEY = "executionArea";
    public static final String LATEST_REQUEST_END_DATE = "endDate";

    private static final Logger LOG = LoggerFactory.getLogger(DwdJob.class);

    @Autowired
    private PublisherChannel pub;

    @Autowired
    private DwdWfsRequestor requestor;

    @Autowired
    private DwdTemporalResolutionHelper temporalResolutionHelper;

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException{
        LOG.debug("Start DwdJob's execute()");
        JobDataMap dataMap = jec.getJobDetail().getJobDataMap();

        // 1) Get all required request parameters stored in the JobDataMap
        String version = dataMap.getString(VERSION_KEY);
        String layerName = dataMap.getString(LAYER_NAME_KEY);
        String serviceUrl = dataMap.getString(SERVICE_URL_KEY);
        String durationISO = dataMap.getString(TEMPORAL_COVERAGE_KEY); // previous days unnecessary?
        String[] executionAreaJSON = dataMap.getString(EXECUTION_AREA_KEY).split(",");

        // parse executionAreaJSON into Float list
        String bottomLeftYStr = executionAreaJSON[0].split(" ")[0];
        String bottomLeftXStr = executionAreaJSON[0].split(" ")[1];
        String upperRightYStr = executionAreaJSON[1].split(" ")[0];
        String upperRightXStr = executionAreaJSON[1].split(" ")[1];

        float bottomLeftY = Float.parseFloat(bottomLeftYStr);
        float bottomLeftX = Float.parseFloat(bottomLeftXStr);
        float upperRightY = Float.parseFloat(upperRightYStr);
        float upperRightX = Float.parseFloat(upperRightXStr);
        ArrayList<Float> area = new ArrayList<>();
        area.add(0, bottomLeftY);
        area.add(1, bottomLeftX);
        area.add(2, upperRightY);
        area.add(3, upperRightX);

        Period period = Period.parse(durationISO, ISOPeriodFormat.standard());

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

        this.createDwdDataEnvelope(version, layerName, serviceUrl, area, startDate, endDate);
    }

    /**
     * Create and publish the {@link DwdDataEnvelope} for a set of request parameters
     *
     * @param version    version number of WFS - usually 2.0.0
     * @param layerName  short designation of layer
     * @param serviceUrl general url of webservice
     * @param area       bbox (minLon, minLat, maxLon, maxLat)
     * @param startDate  start date of the request timeframe
     * @param endDate    end date of the request timeframe
     */
    private void createDwdDataEnvelope(String version, String layerName, String serviceUrl,
                                       ArrayList<Float> area, DateTime startDate, DateTime endDate) {
        DwdRequestParamsEncoder encoder = new DwdRequestParamsEncoder();

        List<DateTime[]> interval = temporalResolutionHelper.getRequestIntervals(startDate, endDate, layerName);

        // Start requesting DWD data iteratively if the amount of data is too large
        if (interval != null) {
            for (DateTime[] dateTimes : interval) {
                DwdWfsRequestParams params = encoder.encode(version, layerName, area, dateTimes[0], dateTimes[1]);
                DwdDataEnvelope dataEnvelope = this.requestDwdMetadata(serviceUrl, params);
                if (dataEnvelope != null) {
                    // Publish DwdDataEnvelope message
                    pub.sendDataEnvelope().send(MessageBuilder.withPayload(dataEnvelope).build());
                    LOG.info("Successfully created and published new DwdDataEnvelope: {}", dataEnvelope);
                } else {
                    LOG.warn("Failed creation and publishing of new DwdDataEnvelope for request params: {}", params);
                }
            }
        }
    }

    /**
     * Request DWD metadata from an WFS and create a {@link DwdDataEnvelope}
     *
     * @param serviceUrl the DWD WFS service URL
     * @param params     parameters to use for DWD WFS request
     * @return new found DWD datasets as {@link DwdDataEnvelope}
     */
    private DwdDataEnvelope requestDwdMetadata(String serviceUrl, DwdWfsRequestParams params) {
        DwdProductsMetadataDecoder decoder = new DwdProductsMetadataDecoder();
        DwdDataEnvelope dataEnvelope = null;
        try {
            // Request DWD WFS with request paramaters
            DwdProductsMetadata metadata = requestor.request(serviceUrl, params);
            if (metadata == null) {
                LOG.info("No dataEnvelope to publish from DWD WFS response");
                return null;
            }

            // Decode DwdProductsMetadata to DwdDataEnvelope
            dataEnvelope = decoder.decode(metadata);
            LOG.debug("Publish new dataEnvelope:\n{}", dataEnvelope.toString());

        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOG.error(e.getMessage());
            LOG.debug("Error while performing DWD WFS request for URL " + serviceUrl + " with parameters: " + params, e);
        }
        return dataEnvelope;
    }

}
