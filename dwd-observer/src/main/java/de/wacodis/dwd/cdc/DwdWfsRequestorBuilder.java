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

import java.util.ArrayList;

import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import de.wacodis.dwd.cdc.model.Envelope;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import net.opengis.fes.x20.AndDocument;
import net.opengis.fes.x20.BBOXDocument;
import net.opengis.fes.x20.BBOXType;
import net.opengis.fes.x20.BinaryLogicOpType;
import net.opengis.fes.x20.FilterDocument;
import net.opengis.fes.x20.FilterType;
import net.opengis.fes.x20.LiteralDocument;
import net.opengis.fes.x20.LiteralType;
import net.opengis.fes.x20.PropertyIsBetweenDocument;
import net.opengis.fes.x20.PropertyIsBetweenType;
import net.opengis.fes.x20.ValueReferenceDocument;
import net.opengis.gml.x32.DirectPositionType;
import net.opengis.gml.x32.EnvelopeDocument;
import net.opengis.gml.x32.EnvelopeType;
import net.opengis.wfs.x20.GetCapabilitiesDocument;
import net.opengis.wfs.x20.GetCapabilitiesType;
import net.opengis.wfs.x20.GetFeatureDocument;
import net.opengis.wfs.x20.GetFeatureType;
import net.opengis.wfs.x20.QueryDocument;
import net.opengis.wfs.x20.QueryType;

/**
 * Helps building a DWD WFS request for certain request parameters
 */
public class DwdWfsRequestorBuilder {

    // class attributes
    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final String TYPE_NAME_PREFIX = "CDC:VGSL_";
    public static final String GEOMETRY_ATTRIBUTE = "CDC:GEOM";
    public static final String TIMESTAMP_ATTRIBUTE = "CDC:ZEITSTEMPEL";
    public static final String EPSG_URN = "urn:ogc:def:crs:EPSG::4326";
    public static final String SERVICE_ATTRIBUTE = "WFS";
    public static final String OUTPUT_FORMAT = "application/gml+xml; version=3.2";

    // attributes
//    String version;// &version=2.0.0
//    String typeName;// &typeName=CDC%3AVGSL_FX_MN003
//    List<String> bbox = new ArrayList<String>();// &bbox=51.200,6.700,51.500,7.300
//    DateTime startDate;
//    DateTime endDate;
    ArrayList<String[]> namespaces = new ArrayList<String[]>();
    ArrayList<String[]> attributes = new ArrayList<String[]>();
    private DwdWfsRequestParams params;

    public DwdWfsRequestorBuilder(DwdWfsRequestParams params) {
//        this.version = params.getVersion();
//        this.typeName = params.getTypeName();
//        // convert bbox from Float to String
//        for (int i = 0; i < params.getBbox().size(); i++) {
//            bbox.add(i, Float.toString(params.getBbox().get(i)));
//        }
//        this.startDate = params.getStartDate();
//        this.endDate = params.getEndDate();
        this.params = params;
    }

    /**
     * Creates a {@link GetFeatureDocument} for a WFS GetFeature request assembling the single POSt body XML elements
     *
     * @return {@link GetFeatureDocument} for the WFS GetFeature request POST body
     */
    public GetFeatureDocument createGetFeaturePost() {

        createXmlAttributesList();

        // <GetFeature>
        GetFeatureDocument getFeatureDoc = GetFeatureDocument.Factory.newInstance();
        GetFeatureType getFeature = getFeatureDoc.addNewGetFeature();

        // <Query>
        QueryDocument queryDoc = QueryDocument.Factory.newInstance();
        QueryType query = queryDoc.addNewQuery();
        ArrayList<String> typeList = new ArrayList<String>();
        typeList.add(TYPE_NAME_PREFIX + params.getTypeName());

        // <Filter>
        FilterDocument filterDocument = FilterDocument.Factory.newInstance();
        FilterType filter = filterDocument.addNewFilter();

        // <And>
        AndDocument andDocument = AndDocument.Factory.newInstance();
        BinaryLogicOpType andType = andDocument.addNewAnd();

        // <BBOX>
        BBOXType bboxType = createBboxElement(this.params.getEnvelope());
        // </BBOX>
        addXSAnyElement(andType, bboxType);

        // <PropertyIsBetween>
        PropertyIsBetweenType propBetweenType = createPropertyIsBetweenElement();
        // </PropertyIsBetween>
        addXSAnyElement(andType, propBetweenType);

        // </And>
        filter.set(andDocument);
        // </Filter>
        query.set(filterDocument);
        // </Query>
        query.setTypeNames(typeList);
        // </GetFeature>
        getFeature.set(queryDoc);

        // Attributes for <GetFeature>
        // namespaces
        addAttributesToElement(getFeature);
        return getFeatureDoc;
    }

    /**
     * Add attributes and namespaces to the {@link GetFeatureType} element
     *
     * @param getFeature {@link GetFeatureType} of a WFS request
     */
    private void addAttributesToElement(GetFeatureType getFeature) {
        XmlCursor cursor = getFeature.newCursor();
        cursor.toNextToken();
        // insert namespaces (prefix, url)
        for (int i = 0; i < this.namespaces.size(); i++) {
            cursor.insertNamespace(this.namespaces.get(i)[0], this.namespaces.get(i)[1]);
        }
        // insert attributes (prefix, attributename, attributevalue)
        for (int i = 0; i < this.attributes.size(); i++) {
            cursor.insertAttributeWithValue(this.attributes.get(i)[1], this.attributes.get(i)[0], this.attributes.get(i)[2]);
        }
        cursor.dispose();
    }

    /**
     * Creates a {@link PropertyIsBetweenType} for a WFS GetFeature request
     *
     * @return {@link PropertyIsBetweenType}
     */
    private PropertyIsBetweenType createPropertyIsBetweenElement() {

        PropertyIsBetweenDocument propBetweenDocument = PropertyIsBetweenDocument.Factory.newInstance();
        PropertyIsBetweenType propBetweenType = propBetweenDocument.addNewPropertyIsBetween();

        // <ValueReference>
        ValueReferenceDocument valueRefDocument2 = ValueReferenceDocument.Factory.newInstance();
        valueRefDocument2.setValueReference(TIMESTAMP_ATTRIBUTE);
        // </ValueReference>

        // <Literal>
        LiteralDocument litDoc = LiteralDocument.Factory.newInstance();
        LiteralType litType1 = litDoc.addNewLiteral();
        XmlString xmlString = XmlString.Factory.newValue(FORMATTER.print(this.params.getStartDate()));
        litType1.set(xmlString);
        // </Literal>

        // <Literal>
        LiteralDocument litDoc2 = LiteralDocument.Factory.newInstance();
        LiteralType litType2 = litDoc2.addNewLiteral();
        XmlString xmlString2 = XmlString.Factory.newValue(FORMATTER.print(this.params.getEndDate()));
        litType2.set(xmlString2);
        // </Literal>

        addXSAnyElement(propBetweenType, litType2);
        addXSAnyElement(propBetweenType, litType1);

        addXSAnyElement(propBetweenType, valueRefDocument2.getExpression());
        return propBetweenType;
    }

    /**
     * Creates a {@link BBOXType} for a WFS GetFeature request
     *
     * @return {@link BBOXType}
     */
    public BBOXType createBboxElement(Envelope envelope) {

        BBOXDocument bboxDocument = BBOXDocument.Factory.newInstance();
        BBOXType bboxType = bboxDocument.addNewBBOX();

        // <ValueReference>
        ValueReferenceDocument valueRefDocument = ValueReferenceDocument.Factory.newInstance();
        valueRefDocument.setValueReference(GEOMETRY_ATTRIBUTE);
        // </ValueReference>

        // <Envelope>
        EnvelopeDocument envDoc = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envType = envDoc.addNewEnvelope();
        envType.setSrsName(EPSG_URN);
        DirectPositionType lowerCorner = envType.addNewLowerCorner();
        lowerCorner.setStringValue(envelope.getMinLat() + " " + envelope.getMinLon());
        DirectPositionType upperCorner = envType.addNewUpperCorner();
        upperCorner.setStringValue(envelope.getMaxLat() + " " + envelope.getMaxLon());
        // </Envelope>
        addXSAnyElement(bboxType, envType);
        addXSAnyElement(bboxType, valueRefDocument.getExpression());
        return bboxType;
    }

    /**
     * Adds childelements to a target element
     *
     * @param target   where the children have to be added
     * @param newChild what has to be added
     */
    protected void addXSAnyElement(XmlObject target, XmlObject newChild) {
        XmlCursor childCur = newChild.newCursor();

        XmlCursor targetCur = target.newCursor();
        targetCur.toNextToken();

        childCur.moveXml(targetCur);

        childCur.dispose();
        targetCur.dispose();
    }

    /**
     * Creates lists for the namespaces and attributes
     */
    private void createXmlAttributesList() {
        // namespaces
        this.namespaces.add(new String[]{"xsi", "http://www.w3.org/2001/XMLSchema-instance"});
        this.namespaces.add(new String[]{"", "http://www.opengis.net/wfs/2.0"});
        this.namespaces.add(new String[]{"wfs", "http://www.opengis.net/wfs/2.0"});
        this.namespaces.add(new String[]{"ows", "http://www.opengis.net/ows/1.1"});
        this.namespaces.add(new String[]{"gml", "http://www.opengis.net/gml/3.2"});
        this.namespaces.add(new String[]{"fes", "http://www.opengis.net/fes/2.0"});
        this.namespaces.add(new String[]{"xlink", "http://www.w3.org/1999/xlink"});
        this.namespaces.add(new String[]{"xs", "http://www.w3.org/2001/XMLSchema"});
        this.namespaces.add(new String[]{"inspire_dls", "http://inspire.ec.europa.eu/schemas/inspire_dls/1.0"});
        this.namespaces.add(new String[]{"inspire_common", "http://inspire.ec.europa.eu/schemas/common/1.0"});
        this.namespaces.add(new String[]{"CDC", "https://cdc.dwd.de"});
        this.namespaces.add(new String[]{"ogc", "http://www.opengis.net/ogc"});
        // attributes
        this.attributes.add(new String[]{"xsi", "schemaLocation",
                "http://www.opengis.net/wfs/2.0 https://cdc.dwd.de:443/geoserver/schemas/wfs/2.0/wfs.xsd http://inspire.ec.europa.eu/schemas/inspire_dls/1.0 http://inspire.ec.europa.eu/schemas/inspire_dls/1.0/inspire_dls.xsd"});
        this.attributes.add(new String[]{"", "service", SERVICE_ATTRIBUTE});
        this.attributes.add(new String[]{"", "version", this.params.getVersion()});
        this.attributes.add(new String[]{"", "outputFormat", OUTPUT_FORMAT});
    }

    /**
     * Creates GetCapabilities POST body
     *
     * @return {@link GetCapabilitiesDocument} for GetCapabilities POST request body
     */
    public GetCapabilitiesDocument createGetCapabilitiesPost() {
        GetCapabilitiesDocument getCapDoc = GetCapabilitiesDocument.Factory.newInstance();
        GetCapabilitiesType getCapType = getCapDoc.addNewGetCapabilities();
        getCapType.setService(SERVICE_ATTRIBUTE);

        return getCapDoc;
    }
}
