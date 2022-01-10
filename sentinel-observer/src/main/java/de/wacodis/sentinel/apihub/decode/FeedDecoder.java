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
package de.wacodis.sentinel.apihub.decode;

import de.wacodis.observer.decode.DecodingException;
import de.wacodis.observer.decode.SimpleNamespaceContext;
import de.wacodis.sentinel.apihub.ProductMetadata;
import de.wacodis.sentinel.apihub.SearchResult;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.*;

/**
 *
 * @author matthes rieke
 */
public class FeedDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(FeedDecoder.class);

    private final XPath xpath;

    public FeedDecoder() {
        XPathFactory factory = XPathFactory.newInstance();
        this.xpath = factory.newXPath();
        Map<String, String> prefMap = new HashMap<String, String>() {
            {
                put("os", "http://a9.com/-/spec/opensearch/1.1/");
                put("a", "http://www.w3.org/2005/Atom");
            }
        };
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xpath.setNamespaceContext(namespaces);
    }

    public SearchResult parse(Document doc) throws DecodingException {
        try {
            XPathExpression expr = this.xpath.compile("/a:feed/os:totalResults");
            Object result = expr.evaluate(doc, XPathConstants.NUMBER);
            int total = (int) extractDouble(result);

            expr = this.xpath.compile("/a:feed/os:startIndex");
            result = expr.evaluate(doc, XPathConstants.NUMBER);
            int startIndex = (int) extractDouble(result);

            expr = this.xpath.compile("/a:feed/os:itemsPerPage");
            result = expr.evaluate(doc, XPathConstants.NUMBER);
            int itemsPerPage = (int) extractDouble(result);

            List<ProductMetadata> products = parseProducts(doc);

            SearchResult sr = new SearchResult();
            sr.setItemsPerPage(itemsPerPage);
            sr.setStartIndex(startIndex);
            sr.setTotalResults(total);
            sr.setProducts(products);

            return sr;
        } catch (XPathExpressionException ex) {
            LOG.warn(ex.getMessage());
            LOG.debug(ex.getMessage(), ex);
            throw new DecodingException("Could not process OpenSearch response", ex);
        }
    }

    private List<ProductMetadata> parseProducts(Document doc) throws XPathExpressionException {
        XPathExpression expr = this.xpath.compile("/a:feed/a:entry");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        if (result != null && result instanceof NodeList) {
            NodeList nodes = (NodeList) result;
            List<ProductMetadata> products = new ArrayList<>(nodes.getLength());
            
            XPathExpression titleExpr = this.xpath.compile("./a:title");
            XPathExpression idExpr = this.xpath.compile("./a:id");
            XPathExpression cloudCoverPercentageExpr = this.xpath.compile("./a:double[@name = 'cloudcoverpercentage']");
            XPathExpression instrumentShortNameExpr = this.xpath.compile("./a:str[@name = 'instrumentshortname']");
            XPathExpression beginPositionExpr = this.xpath.compile("./a:date[@name = 'beginposition']");
            XPathExpression endPositionExpr = this.xpath.compile("./a:date[@name = 'endposition']");
            XPathExpression ingestionDateExpr = this.xpath.compile("./a:date[@name = 'ingestiondate']");
            XPathExpression footprintExpr = this.xpath.compile("./a:str[@name = 'footprint']");
            XPathExpression platformExpr = this.xpath.compile("./a:str[@name = 'platformname']");
            XPathExpression sensorModeExpr = this.xpath.compile("./a:str[@name = 'sensoroperationalmode']");
            XPathExpression processLvlExpr = this.xpath.compile("./a:str[@name = 'processinglevel']");
            XPathExpression productTypeExpr = this.xpath.compile("./a:str[@name = 'producttype']");
            XPathExpression gmlFootprintExpr = this.xpath.compile("./a:str[@name = 'gmlfootprint']");
            
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                Object titleCandidate = titleExpr.evaluate(n, XPathConstants.STRING);
                Object idCandidate = idExpr.evaluate(n, XPathConstants.STRING);
                Object cloudCoverPercentageCandidate = cloudCoverPercentageExpr.evaluate(n, XPathConstants.NUMBER);
                Object instrumentShortNameCandidate = instrumentShortNameExpr.evaluate(n, XPathConstants.STRING);
                Object beginPositionCandidate = beginPositionExpr.evaluate(n, XPathConstants.STRING);
                Object endPositionCandidate = endPositionExpr.evaluate(n, XPathConstants.STRING);
                Object ingestionDateCandidate = ingestionDateExpr.evaluate(n, XPathConstants.STRING);
                Object footprintCandidate = footprintExpr.evaluate(n, XPathConstants.STRING);
                Object platformCandidate = platformExpr.evaluate(n, XPathConstants.STRING);
                Object sensorModeCandidate = sensorModeExpr.evaluate(n, XPathConstants.STRING);
                Object processLvlCandidate = processLvlExpr.evaluate(n, XPathConstants.STRING);
                Object productTypeCandidate = productTypeExpr.evaluate(n, XPathConstants.STRING);
                Object gmlFootprintCandidate = gmlFootprintExpr.evaluate(n, XPathConstants.STRING);
                
                ProductMetadata p = new ProductMetadata();                
                p.setTitle(extractString(titleCandidate));
                p.setId(extractString(idCandidate));
                p.setInstrumentShortName(extractString(instrumentShortNameCandidate));
                p.setCloudCoverPercentage(extractDouble(cloudCoverPercentageCandidate));
                p.setBeginPosition(extractDate(beginPositionCandidate));
                p.setEndPosition(extractDate(endPositionCandidate));
                p.setIngestionDate(extractDate(ingestionDateCandidate));
                p.setFootprintWkt(extractString(footprintCandidate));
                p.setPlatformName(extractString(platformCandidate));
                p.setSensorMode(extractString(sensorModeCandidate));
                p.setProcessingLevel(extractString(processLvlCandidate));
                p.setProductType(extractString(productTypeCandidate));
                p.setGmlFootprint(extractString(gmlFootprintCandidate));
                
                products.add(p);
            }
            
            return products;
        }
    
        return Collections.emptyList();
    }

    private String extractString(Object candidate) {
        if (!StringUtils.isEmpty(candidate)) {
            return candidate.toString();
        }
        
        return null;
    }

    private double extractDouble(Object candidate) {
        if (candidate != null && candidate instanceof Number) {
            return (Double) candidate;
        }
        
        return Double.NaN;
    }

    private DateTime extractDate(Object candidate) {
        String asStr = extractString(candidate);
        if (asStr != null) {
            return new DateTime(asStr);
        }
        
        return null;
    }

}
