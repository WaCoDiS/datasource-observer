/*
 * Copyright 2019 WaCoDiS Contributors
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

import de.wacodis.sentinel.apihub.ApiHubClient;
import de.wacodis.api.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.api.model.CopernicusSubsetDefinition;
import de.wacodis.sentinel.apihub.ProductMetadata;
import de.wacodis.sentinel.apihub.QueryBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
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
    
    @Autowired
    private ApiHubClient hubClient;
    
    @Override
    public void execute(JobExecutionContext ctxt) throws JobExecutionException {
        JobDataMap dataMap = ctxt.getJobDetail().getJobDataMap();
        
        /** possibly stored in previous execution */
        Object lastLatest = dataMap.get(LAST_LATEST_PRODUCT_KEY);
        DateTime targettedStartDate = null;
        if (lastLatest != null && lastLatest instanceof DateTime) {
            // also cover the previous 12h to consider late/async arrival of products
            targettedStartDate = ((DateTime) lastLatest).minusHours(12);
        } else {
            /** TODO: temporal coverage not yet clear in business logic */
            Object previousDaysCandidate = dataMap.get(PREVIOUS_DAYS_KEY);
            if (previousDaysCandidate != null && previousDaysCandidate instanceof Integer) {
                int previousDays = (int) previousDaysCandidate;
                targettedStartDate = DateTime.now().minusDays(previousDays);
            } else {
                // lets default to one week
                targettedStartDate = DateTime.now().minusDays(7);
            }
        }
        
        /** defined on the job level */
        Object maxCloud = dataMap.get(MAX_CLOUD_COVERAGE_KEY);
        double maxCloudPercentage = 0.0;
        if (maxCloud != null && maxCloud instanceof Number) {
            maxCloudPercentage = ((Number) maxCloud).doubleValue();
        }
        
        /** defined on the job level */
        Object platform = dataMap.get(PLATFORM_KEY);
        String platformName = null;
        if (platform != null && platform instanceof String) {
            platformName = (String) platform;
        } else if (platform != null && platform instanceof QueryBuilder.PlatformName) {
            platformName = ((QueryBuilder.PlatformName) platform).toString();
        }
        
        
        /** defined on the job level **/
        Object aoi = dataMap.get("areaOfInterest");
        AbstractDataEnvelopeAreaOfInterest areaOfInterest = null;
        if (aoi != null && aoi instanceof AbstractDataEnvelopeAreaOfInterest) {
            areaOfInterest = (AbstractDataEnvelopeAreaOfInterest) aoi;
        }
        
        /**
         * retrieve products from the API
         */
        List<ProductMetadata> newProductCandidates = hubClient
                .requestProducts(targettedStartDate.minusHours(12),
                        maxCloudPercentage,
                        platformName,
                        areaOfInterest);
        
        /** filter out duplicates from previous executions **/
        Object lastIds = dataMap.get(LAST_EXECUTION_PRODUCT_IDS_KEY);
        List<ProductMetadata> newProducts;
        if (lastIds != null && lastIds instanceof Set) {
            Set<String> lastIdList = (Set<String>) lastIds;
            newProducts = newProductCandidates.stream()
                    .filter(p -> !lastIdList.contains(p.getId()))
                    .collect(Collectors.toList());
        } else {
            newProducts = newProductCandidates;
        }
        
        /** sort by beginPosition so we can uses this in upcoming executions */
        newProducts.sort((ProductMetadata pm1, ProductMetadata pm2) -> {
            return pm1.getBeginPosition().isBefore(pm2.getBeginPosition()) ? 1 : -1;
        });
        
        if (!newProducts.isEmpty()) {
            /** store the last begin position */
            dataMap.put(LAST_LATEST_PRODUCT_KEY, newProducts.get(newProducts.size() - 1).getBeginPosition());

            /** store the last IDs in the execution environment */
            dataMap.put(LAST_EXECUTION_PRODUCT_IDS_KEY, newProducts.stream()
                    .map(p -> p.getId())
                    .distinct()
                    .collect(Collectors.toSet()));            
        }
        
        LOG.info("Found {} new products for Job {}", newProducts.size(), "");
        
    }
    
}
