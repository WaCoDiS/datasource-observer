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
import org.joda.time.DateTime;
import org.quartz.JobDataMap;

import java.util.List;

/**
 * Encodes a SubsetDefinition into request paramateres for a Code De Opensearch request
 *
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 * @author <a href="mailto:sebastian.drost@hs-bochum.de">Sebastian Drost</a>
 */

public class CodeDeRequestParamsEncoder {

    /**
     * Puts all necessary attributes of a subsetdefinition into a requestParams object
     *
     * @param dataMap a {@link JobDataMap} that holds all request parameter values
     * @return object containing all parameters
     */
    public CodeDeRequestParams encode(JobDataMap dataMap, DateTime startDate, DateTime endDate) {

        String satellite = dataMap.getString(CodeDeJob.SATTELITE_KEY);
        String instrument = dataMap.getString(CodeDeJob.INSTRUMENT_KEY);
        String productType = dataMap.getString(CodeDeJob.PRODUCT_TYPE_KEY);
        String processingLevel = dataMap.getString(CodeDeJob.PROCESSING_LEVEL_KEY);

        String[] executionAreaJSON = dataMap.getString(CodeDeJob.BBOX_KEY).split(",");
        Float[] area = new Float[]{
                Float.parseFloat(executionAreaJSON[0].split(" ")[0]),
                Float.parseFloat(executionAreaJSON[0].split(" ")[1]),
                Float.parseFloat(executionAreaJSON[1].split(" ")[0]),
                Float.parseFloat(executionAreaJSON[1].split(" ")[1])
        };

        Float[] cloudCover = null;
        if (satellite.equals("Sentinel2")) {
            cloudCover = new Float[]{0.f, dataMap.getFloat(CodeDeJob.CLOUD_COVER_KEY)};
        }

        String sensorMode = null;
        if (satellite.equals("Sentinel1")) {
            sensorMode = dataMap.getString(CodeDeJob.SENSOR_MODE_KEY);
        }

        return new CodeDeRequestParams(satellite, instrument, productType, processingLevel, startDate, endDate, area, sensorMode, cloudCover);
    }
}
