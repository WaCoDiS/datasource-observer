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
package de.wacodis.sentinel;

import de.wacodis.observer.core.TemporalCoverageConstants;
import de.wacodis.observer.model.AbstractDataEnvelope;
import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.model.AbstractDataEnvelopeTimeFrame;
import de.wacodis.observer.model.CopernicusDataEnvelope;
import de.wacodis.observer.publisher.PublisherChannel;
import de.wacodis.sentinel.apihub.ApiHubClient;
import de.wacodis.sentinel.apihub.GeojsonHelper;
import de.wacodis.sentinel.apihub.ProductMetadata;
import de.wacodis.sentinel.apihub.QueryBuilder;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.io.gml2.GMLReader;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author matthes rieke
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SentinelJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(SentinelJob.class);

    public static String MAX_CLOUD_COVERAGE_KEY = "maximumCloudCoverage";
    public static String PLATFORM_KEY = "platformName";
    public static String PREVIOUS_DAYS_KEY = "previousDays";
    public static String LAST_LATEST_PRODUCT_KEY = "lastLatestProduct";
    public static String LAST_EXECUTION_PRODUCT_IDS_KEY = "lastExecutionProductIds";

    private GeometryFactory factory;
    private GMLReader gmlReader;
    private GeoJsonWriter geojsonWriter;

    @Autowired
    private ApiHubClient hubClient;

    @Autowired
    private PublisherChannel publisher;

    @Autowired
    private GeojsonHelper geojsonHelper;

    @Override
    public void execute(JobExecutionContext ctxt) throws JobExecutionException {
        JobDataMap dataMap = ctxt.getJobDetail().getJobDataMap();      

        DateTime endDate;
        DateTime startDate;        

        // If there was a Job execution before, consider the latest request
        // end date as start date for the current request.
        // Else, use the factory level based generic configuration of the first 
        if (dataMap.get(LAST_LATEST_PRODUCT_KEY) != null) {
        	startDate = DateTime.parse((String)dataMap.get(LAST_LATEST_PRODUCT_KEY));
        	endDate = DateTime.now();
        }
            
        else {
        	startDate = DateTime.parse((String)dataMap.get(TemporalCoverageConstants.START_DATE));
        	endDate = DateTime.parse((String)dataMap.get(TemporalCoverageConstants.END_DATE));
        }
        dataMap.put(LAST_LATEST_PRODUCT_KEY, endDate.toString());

        /*
         * defined on the job level
         */
        Object maxCloud = dataMap.get(MAX_CLOUD_COVERAGE_KEY);
        double maxCloudPercentage = 0.0;
        if (maxCloud instanceof Number) {
            maxCloudPercentage = ((Number) maxCloud).doubleValue();
        }

        /*
         * defined on the job level
         */
        Object platform = dataMap.get(PLATFORM_KEY);
        String platformName = null;
        if (platform instanceof String) {
            platformName = (String) platform;
        } else if (platform instanceof QueryBuilder.PlatformName) {
            platformName = platform.toString();
        }

        if (platform == null) {
            LOG.warn("Skipping the job, as the platform was not defined: {}", dataMap);
            return;
        }

        /*
         * defined on the job level *
         */
        Object aoi = dataMap.get("areaOfInterest");
        AbstractDataEnvelopeAreaOfInterest areaOfInterest = null;
        if (aoi instanceof AbstractDataEnvelopeAreaOfInterest) {
            areaOfInterest = (AbstractDataEnvelopeAreaOfInterest) aoi;
        }

        /*
         * retrieve products from the API
         */
        List<ProductMetadata> newProductCandidates = hubClient
                .requestProducts(startDate.minusHours(12),
                        maxCloudPercentage,
                        platformName,
                        areaOfInterest);

        /*
         * filter out duplicates from previous executions *
         */
        Object lastIds = dataMap.get(LAST_EXECUTION_PRODUCT_IDS_KEY);
        List<ProductMetadata> newProducts;
        if (lastIds instanceof Set) {
            Set<String> lastIdList = (Set<String>) lastIds;
            newProducts = newProductCandidates.stream()
                    .filter(p -> !lastIdList.contains(p.getId()))
                    .collect(Collectors.toList());
        } else {
            newProducts = newProductCandidates;
        }

        /*
         * sort by beginPosition so we can uses this in upcoming executions
         */
        newProducts.sort((ProductMetadata pm1, ProductMetadata pm2) -> {
            return pm1.getBeginPosition().isBefore(pm2.getBeginPosition()) ? 1 : -1;
        });

        if (!newProducts.isEmpty()) {
            /*
             * store the last begin position
             */
            dataMap.put(LAST_LATEST_PRODUCT_KEY, newProducts.get(newProducts.size() - 1).getBeginPosition().toString());

            /*
             * store the last IDs in the execution environment
             */
            dataMap.put(LAST_EXECUTION_PRODUCT_IDS_KEY, newProducts.stream()
                    .map(p -> p.getId())
                    .distinct()
                    .collect(Collectors.toSet()));
        }

        LOG.info("Found {} new products for Job {}", newProducts.size(), ctxt.getJobDetail().getKey());

        final CopernicusDataEnvelope.SatelliteEnum satellite = CopernicusDataEnvelope.SatelliteEnum.fromValue(platformName.toLowerCase());
        newProducts.stream().forEach(p -> {
            CopernicusDataEnvelope env = prepareEnvelop(p);
            env.setPortal(CopernicusDataEnvelope.PortalEnum.SENTINEL_HUB);

            publish(env);
        });
    }

    private CopernicusDataEnvelope prepareEnvelop(ProductMetadata metadata) {
        //Build new SensorWebDataEnvelope
        CopernicusDataEnvelope dataEnvelope = new CopernicusDataEnvelope();

        dataEnvelope.setSourceType(AbstractDataEnvelope.SourceTypeEnum.COPERNICUSDATAENVELOPE);
        dataEnvelope.setCloudCoverage((float) metadata.getCloudCoverPercentage());
        dataEnvelope.setAreaOfInterest(createAreaOfInterest(metadata.resolveBbox()));
        dataEnvelope.setDatasetId(metadata.getId());

        CopernicusDataEnvelope.SatelliteEnum satellite = resolveSatellite(metadata);
        dataEnvelope.setSatellite(satellite);
        dataEnvelope.setInstrument(metadata.getInstrumentShortName());
        dataEnvelope.sensorMode(metadata.getSensorMode());
        dataEnvelope.setProductLevel(resolveProcessingLevel(metadata));
        dataEnvelope.setProductType(resolveProductType(metadata, satellite));
        dataEnvelope.setFootprint(resolveGmlFootprintAsGeojson(metadata));

        dataEnvelope.setModified(new DateTime());

        AbstractDataEnvelopeTimeFrame timeFrame = new AbstractDataEnvelopeTimeFrame();
        timeFrame.setStartTime(metadata.getBeginPosition());
        timeFrame.setEndTime(metadata.getEndPosition());
        dataEnvelope.setTimeFrame(timeFrame);

        return dataEnvelope;
    }

    @Deprecated
    private CopernicusDataEnvelope prepareEnvelop(double cloudCover, Envelope aoi, String gmlFootprint, String id,
                                                  CopernicusDataEnvelope.SatelliteEnum platformName, DateTime firstProduct, DateTime lastProduct) {
        //Build new SensorWebDataEnvelope
        CopernicusDataEnvelope dataEnvelope = new CopernicusDataEnvelope();

        dataEnvelope.setSourceType(AbstractDataEnvelope.SourceTypeEnum.COPERNICUSDATAENVELOPE);
        dataEnvelope.setCloudCoverage((float) cloudCover);
        dataEnvelope.setAreaOfInterest(createAreaOfInterest(aoi));
        dataEnvelope.setSatellite(platformName);
        dataEnvelope.setDatasetId(id);


        dataEnvelope.setModified(new DateTime());

        AbstractDataEnvelopeTimeFrame timeFrame = new AbstractDataEnvelopeTimeFrame();
        timeFrame.setStartTime(firstProduct);
        timeFrame.setEndTime(lastProduct);
        dataEnvelope.setTimeFrame(timeFrame);

        return dataEnvelope;
    }

    private CopernicusDataEnvelope.SatelliteEnum resolveSatellite(ProductMetadata metadata) {
        return CopernicusDataEnvelope.SatelliteEnum.fromValue(metadata.getPlatformName().toLowerCase());
    }

    private String resolveProcessingLevel(ProductMetadata metadata) {
        // This is the case for Sentinel-1 datasets
        if (metadata.getProcessingLevel() == null) {
            return null;
            // For Sentinel-2 do some String processing to meet the expected values
            // (LEVEL1C and LEVEL2A instead of Level-1C and Level-2A)
        } else {
            return metadata.getProcessingLevel().replace("-", "").toUpperCase();
        }
    }

    private String resolveProductType(ProductMetadata metadata, CopernicusDataEnvelope.SatelliteEnum satellite) {
        // For Sentinel-2 resolve the product type from processing level
        // to meet the expected values
        switch (satellite) {
            case _2: {
                if (metadata.getProcessingLevel().equals("Level-2A"))
                    return "L2A";
                else if (metadata.getProcessingLevel().equals("Level-1C"))
                    return "L1C";
                else
                    return null;

            }
            // For all other datasets assume the originally product type
            default:
                return metadata.getProductType();
        }
    }

    private String resolveGmlFootprintAsGeojson(ProductMetadata metadata) {
        if (metadata.getGmlFootprint() == null || metadata.getGmlFootprint().isEmpty()) {
            return null;
        } else {
            return geojsonHelper.decodeGml(metadata.getGmlFootprint());
        }
    }

    private void publish(CopernicusDataEnvelope data) {
        publisher.sendDataEnvelope().send(MessageBuilder.withPayload(data).build());
        LOG.info("Published new CopernicusDataEnvelope for Copernicus dataset: {}", data.getDatasetId());
        LOG.debug("Published CopernicusDataEnvelope: {}", data);
    }

    private AbstractDataEnvelopeAreaOfInterest createAreaOfInterest(Envelope aoi) {
        if (aoi == null) {
            return null;
        }

        AbstractDataEnvelopeAreaOfInterest result = new AbstractDataEnvelopeAreaOfInterest();
        List<Float> asList = Arrays.asList((float) aoi.getMinX(), (float) aoi.getMinY(),
                (float) aoi.getMaxX(), (float) aoi.getMaxY());
        result.setExtent(asList);
        return result;
    }
}
