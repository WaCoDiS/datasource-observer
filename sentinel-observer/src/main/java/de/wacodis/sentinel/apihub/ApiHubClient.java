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
package de.wacodis.sentinel.apihub;

import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author matthes rieke
 */
@Component
public class ApiHubClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApiHubClient.class);

    @Autowired
    private RestTemplate apiHubRestTemplate;
    
    @Value("${datasource-observer.sentinelhub.page-rows:10}")
    private int rows;
    
    private QueryBuilder queryBuilder = new QueryBuilder();
    
    /**
     * perform an API query with the given parameters
     * 
     * @param lastLatestDate the latest timestamp of an already retrieved product. can be null
     * @param maxCloudPercentage the maximum cloud coverage (considered if > 0.0)
     * @param platformName the platform name (e.g. Sentinel-2)
     * @param areaOfInterest the are of interest
     * @return the list of matching products
     */
    public List<ProductMetadata> requestProducts(DateTime lastLatestDate, Double maxCloudPercentage,
            String platformName, AbstractDataEnvelopeAreaOfInterest areaOfInterest) {
        QueryBuilder qb = queryBuilder.withBeginPosition(lastLatestDate, DateTime.now())
                .withPlatformName(platformName)
                .withMaximumCloudcoverPercentage(maxCloudPercentage.intValue());
        
        if (areaOfInterest != null) {
            List<Float> ex = areaOfInterest.getExtent();
            qb.withFootprint(QueryBuilder.FootprintOperator.Intersects, ex.get(0), ex.get(1), ex.get(2), ex.get(3));
        }
        
        String q = qb.build();
        
        // first request to identify the number of pages
        ResponseEntity<SearchResult> sr1Entity = this.apiHubRestTemplate.getForEntity("/search?rows=" + this.rows
                + "&q=" + q,
                SearchResult.class);

        SearchResult sr1 = sr1Entity.getBody();        
        if (sr1 == null || sr1Entity.getStatusCodeValue() >= HttpStatus.MULTIPLE_CHOICES.value()) {
            LOG.warn("Could not retrieve OpenSearch response: {}", sr1Entity.getStatusCode());
            return Collections.emptyList();
        }
        
        List<ProductMetadata> allProducts = new ArrayList<>(sr1.getProducts());
        
        /** page through the remaining products */
        int totalPages = sr1.getTotalResults() / sr1.getItemsPerPage();
        for (int i = 1; i < totalPages; i++) {
            ResponseEntity<SearchResult> nextPage = this.apiHubRestTemplate.getForEntity("/search?rows=" + this.rows
                + "&start=" + (i * this.rows)
                + "&q=" + q,
                SearchResult.class);
            
            SearchResult nextPageResult = nextPage.getBody();        
            
            if (nextPageResult == null || nextPage.getStatusCodeValue() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                LOG.warn("Could not retrieve page {} of OpenSearch query: {}", i, sr1Entity.getStatusCode());
            } else {
                allProducts.addAll(nextPageResult.getProducts());
            }
            
        }
        
        return allProducts;
    }
    
}
