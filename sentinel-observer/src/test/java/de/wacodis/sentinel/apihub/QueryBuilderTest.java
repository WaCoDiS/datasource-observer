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

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author matthes rieke
 */
public class QueryBuilderTest {
    
    @Test
    public void testBuildingFull() {
        QueryBuilder builder = new QueryBuilder();
        builder.withFootprint(QueryBuilder.FootprintOperator.Intersects, 6.9315, 50.9854, 7.6071, 51.3190);
        builder.withProductType(QueryBuilder.ProductType.SLC);
        builder.withFilename("S2A_*").withMaximumCloudcoverPercentage(10).withPlatformName(QueryBuilder.PlatformName.Sentinel2);
        builder.withIngestionDateByPreviousDays(2).withBeginPosition(new DateTime("2019-01-01"), new DateTime("2019-02-01"));
        
        String expected = "ingestiondate:[NOW-2DAYS TO NOW] AND "
                + "producttype:SLC AND "
                + "filename:S2A_* AND "
                + "cloudcoverpercentage:[0 TO 10] AND "
                + "platformname:Sentinel-2 AND "
                + "beginposition:[2019-01-01T00:00:00Z TO 2019-02-01T00:00:00Z] AND "
                + "footprint:\"Intersects(POLYGON((7.6071 50.9854,6.9315 50.9854,6.9315 51.3190,7.6071 51.3190,7.6071 50.9854)))\"";
        
        Assert.assertThat(builder.build(), CoreMatchers.equalTo(expected));
    }
    
    @Test
    public void testBuildingPartial() {
        QueryBuilder builder = new QueryBuilder()
                .withFootprint(QueryBuilder.FootprintOperator.Intersects, 6.9315, 50.9854, 7.6071, 51.3190)
                .withPlatformName(QueryBuilder.PlatformName.Sentinel2)
                .withBeginPosition(new DateTime("2019-01-01"), new DateTime("2019-02-01"));
        
        String expected = "platformname:Sentinel-2 AND "
                + "beginposition:[2019-01-01T00:00:00Z TO 2019-02-01T00:00:00Z] AND "
                + "footprint:\"Intersects(POLYGON((7.6071 50.9854,6.9315 50.9854,6.9315 51.3190,7.6071 51.3190,7.6071 50.9854)))\"";
        
        Assert.assertThat(builder.build(), CoreMatchers.equalTo(expected));
    }
    
}
