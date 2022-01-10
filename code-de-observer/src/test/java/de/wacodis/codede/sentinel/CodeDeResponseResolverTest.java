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
package de.wacodis.codede.sentinel;

import de.wacodis.observer.decode.SimpleNamespaceContext;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeDeResponseResolverTest {

    private static CodeDeResponseResolver resolver;
    private static Document xmlDoc;
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder db;
    private static XPath xpath;
    private static Node node;

    @BeforeAll
    public static void setup() throws Exception {

        resolver = new CodeDeResponseResolver();
        dbf.setNamespaceAware(true);
        //dbf.setCoalescing(true);
        db = dbf.newDocumentBuilder();
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        Map<String, String> prefMap = new HashMap<String, String>() {
            {
                put("a", "http://www.w3.org/2005/Atom");
            }
        };
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xpath.setNamespaceContext(namespaces);

        InputStream openSearchResponseStream = CodeDeResponseResolverTest.class.getClassLoader().getResourceAsStream("catalog.code-de.org.xml");
        xmlDoc = db.parse(openSearchResponseStream);
        String xPathString = "/a:feed/a:entry[1]";
        XPathExpression expression = xpath.compile(xPathString);
        node = (Node) expression.evaluate(xmlDoc, XPathConstants.NODE);

    }

    @Test
    public void testGetDownloadLink() throws IOException, SAXException, XPathExpressionException {

        // expected download links
        String expectedDownloadLink = "https://code-de.org/download/S2A_MSIL2A_20190120T103341_N0211_R108_T32ULB_20190120T131644.SAFE.zip";
        // actual download link
        String actualDownloadLink = resolver.getDownloadLink(node);

        Assertions.assertEquals(expectedDownloadLink, actualDownloadLink);
    }

    @Test
    public void testGetCloudCoverage() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        // expected cloud coverage
        float expectedCloudCoverage = 2.175213f;
        // actual cloud coverage
        float actualCloudCoverage = Float.parseFloat(resolver.getCloudCoverage(node));

        Assertions.assertEquals(expectedCloudCoverage, actualCloudCoverage, 0.001);
    }

    @Test
    public void testGetIdentifier() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        // expected identifier
        String expectedIdentifier = "EOP:CODE-DE:S2_MSI_L2A:/S2A_MSIL2A_20190120T103341_N0211_R108_T32ULB_20190120T131644";
        // actual identifier
        String actualIdentifier = resolver.getIdentifier(node);

        Assertions.assertEquals(expectedIdentifier, actualIdentifier);
    }

    @Test
    public void testGetTimeFrame() throws IOException, SAXException, XPathExpressionException {

        // expected time frame
        List<DateTime> expectedTimeFrame = new ArrayList<DateTime>() {
            {
                add(DateTime.parse("2019-01-20T10:33:41.024Z", CodeDeResponseResolver.FORMATTER));
                add(DateTime.parse("2019-01-20T10:33:41.024Z", CodeDeResponseResolver.FORMATTER));
            }
        };

        // actual time frame
        List<DateTime> actualTimeFrame = resolver.getTimeFrame(node);

        Assertions.assertEquals(expectedTimeFrame, actualTimeFrame);
    }

    @Test
    public void testGetBbox() throws IOException, SAXException, XPathExpressionException {

        // expected bbox
        List<Float> expectedbbox = new ArrayList<Float>() {
            {
                add(50.43685871745407f);
                add(6.590688202965415f);
                add(51.44399790803582f);
                add(7.729300891794365f);
            }
        };

        // actual bbox
        List<Float> actualBbox = resolver.getBbox(node);

        Assertions.assertEquals(expectedbbox, actualBbox);
    }

    @Test
    public void testGetNumberOfPages() throws XPathExpressionException {
        // expected number of pages
        int expectedNOP = 3;    // 114/50 and round result up
        // actual number of pages
        int actualNOP = resolver.getNumberOfPages(xmlDoc);

        Assertions.assertEquals(expectedNOP, actualNOP);
    }

}