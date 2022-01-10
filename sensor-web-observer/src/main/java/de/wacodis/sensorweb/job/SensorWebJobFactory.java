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
package de.wacodis.sensorweb.job;

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
import de.wacodis.observer.model.SensorWebSubsetDefinition;
import de.wacodis.observer.model.WacodisJobDefinition;

@Component
public class SensorWebJobFactory implements JobFactory {

    private static final Logger log = LoggerFactory.getLogger(SensorWebJobFactory.class);

    @Autowired
    private ExecutionIntervalConfig intervalConfig;

    @Override
    public boolean supportsJobDefinition(WacodisJobDefinition job) {
        Optional<AbstractSubsetDefinition> sweDefs = job.getInputs().stream()
                .filter(in -> in instanceof SensorWebSubsetDefinition).findAny();

        return sweDefs.isPresent();
    }

    //access job specific inputs
//	@Override
//	public void initializeParameters(WacodisJobDefinition job, JobDataMap data) {
//		for (AbstractSubsetDefinition subset : job.getInputs()) {
//			if (subset instanceof SensorWebSubsetDefinition) {
//				SensorWebSubsetDefinition senSubset = (SensorWebSubsetDefinition) subset;
//				data.put("procedures", Collections.singletonList(senSubset.getProcedure()));
//				data.put("observedProperties", Collections.singletonList(senSubset.getObservedProperty()));
//				data.put("offerings", Collections.singletonList(senSubset.getOffering()));
//				data.put("featureIdentifiers", Collections.singletonList(senSubset.getFeatureOfInterest()));
//				data.put("serviceURL", senSubset.getServiceUrl());
//				data.put("executionInterval", intervalConfig.getSensorWeb());
//			}
//		}
//	}
//
//	@Override
//	public JobDetail prepareJob(WacodisJobDefinition job, JobDataMap data) {
//		log.info("Preparing JobDetail");
//
//		return JobBuilder.newJob(SensorWebJob.class)
//				.withIdentity(job.getId().toString(), job.getName())
//				.usingJobData(data)
//				.build();
//	}

    @Override
    public JobBuilder initializeJobBuilder(WacodisJobDefinition job, JobDataMap data, AbstractSubsetDefinition subsetDefinition) {
        if (subsetDefinition instanceof SensorWebSubsetDefinition) {
            SensorWebSubsetDefinition senSubset = (SensorWebSubsetDefinition) subsetDefinition;
            data.put("procedures", Collections.singletonList(senSubset.getProcedure()));
            data.put("observedProperties", Collections.singletonList(senSubset.getObservedProperty()));
            data.put("offerings", Collections.singletonList(senSubset.getOffering()));
            data.put("featureIdentifiers", Collections.singletonList(senSubset.getFeatureOfInterest()));
            data.put("serviceURL", senSubset.getServiceUrl());
            data.put("executionInterval", intervalConfig.getSensorWeb());
        }

        log.info("Preparing JobDetail");

        return JobBuilder.newJob(SensorWebJob.class)
                .usingJobData(data);
    }

    @Override
    public Stream<AbstractSubsetDefinition> filterJobInputs(WacodisJobDefinition job) {
        return job.getInputs().stream()
                .filter(in -> in instanceof SensorWebSubsetDefinition);
    }

    @Override
    public String generateSubsetSpecificIdentifier(AbstractSubsetDefinition subsetDefinition) {
        StringBuilder builder = new StringBuilder("");

        // TODO check ID generation --> what parameters shall be used?

        if (subsetDefinition instanceof SensorWebSubsetDefinition) {
            SensorWebSubsetDefinition senDef = (SensorWebSubsetDefinition) subsetDefinition;
            builder.append(senDef.getSourceType());

            if (senDef.getProcedure() != null) {
                builder.append("_" + Collections.singletonList(senDef.getProcedure()));
            }
            if (senDef.getObservedProperty() != null) {
                builder.append("_" + Collections.singletonList(senDef.getObservedProperty()));
            }
            if (senDef.getOffering() != null) {
                builder.append("_" + Collections.singletonList(senDef.getOffering()));
            }
            if (senDef.getFeatureOfInterest() != null) {
                builder.append("_" + Collections.singletonList(senDef.getFeatureOfInterest()));
            }
            if (senDef.getServiceUrl() != null) {
                builder.append("_" + Collections.singletonList(senDef.getServiceUrl()));
            }
        }

        return builder.toString();
    }
    
    @Override
	public Class getQuartzJobClass() {
		// TODO Auto-generated method stub
		return SensorWebJob.class;
	}
    
    @Override
	public JobDetail modifyBboxParameter(JobDetail jobDetail, String expandedBbox) {
		// here we must do nothing as this Job dies not specify BBOX parameter
		return jobDetail;
	}

}
