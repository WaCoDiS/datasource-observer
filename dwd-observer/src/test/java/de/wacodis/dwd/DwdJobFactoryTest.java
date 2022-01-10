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
package de.wacodis.dwd;

import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.DwdSubsetDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class DwdJobFactoryTest {

    @Test
    public void testGenerateSubsetSpecificIdentifier(){
        DwdSubsetDefinition def = new DwdSubsetDefinition();
        def.setSourceType(AbstractSubsetDefinition.SourceTypeEnum.DWDSUBSETDEFINITION);
        def.setServiceUrl("https://cdc.dwd.de:443/geoserver/CDC/wfs?");
        def.setLayerName("FX_MN003");

        DwdJobFactory jobFactory = new DwdJobFactory();
        String identifier = jobFactory.generateSubsetSpecificIdentifier(def);

        Assertions.assertEquals("DwdSubsetDefinition_[https://cdc.dwd.de:443/geoserver/CDC/wfs?]_[FX_MN003]", identifier);
    }
}
