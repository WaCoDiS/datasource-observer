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

import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.CopernicusSubsetDefinition;
import de.wacodis.observer.model.SensorWebSubsetDefinition;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class SensorWebJobFactoryTest {

    @Test
    public void testGenerateSubsetSpecificIdentifier(){
        SensorWebSubsetDefinition def = new SensorWebSubsetDefinition();
        def.setSourceType(AbstractSubsetDefinition.SourceTypeEnum.SENSORWEBSUBSETDEFINITION);
        def.setProcedure("testProcedure");
        def.setObservedProperty("testProperty");
        def.setOffering("testOffering");
        def.setFeatureOfInterest("testFOI");
        def.setServiceUrl("testService");

        SensorWebJobFactory jobFactory = new SensorWebJobFactory();
        String identifier = jobFactory.generateSubsetSpecificIdentifier(def);

        Assert.assertEquals("SensorWebSubsetDefinition_[testProcedure]_[testProperty]_[testOffering]_[testFOI]_[testService]", identifier);
    }

}
