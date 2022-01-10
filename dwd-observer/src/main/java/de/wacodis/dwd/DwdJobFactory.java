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

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.wacodis.observer.config.ExecutionIntervalConfig;
import de.wacodis.observer.core.JobFactory;
import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.DwdSubsetDefinition;
import de.wacodis.observer.model.WacodisJobDefinition;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@Component
public class DwdJobFactory implements JobFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DwdJobFactory.class);

    @Autowired
    private ExecutionIntervalConfig intervalConfig;

    @Override
    public boolean supportsJobDefinition(WacodisJobDefinition job) {
        Optional<AbstractSubsetDefinition> def = job.getInputs().stream()
                .filter(in -> in instanceof DwdSubsetDefinition).findAny();

        return def.isPresent();
    }

//    @Override
//    public JobDetail initializeJob(WacodisJobDefinition job, JobDataMap data) {
//        LOG.info("Preparing DwdJob JobDetail");
//
//        Optional<AbstractSubsetDefinition> defOpt = job.getInputs().stream()
//                .filter((i -> i instanceof DwdSubsetDefinition)).findFirst();
//
//        // this should always be the case
//        if (defOpt.isPresent()) {
//            DwdSubsetDefinition def = (DwdSubsetDefinition) defOpt.get();
//
//            // Put all required request parameters into JobDataMap
//
//            // data.put(DwdJob.LAYER_NAME_KEY, def.getLayerName());
//            data.put(DwdJob.VERSION_KEY, "2.0.0");
//            data.put(DwdJob.LAYER_NAME_KEY, def.getLayerName());
//            data.put(DwdJob.SERVICE_URL_KEY, def.getServiceUrl());
//            data.put(DwdJob.TEMPORAL_COVERAGE_KEY, job.getTemporalCoverage().getDuration());
//            data.put(DwdJob.EXECUTION_INTERVAL_KEY, intervalConfig.getDwd());
//            data.put(DwdJob.LATEST_REQUEST_END_DATE, null);
//
//            String extent = job.getAreaOfInterest().getExtent().get(0) + " "
//                    + job.getAreaOfInterest().getExtent().get(1) + ","
//                    + job.getAreaOfInterest().getExtent().get(2) + " "
//                    + job.getAreaOfInterest().getExtent().get(3);
//            data.put(DwdJob.EXECUTION_AREA_KEY, extent);    // e.g. "52.0478 6.0124,52.5687 7.1420"
//
//        }
//		return JobBuilder.newJob(DwdJob.class).withIdentity(job.getId().toString(), job.getName()).usingJobData(data)
//				.storeDurably(true).build();
//    }

    @Override
    public JobBuilder initializeJobBuilder(WacodisJobDefinition job, JobDataMap data, AbstractSubsetDefinition subsetDefinition) {
        if (subsetDefinition instanceof DwdSubsetDefinition) {
            LOG.info("Preparing DwdJob JobDetail");
            DwdSubsetDefinition dwdDef = (DwdSubsetDefinition) subsetDefinition;

            // Put all required request parameters into JobDataMap

            // data.put(DwdJob.LAYER_NAME_KEY, def.getLayerName());
            data.put(DwdJob.VERSION_KEY, "2.0.0");
            data.put(DwdJob.LAYER_NAME_KEY, dwdDef.getLayerName());
            data.put(DwdJob.SERVICE_URL_KEY, dwdDef.getServiceUrl());
            data.put(DwdJob.TEMPORAL_COVERAGE_KEY, job.getTemporalCoverage().getDuration());
            data.put(DwdJob.EXECUTION_INTERVAL_KEY, intervalConfig.getDwd());
            data.put(DwdJob.LATEST_REQUEST_END_DATE, null);

            String extent = job.getAreaOfInterest().getExtent().get(0) + " "
                    + job.getAreaOfInterest().getExtent().get(1) + ","
                    + job.getAreaOfInterest().getExtent().get(2) + " "
                    + job.getAreaOfInterest().getExtent().get(3);
            data.put(DwdJob.EXECUTION_AREA_KEY, extent);    // e.g. "52.0478 6.0124,52.5687 7.1420"

        }
        return JobBuilder.newJob(DwdJob.class)
                .usingJobData(data);
    }

    @Override
    public Stream<AbstractSubsetDefinition> filterJobInputs(WacodisJobDefinition job) {
        return job.getInputs().stream()
                .filter(in -> in instanceof DwdSubsetDefinition);
    }

    @Override
    public String generateSubsetSpecificIdentifier(AbstractSubsetDefinition subsetDefinition) {
        StringBuilder builder = new StringBuilder("");

        // TODO check ID generation --> what parameters shall be used?

        if (subsetDefinition instanceof DwdSubsetDefinition) {
            DwdSubsetDefinition dwdDef = (DwdSubsetDefinition) subsetDefinition;
            builder.append(dwdDef.getSourceType());

            if (dwdDef.getServiceUrl() != null) {
                builder.append("_" + Collections.singletonList(dwdDef.getServiceUrl()));
            }
            if (dwdDef.getLayerName() != null) {
                builder.append("_" + Collections.singletonList(dwdDef.getLayerName()));
            }
        }

        return builder.toString();

    }
    
    @Override
	public Class getQuartzJobClass() {
		// TODO Auto-generated method stub
		return DwdJob.class;
	}
    
    @Override
	public JobDetail modifyBboxParameter(JobDetail jobDetail, String expandedBbox) {
		String[] split = expandedBbox.split(",");
		String extent = split[0] + " " + split[1] + "," + split[2] + " " + split[3];
		jobDetail.getJobDataMap().put(DwdJob.EXECUTION_AREA_KEY, extent);    // e.g. "52.0478 6.0124,52.5687 7.1420"
		return jobDetail;
	}

}
