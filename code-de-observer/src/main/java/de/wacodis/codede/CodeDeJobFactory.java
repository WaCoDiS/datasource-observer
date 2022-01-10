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

import de.wacodis.observer.config.ExecutionIntervalConfig;
import de.wacodis.observer.core.JobFactory;
import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.CopernicusSubsetDefinition;
import de.wacodis.observer.model.WacodisJobDefinition;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
@Component
@ConditionalOnProperty(value = "datasource-observer.code-de.enabled", havingValue = "true")
public class CodeDeJobFactory implements JobFactory {
    private final static String PRODUCT_IDENTIFIER_PREFIX = "EOP:CODE-DE:";
    private final static Map<String, String> SATELLITE_MAPPING;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("sentinel-1", "Sentinel1");
        map.put("sentinel-2", "Sentinel2");
        map.put("sentinel-3", "Sentinel3");
        SATELLITE_MAPPING = Collections.unmodifiableMap(map);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CodeDeJobFactory.class);

    @Autowired
    private ExecutionIntervalConfig intervalConfig;

    @Override
    public boolean supportsJobDefinition(WacodisJobDefinition job) {
        Optional<AbstractSubsetDefinition> def = job.getInputs().stream()
                .filter(in -> in instanceof CopernicusSubsetDefinition).findAny();

        return def.isPresent();
    }

    @Override
    public JobBuilder initializeJobBuilder(WacodisJobDefinition job, JobDataMap data, AbstractSubsetDefinition subsetDefinition) {
        // this should always be the case
        if (subsetDefinition instanceof CopernicusSubsetDefinition) {
            LOG.info("Preparing CodeDeJob JobDetail");

            CopernicusSubsetDefinition def = (CopernicusSubsetDefinition) subsetDefinition;

            // Put all required request parameters into JobDataMap
            data.put(CodeDeJob.SATTELITE_KEY, SATELLITE_MAPPING.get(def.getSatellite().toString()));
            data.put(CodeDeJob.INSTRUMENT_KEY, def.getInstrument());
            data.put(CodeDeJob.PRODUCT_TYPE_KEY, def.getProductType());
            data.put(CodeDeJob.PROCESSING_LEVEL_KEY, def.getProductLevel());
            data.put(CodeDeJob.TEMPORAL_COVERAGE_KEY, job.getTemporalCoverage().getDuration());
            if (def.getSensorMode() != null && !def.getSensorMode().isEmpty()) {
                data.put(CodeDeJob.SENSOR_MODE_KEY, def.getSensorMode());
            }
            if (def.getMaximumCloudCoverage() != null) {
                data.put(CodeDeJob.CLOUD_COVER_KEY, def.getMaximumCloudCoverage());
            }

            data.put(CodeDeJob.LATEST_REQUEST_END_DATE, null);
            String extent = job.getAreaOfInterest().getExtent().get(0) + " "
                    + job.getAreaOfInterest().getExtent().get(1) + ","
                    + job.getAreaOfInterest().getExtent().get(2) + " "
                    + job.getAreaOfInterest().getExtent().get(3);
            data.put(CodeDeJob.BBOX_KEY, extent);    // e.g. "52.0478 6.0124,52.5687 7.1420"
            data.put(CodeDeJob.EXECUTION_INTERVAL_KEY, intervalConfig.getSentinel());
        }

        // create the quartz object
        return JobBuilder.newJob(CodeDeJob.class)
                .usingJobData(data);
    }

    @Override
    public Stream<AbstractSubsetDefinition> filterJobInputs(WacodisJobDefinition job) {
        return job.getInputs().stream()
                .filter((i -> i instanceof CopernicusSubsetDefinition));
    }

    @Override
    public String generateSubsetSpecificIdentifier(AbstractSubsetDefinition subsetDefinition) {
        StringBuilder builder = new StringBuilder();

        if (subsetDefinition instanceof CopernicusSubsetDefinition) {
            CopernicusSubsetDefinition copDef = (CopernicusSubsetDefinition) subsetDefinition;
            builder.append(copDef.getSourceType());

            if (copDef.getSatellite() != null) {
                builder.append("_")
                        .append(copDef.getSatellite());
            }
        }

        return builder.toString();
    }

    @Deprecated
    protected String buildProductIdentifier(CopernicusSubsetDefinition def) {
        String satelliteAbr = SATELLITE_MAPPING.get(def.getSatellite().toString());
        String productId = PRODUCT_IDENTIFIER_PREFIX
                + String.join("_", satelliteAbr, def.getInstrument(), def.getProductLevel());

        return def.getProductType() != null && !def.getProductType().isEmpty()
                ? String.join("_", productId, def.getProductType())
                : productId;
    }

	@Override
	public Class getQuartzJobClass() {
		// TODO Auto-generated method stub
		return CodeDeJob.class;
	}

	@Override
	public JobDetail modifyBboxParameter(JobDetail jobDetail, String expandedBbox) {
		String[] split = expandedBbox.split(",");
		String extent = split[0] + " " + split[1] + "," + split[2] + " " + split[3];
		jobDetail.getJobDataMap().put(CodeDeJob.BBOX_KEY, extent);    // e.g. "52.0478 6.0124,52.5687 7.1420"
		return jobDetail;
	}
}
