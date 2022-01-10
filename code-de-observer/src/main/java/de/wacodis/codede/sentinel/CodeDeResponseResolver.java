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

import de.wacodis.codede.CodeDeJob;
import de.wacodis.observer.decode.SimpleNamespaceContext;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to resolve required product metadata from the OpenSearch Response
 *
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
@Deprecated
public class CodeDeResponseResolver {

    public CodeDeResponseResolver() {
        // enables namespaces with the xpath-library
        XPathFactory factory = XPathFactory.newInstance();
        this.xpath = factory.newXPath();
        Map<String, String> prefMap = new HashMap<String, String>() {
            {
                // default namespace
                put("a", "http://www.w3.org/2005/Atom");
                // namespaces used in the xml documents
                put("opt", "http://www.opengis.net/opt/2.1");
                put("om", "http://www.opengis.net/om/2.0");
                put("georss", "http://www.georss.org/georss");
                put("dc", "http://purl.org/dc/elements/1.1/");
                put("eop", "http://www.opengis.net/eop/2.1");
                put("os", "http://a9.com/-/spec/opensearch/1.1/");
            }
        };
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xpath.setNamespaceContext(namespaces);
    }

    private static final int ITEMS_PER_PAGE = 50;
    private static final Logger LOG = LoggerFactory.getLogger(CodeDeJob.class);
    private XPath xpath;
    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * Delivers the single entry nodes from the requested sentinel products (all <entry>-tags)
     *
     * @param responseDoc requested document
     * @return entry nodes as NodeList
     * @throws XPathExpressionException
     */
    public NodeList getEntryNodes(Document responseDoc) throws XPathExpressionException {
        String xPathString = "/a:feed/a:entry";
        XPathExpression expression = xpath.compile(xPathString);
        return (NodeList) expression.evaluate(responseDoc, XPathConstants.NODESET);
    }

    /**
     * Delivers the Downloadlink of one specific sentinel product (<entry>-Tag)
     *
     * @param entryNode One specific sentinel Product which corresponds to an <entry>-Tag
     * @return URL as String
     * @throws XPathExpressionException
     */
    public String getDownloadLink(Node entryNode) throws XPathExpressionException {
        String xPathString = "./a:link[@title=\"Download\"]/@href";
        XPathExpression expression = this.xpath.compile(xPathString);
        return (String) expression.evaluate(entryNode, XPathConstants.STRING);
    }

    /**
     * Returns the cloud coverage percentage of the product
     *
     * @param entryNode the xml document which contains the metadata of one sentinel product
     * @return float number (percentage)
     * @throws XPathExpressionException
     */
    public String getCloudCoverage(Node entryNode) throws XPathExpressionException {
        String xpathString = "./opt:EarthObservation/om:result/opt:EarthObservationResult/opt:cloudCoverPercentage";
        XPathExpression expression = this.xpath.compile(xpathString);
        return (String) expression.evaluate(entryNode, XPathConstants.STRING);
    }


    /**
     * Returns the identifier/datasetID of the sentinel layer
     *
     * @param entryNode the xml document which contains the metadata of one sentinel product
     * @return identifier of a sentinel layer
     */
    public String getIdentifier(Node entryNode) throws XPathExpressionException {
        String xpathString = "./opt:EarthObservation/eop:metaDataProperty/eop:EarthObservationMetaData/eop:identifier";
        XPathExpression expression = this.xpath.compile(xpathString);
        return (String) expression.evaluate(entryNode, XPathConstants.STRING);
    }

    /**
     * Returns the Date of recording as a list of two dates which are equal
     *
     * @param entryNode One specific sentinel Product which corresponds to an <entry>-Tag
     * @return DateTime list which contains the start and enddate
     * @throws XPathExpressionException
     */
    public List<DateTime> getTimeFrame(Node entryNode) throws XPathExpressionException {
        List<DateTime> result = new ArrayList<>();

        String xPathString = "./dc:date";
        XPathExpression expression = this.xpath.compile(xPathString);
        NodeList nodeList = (NodeList) expression.evaluate(entryNode, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String[] timeFrame = nodeList.item(i).getTextContent().split("/");
            for (String s : timeFrame) {
                DateTime date = DateTime.parse(s, FORMATTER);
                result.add(date);
            }
        }

        return result;
    }

    /**
     * Returns the Bounding Box of the product
     *
     * @param entryNode One specific sentinel Product which corresponds to an <entry>-Tag
     * @return Bounding Box of the sentinel product - Schema [minLat, minLon, maxLat, maxLon]
     * @throws XPathExpressionException
     */
    public List<Float> getBbox(Node entryNode) throws XPathExpressionException {
        ArrayList<Float> bbox = new ArrayList<>();
        String xPathString = "./georss:box";
        XPathExpression expression = this.xpath.compile(xPathString);
        NodeList nodeList = (NodeList) expression.evaluate(entryNode, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            String[] bboxCoordinates = nodeList.item(i).getTextContent().split(" ");
            for (String bboxCoordinate : bboxCoordinates) {
                bbox.add(Float.parseFloat(bboxCoordinate));
            }
        }

        return bbox;
    }

    /**
     * Delivers the number of pages of the product.
     *
     * @param responseDoc requested document
     * @return number of pages (int)
     * @throws XPathExpressionException
     */
    public int getNumberOfPages(Document responseDoc) throws XPathExpressionException {
        String xPathString = "/a:feed/os:totalResults";
        XPathExpression expression = this.xpath.compile(xPathString);
        int totalResults = (int) ((double) expression.evaluate(responseDoc, XPathConstants.NUMBER));
        return numberOfPagesCalculation(totalResults);
    }

    /**
     * Calculates the number of pages. Each page contains 50 <entry>-Tags
     *
     * @param totalResults number of total <entry>-Tags
     * @return number of pages (int)
     */
    private int numberOfPagesCalculation(int totalResults) {
        int modulo = totalResults % ITEMS_PER_PAGE;
        int nop = (totalResults - modulo) / ITEMS_PER_PAGE;
        if (modulo > 0)
            nop += 1;
        return nop;
    }

}
