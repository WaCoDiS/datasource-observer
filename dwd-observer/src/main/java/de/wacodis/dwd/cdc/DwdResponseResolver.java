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
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import de.wacodis.dwd.cdc.model.Envelope;
import de.wacodis.dwd.cdc.model.SpatioTemporalExtent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


@Component
/**
 * Helper class to resolve required product metadata from DWD WFS responses
 */
public class DwdResponseResolver {

    final static Logger LOG = LoggerFactory.getLogger(DwdResponseResolver.class);
    private static final String FEATURE_TYPE_TAG = "FeatureType";
    private static final String TITLE_TAG = "Title";
    private static final String NAME_TAG = "Name";
    private static final String LOWER_CORNER_TAG = "gml:lowerCorner";
    private static final String UPPER_CORNER_TAG = "gml:upperCorner";
    private static final String FEATURE_COLLECION_TAG = "wfs:FeatureCollection";
    private static final String CAPABILITIES_TAG = "wfs:WFS_Capabilities";

    /**
     * Delivers a String Array consisting of <name>- and <title> values
     *
     * @param typeName name with prefix, e.g. CDC:VGSL_TT_TU_MN009
     * @param doc      GetCapabilities response document
     * @return featureTypeName <name> and <title> of the denoted feature
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public String[] requestTypeName(Document doc, String typeName)
            throws ParserConfigurationException, SAXException, IOException {
        LOG.debug("Resolve TypeName out of GetCapabilities Document");

        // initialize return attribute
        String[] featureTypeName = new String[2];

        // search all FeatureType elements
        NodeList nodes = doc.getElementsByTagName(FEATURE_TYPE_TAG);
        for (int i = 0; i < nodes.getLength(); i++) {
            // check content of childnodes <name> and <title> of every <FeatureType>
            Element featureType = (Element) nodes.item(i);

            NodeList titleNodes = featureType.getElementsByTagName(TITLE_TAG);
            String title = titleNodes.item(0).getTextContent();

            NodeList NameNodes = featureType.getElementsByTagName(NAME_TAG);
            String name = NameNodes.item(0).getTextContent();

            // search for the correct typeName
            if (name.equals(typeName)) {
                // fill return attribute
                featureTypeName[0] = name; // <name>
                featureTypeName[1] = title; // <title>
            }

        }

        return featureTypeName;
    }

    /**
     * Determines the spatial and temporal extentn of the denoted feature
     *
     * @param doc      GetFeature response document
     * @param typeName name with prefix, e.g. CDC:VGSL_TT_TU_MN009
     * @return timeAndBbox spatial and temporal extent
     */
    public SpatioTemporalExtent generateSpatioTemporalExtent(Document doc, String typeName) {
        SpatioTemporalExtent timeAndBbox = new SpatioTemporalExtent();

        // BBOX
        NodeList lowerNodes = doc.getElementsByTagName(LOWER_CORNER_TAG);
        String lowerCornerBBox = lowerNodes.item(0).getTextContent();
        NodeList upperNodes = doc.getElementsByTagName(UPPER_CORNER_TAG);
        String upperCornerBBox = upperNodes.item(0).getTextContent();

        // BBOX Parameter
        // Schema is [minLon, minLat, maxLon, maxLat]
        Envelope envelope = new Envelope();
        envelope.setMinLon(Float.parseFloat(lowerCornerBBox.split(" ")[1]));
        envelope.setMinLat(Float.parseFloat(lowerCornerBBox.split(" ")[0]));
        envelope.setMaxLon(Float.parseFloat(upperCornerBBox.split(" ")[1]));
        envelope.setMaxLat(Float.parseFloat(upperCornerBBox.split(" ")[0]));

        // TimeFrame Parameter
        DateTime startDate = new DateTime();
        DateTime endDate = new DateTime();
        ArrayList<DateTime> timeFrame = new ArrayList<DateTime>();

        NodeList featureNodes = doc.getElementsByTagName(typeName);
        for (int i = 0; i < featureNodes.getLength(); i++) {
            Element feature = (Element) featureNodes.item(i);
            NodeList timeStampNodes = feature.getElementsByTagName(DwdWfsRequestorBuilder.TIMESTAMP_ATTRIBUTE);
            String timeStamp = timeStampNodes.item(0).getTextContent();

            DateTime temp = DateTime.parse(timeStamp, DwdWfsRequestorBuilder.FORMATTER);
            // Set start Values
            if (i == 0) {

                // Time Frame - First values
                startDate = temp;
                endDate = temp;
                timeFrame.add(0, startDate);
                timeFrame.add(1, endDate);
            }

            // Set StartDate or EndDate
            if (temp.isBefore(startDate)) {
                startDate = temp;
                timeFrame.remove(0);
                timeFrame.add(0, startDate);
            }

            if (temp.isAfter(endDate)) {
                endDate = temp;
                timeFrame.remove(1);
                timeFrame.add(1, endDate);
            }
        }
        timeAndBbox.setbBox(envelope);
        timeAndBbox.setTimeFrame(timeFrame);

        return timeAndBbox;
    }

    /**
     * Checks whether the WFS response {@link Document} contains a FeatureCollection or not
     *
     * @param doc the {@link Document} that contains the WFS response
     * @return true if the response document contains a FeatureCollection and the collection is not empty
     * @throws SAXException if the WFS response does not contain a wfs:FeatureCollection tag
     */
    public boolean responseContainsFeatureCollection(Document doc) throws SAXException {
        if (!doc.getDocumentElement().getTagName().equals(FEATURE_COLLECION_TAG)) {
            throw new SAXException(String.format("No tag \'%s\' within WFS response document: %s", FEATURE_COLLECION_TAG, doc.getDocumentElement().getTextContent()));
        }
        int numberReturned = Integer.parseInt(doc.getDocumentElement().getAttribute("numberReturned"));
        if (numberReturned <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether the WFS response {@link Document} contains WFS_Capabilities or not
     *
     * @param doc the {@link Document} that contains the WFS response
     * @return true if the response document contains WFS_Capabilities
     * @throws SAXException if the WFS response does not contain a wfs:WFS_Capabilities tag
     */
    public boolean responseContainsCapabilities(Document doc) throws SAXException {
        if (!doc.getDocumentElement().getTagName().equals(CAPABILITIES_TAG)) {
            throw new SAXException(String.format("No tag \'%s\' within WFS response document: %s", CAPABILITIES_TAG, doc.getDocumentElement().getTextContent()));
        }

        return true;
    }

}
