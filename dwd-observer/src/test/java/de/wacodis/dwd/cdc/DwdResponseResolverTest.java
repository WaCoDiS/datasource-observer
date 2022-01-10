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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.wacodis.dwd.cdc.model.Envelope;
import de.wacodis.dwd.cdc.model.SpatioTemporalExtent;
import net.opengis.gml.x32.EnvelopeDocument;
import net.opengis.gml.x32.EnvelopeType;
import net.opengis.wfs.x20.EnvelopePropertyType;
import net.opengis.wfs.x20.FeatureCollectionDocument;
import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class DwdResponseResolverTest {

	private static DwdResponseResolver resolver;
	private static DocumentBuilder docBuilder;
	private String typeName = DwdWfsRequestorBuilder.TYPE_NAME_PREFIX + "FX_MN003";

	@BeforeAll
	static void setUp() throws ParserConfigurationException {
		resolver = new DwdResponseResolver();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		docBuilder = dbf.newDocumentBuilder();
	}

	@Test
	void testRequestTypeName() throws ParserConfigurationException, SAXException, IOException {
		// actual
		InputStream getCapabilitiesStream = this.getClass().getResourceAsStream("/getCapabilities-test.xml");

		String[] typenameArray = resolver.requestTypeName(docBuilder.parse(getCapabilitiesStream), typeName);
		String name = typenameArray[0];
		String title = typenameArray[1];
		
		// expected
		String expectedName = name;
		String expectedTitle = "Tägliche Stationsmessungen der maximalen Windspitze in ca. 10 m Höhe in m/s";

		Assertions.assertEquals(expectedTitle, title);
		Assertions.assertEquals(expectedName, name);
	}

//	@Test
	void testGenerateSpatioTemporalExtent() throws IOException, SAXException, ParserConfigurationException, XmlException {
		// actual
		InputStream getFeatureResponse = this.getClass().getResourceAsStream("/getFeatureResult-test.xml");

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();
		Document doc = docBuilder.parse(getFeatureResponse);

		FeatureCollectionDocument featureDoc = FeatureCollectionDocument.Factory.parse(getFeatureResponse);
		EnvelopePropertyType envelopePropertyType = featureDoc.getFeatureCollection().getBoundedBy();
		EnvelopeType env = (EnvelopeType)envelopePropertyType.changeType(EnvelopeDocument.type);

		EnvelopeDocument envDocument = EnvelopeDocument.Factory.parse(featureDoc.getFeatureCollection().getBoundedBy().xmlText());

		String boundedByText = featureDoc.getFeatureCollection().getBoundedBy().getDomNode().getTextContent();
		NodeList nodes = featureDoc.getFeatureCollection().getBoundedBy().getDomNode().getChildNodes();
		int nodeLength = nodes.getLength();

		SpatioTemporalExtent timeAndBBox = resolver.generateSpatioTemporalExtent(doc, typeName);
		Envelope bBox = timeAndBBox.getbBox();
		ArrayList<DateTime> timeFrame = timeAndBBox.getTimeFrame();

		ArrayList<DateTime> expectedTimeFrame = new ArrayList<DateTime>();
		DateTime expectedStartDate = DateTime.parse("2019-04-25T00:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		DateTime expectedEndDate = DateTime.parse("2019-04-25T00:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		expectedTimeFrame.add(expectedStartDate);
		expectedTimeFrame.add(expectedEndDate);

		// comparison
		Assertions.assertEquals(6.7686f, bBox.getMinLon());
		Assertions.assertEquals(51.2531f, bBox.getMinLat());
		Assertions.assertEquals(7.2156f, bBox.getMaxLon());
		Assertions.assertEquals(51.4041f, bBox.getMaxLat());

		Assertions.assertEquals(expectedTimeFrame, timeFrame);
	}

	@Test
	void testResponseContainsFeatureCollectionForValidResponse() throws  IOException, SAXException {
		InputStream getFeatureResponse = this.getClass().getResourceAsStream("/getFeatureResult-test.xml");

		Assertions.assertTrue(resolver.responseContainsFeatureCollection(docBuilder.parse(getFeatureResponse)));
	}

	@Test
	void testResponseContainsFeatureCollectionForEmptyResponse() throws IOException, SAXException {
		InputStream getFeatureResponse = this.getClass().getResourceAsStream("/getFeatureResultEmpty-test.xml");

		Assertions.assertFalse(resolver.responseContainsFeatureCollection(docBuilder.parse(getFeatureResponse)));
	}

	@Test
	void testResponseContainsFeatureCollectionThrowsExceptionForInvalidResponse() {
		InputStream getFeatureResponse = this.getClass().getResourceAsStream("/exceptionResponse-test.xml");

		Assertions.assertThrows(SAXException.class, () -> resolver.responseContainsFeatureCollection(docBuilder.parse(getFeatureResponse)));
	}


	@Test
	void testResponseContainsCapabilities() throws IOException, SAXException {
		InputStream getCapabiltiesResponse = this.getClass().getResourceAsStream("/getCapabilities-test.xml");

		Assertions.assertTrue( resolver.responseContainsCapabilities(docBuilder.parse(getCapabiltiesResponse)));
	}

	@Test
	void testResponseContainsCapabilitiesThrowsExceptionForInvalidResponse() {
		InputStream getCapabiltiesResponse = this.getClass().getResourceAsStream("/exceptionResponse-test.xml");

		Assertions.assertThrows(SAXException.class, () -> resolver.responseContainsCapabilities(docBuilder.parse(getCapabiltiesResponse)));
	}


}
