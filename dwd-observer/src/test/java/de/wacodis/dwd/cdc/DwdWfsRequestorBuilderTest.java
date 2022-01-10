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
package de.wacodis.dwd.cdc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import de.wacodis.dwd.cdc.model.Envelope;
import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.opengis.wfs.x20.GetFeatureDocument;

class DwdWfsRequestorBuilderTest {

    private static DwdWfsRequestParams params;
    private static DwdWfsRequestorBuilder builder;
    ArrayList<String> valuesFile = new ArrayList<String>();
    ArrayList<String> valuesCalculated = new ArrayList<String>();

    @BeforeAll
    static void setup() {

        params = new DwdWfsRequestParams();
        params.setVersion("2.0.0");
        params.setTypeName("FX_MN003");
        // params.setTypeName("CDC:VGSL_TT_TU_MN009");

        List<Float> bounds = new ArrayList<Float>();
        bounds.add(0, 51.0000f);
        bounds.add(1, 6.6000f);
        bounds.add(2, 51.5000f);
        bounds.add(3, 7.3000f);
        Envelope envelope = new Envelope();
        envelope.setMinLon(6.6000f);
        envelope.setMinLat(51.0000f);
        envelope.setMaxLon(7.3000f);
        envelope.setMaxLat(51.5000f);

        params.setEnvelope(envelope);

        DateTime startDate = DateTime.parse("2019-04-24T01:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
        DateTime endDate = DateTime.parse("2019-04-25T10:00:00Z", DwdWfsRequestorBuilder.FORMATTER);

        params.setStartDate(startDate);
        params.setEndDate(endDate);

        builder = new DwdWfsRequestorBuilder(params);

    }

    @DisplayName("Test builder Method")
    @Test
    void testBuilder() throws IOException, XmlException {

        GetFeatureDocument result = builder.createGetFeaturePost();

        InputStream postMessage = this.getClass().getResourceAsStream("/postmessage-test.xml");
        GetFeatureDocument gfdoc = GetFeatureDocument.Factory.parse(postMessage);

        iterateNodes(gfdoc.getGetFeature().getDomNode(), valuesFile);
        iterateNodes(result.getGetFeature().getDomNode(), valuesCalculated);

        Assertions.assertTrue(valuesFile.size() == valuesCalculated.size());
        for (int i = 0; i < valuesFile.size(); i++) {
            Assertions.assertEquals(valuesFile.get(i), valuesCalculated.get(i));
        }
    }

    private ArrayList<String> iterateNodes(Node node, ArrayList<String> values) {
        if (node.hasChildNodes()) {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node innerNode = nodeList.item(i);
                iterateNodes(innerNode, values);

            }
        } else {
            values.add(node.getNodeValue());
        }

        return values;
    }

}
