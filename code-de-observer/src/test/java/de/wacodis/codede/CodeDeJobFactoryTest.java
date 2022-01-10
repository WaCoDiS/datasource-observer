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

import de.wacodis.observer.model.AbstractSubsetDefinition;
import de.wacodis.observer.model.CopernicusSubsetDefinition;
import de.wacodis.observer.model.DwdSubsetDefinition;
import de.wacodis.observer.model.WacodisJobDefinition;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class CodeDeJobFactoryTest {

    private static CodeDeJobFactory jobFactory;

    @BeforeAll
    public static void init() {
        jobFactory = new CodeDeJobFactory();
    }

    @Test
    @DisplayName("Test supportsJobDefinition should return true for supported SubsetDefinition")
    void testSupportsJobDefinitionForSupportedSubsetDefinitionReturnsTrue() {
        WacodisJobDefinition jobDef = new WacodisJobDefinition();
        jobDef.addInputsItem(new CopernicusSubsetDefinition());

        Assertions.assertTrue(jobFactory.supportsJobDefinition(jobDef));
    }

    @Test
    @DisplayName("Test supportsJobDefinition should return false for unsupported SubsetDefinition")
    void testSupportsJobDefinitionForUnsupportedSubsetDefinitioReturnsFalse() {
        WacodisJobDefinition jobDef = new WacodisJobDefinition();
        jobDef.addInputsItem(new DwdSubsetDefinition());

        Assertions.assertFalse(jobFactory.supportsJobDefinition(jobDef));
    }

    @Test
    @Disabled
    void testBuildProductIdentifierWithoutProductType() {
        CopernicusSubsetDefinition subsetDef = new CopernicusSubsetDefinition();
        subsetDef.setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);
        subsetDef.setInstrument("MSI");
        subsetDef.setProductLevel("L2A");

        Assertions.assertEquals("EOP:CODE-DE:S2_MSI_L2A", jobFactory.buildProductIdentifier(subsetDef));
    }

    @Test
    @Disabled
    void testBuildProductIdentifierWithProductType() {
        CopernicusSubsetDefinition subsetDef = new CopernicusSubsetDefinition();
        subsetDef.setSatellite(CopernicusSubsetDefinition.SatelliteEnum._3);
        subsetDef.setInstrument("SLSTR");
        subsetDef.setProductLevel("L1");
        subsetDef.setProductType("RBT");

        Assertions.assertEquals("EOP:CODE-DE:S3_SLSTR_L1_RBT", jobFactory.buildProductIdentifier(subsetDef));
    }

    @Test
    public void testGenerateSubsetSpecificIdentifier(){
        CopernicusSubsetDefinition def = new CopernicusSubsetDefinition();
        def.setSourceType(AbstractSubsetDefinition.SourceTypeEnum.COPERNICUSSUBSETDEFINITION);
        def.setSatellite(CopernicusSubsetDefinition.SatelliteEnum._2);

        CodeDeJobFactory jobFactory = new CodeDeJobFactory();
        String identifier = jobFactory.generateSubsetSpecificIdentifier(def);

        Assertions.assertEquals("CopernicusSubsetDefinition_sentinel-2", identifier);
    }


}
