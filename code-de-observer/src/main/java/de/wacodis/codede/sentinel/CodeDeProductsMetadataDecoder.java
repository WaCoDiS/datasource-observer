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

import de.wacodis.observer.model.AbstractDataEnvelope;
import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.model.AbstractDataEnvelopeTimeFrame;
import de.wacodis.observer.model.CopernicusDataEnvelope;

/**
 * Decodes CodeDeProductsMetadata into a CopernicusDataEnvelope
 *
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
public class CodeDeProductsMetadataDecoder {

    /**
     * delivers the content of a CodeDeProductsMetadata-object into a dataEnvelope
     *
     * @param metadata object containing the metadata information of a code de request
     * @return CopernicusDataEnvelope, contains information about the product
     */
    public static CopernicusDataEnvelope decode(CodeDeProductsMetadata metadata) {

        CopernicusDataEnvelope copDE = new CopernicusDataEnvelope();

        if (metadata.getSatellite().equalsIgnoreCase("sentinel1") ||
                metadata.getSatellite().equalsIgnoreCase("sentinel-1")) {
            copDE.setSatellite(CopernicusDataEnvelope.SatelliteEnum._1);
        } else if (metadata.getSatellite().equalsIgnoreCase("sentinel2") ||
                metadata.getSatellite().equalsIgnoreCase("sentinel-2")) {
            copDE.setSatellite(CopernicusDataEnvelope.SatelliteEnum._2);
        } else if (metadata.getSatellite().equalsIgnoreCase("sentinel3") ||
                metadata.getSatellite().equalsIgnoreCase("sentinel-3")) {
            copDE.setSatellite(CopernicusDataEnvelope.SatelliteEnum._3);
        } else {
            copDE.setSatellite(null);
        }

        copDE.setDatasetId(metadata.getDatasetId());
        copDE.setCloudCoverage(metadata.getCloudCover());
        copDE.setPortal(CopernicusDataEnvelope.PortalEnum.CODE_DE);
        copDE.setSourceType(AbstractDataEnvelope.SourceTypeEnum.COPERNICUSDATAENVELOPE);

        AbstractDataEnvelopeAreaOfInterest extent = new AbstractDataEnvelopeAreaOfInterest();
        extent.extent(metadata.getAreaOfInterest());
        copDE.setAreaOfInterest(extent);

        AbstractDataEnvelopeTimeFrame timeframe = new AbstractDataEnvelopeTimeFrame();
        timeframe.setStartTime(metadata.getStartDate());
        timeframe.setEndTime(metadata.getEndDate());
        copDE.setTimeFrame(timeframe);

        return copDE;
    }
}
