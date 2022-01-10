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
import de.wacodis.sentinel.apihub.ProductMetadata;
import de.wacodis.sentinel.apihub.SearchResult;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Matthes Rieke (m.rieke@52north.org)
 */
public class FeedDecoderTest {

    @Test
    public void testDecoding() throws ParserConfigurationException, SAXException, IOException, DecodingException {
        FeedDecoder decoder = new FeedDecoder();
        Document doc = readXml("/os-response.xml");
        SearchResult result = decoder.parse(doc);
        
        Assert.assertThat(result.getTotalResults(), CoreMatchers.is(96));
        Assert.assertThat(result.getItemsPerPage(), CoreMatchers.is(10));
        Assert.assertThat(result.getStartIndex(), CoreMatchers.is(0));
        
        List<ProductMetadata> p = result.getProducts();
        Assert.assertThat(p.size(), CoreMatchers.is(10));
        ProductMetadata p1 = p.get(0);
        Assert.assertThat(p1.getId(), CoreMatchers.equalTo("62cb5ca1-b1c5-4ae9-9ea2-f13f90300490"));
        Assert.assertThat(p1.getTitle(), CoreMatchers.equalTo("S2A_MSIL2A_20190130T103251_N0211_R108_T31UGS_20190130T115213"));
        Assert.assertThat(p1.getInstrumentShortName(), CoreMatchers.equalTo("MSI"));
        Assert.assertThat(p1.getIngestionDate(), CoreMatchers.equalTo(new DateTime("2019-01-30T20:05:43.4Z")));
        Assert.assertThat(p1.getCloudCoverPercentage(), CoreMatchers.equalTo(86.171287));
        Assert.assertThat(p1.getFootprintWkt(), CoreMatchers.equalTo("POLYGON ((6.576402776126024 50.406238167275816,6.628851075629272"
                + " 50.536822596839706,6.686823508496596 50.68300782013704,6.745473100900398 50.82898642785132,6.804652027737079"
                + " 50.97486399443909,6.864109136690101 51.120703959269576,6.924048041127735 51.26647095616419,6.971322514095254"
                + " 51.38160373804328,7.450589699013681 51.366602012514676,7.357621898236907 50.38212225093181,6.576402776126024 50.406238167275816))"));
        Assert.assertThat(p1.getGmlFootprint(), CoreMatchers.equalTo("<gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\">\n" +
                "   <gml:outerBoundaryIs>\n" +
                "      <gml:LinearRing>\n" +
                "         <gml:coordinates>50.406238167275816,6.576402776126024 50.536822596839706,6.628851075629272 50.68300782013704," +
                "6.686823508496596 50.82898642785132,6.745473100900398 50.97486399443909,6.804652027737079 51.120703959269576," +
                "6.864109136690101 51.26647095616419,6.924048041127735 51.38160373804328,6.971322514095254 51.366602012514676," +
                "7.450589699013681 50.38212225093181,7.357621898236907 50.406238167275816,6.576402776126024</gml:coordinates>\n" +
                "      </gml:LinearRing>\n" +
                "   </gml:outerBoundaryIs>\n" +
                "</gml:Polygon>"));

        Assert.assertThat(p1.getPlatformName(), CoreMatchers.equalTo("Sentinel-2"));
        Assert.assertThat(p1.getProcessingLevel(), CoreMatchers.equalTo("Level-2A"));
        Assert.assertThat(p1.getProductType(), CoreMatchers.equalTo("S2MSI2A"));
        Assert.assertNull(p1.getSensorMode());
        Assert.assertThat(p1.resolveBbox(), CoreMatchers.notNullValue());
        Envelope bbox = p1.resolveBbox();
        Assert.assertThat(bbox.getMaxX(), CoreMatchers.is(7.450589699013681));
        Assert.assertThat(bbox.getMaxY(), CoreMatchers.is(51.38160373804328));
        Assert.assertThat(bbox.getMinX(), CoreMatchers.is(6.576402776126024));
        Assert.assertThat(bbox.getMinY(), CoreMatchers.is(50.38212225093181));
        
        p1 = p.get(1);
        Assert.assertThat(p1.getId(), CoreMatchers.equalTo("e67b0ebf-b60d-49f6-b665-5ff99adcac60"));
        Assert.assertThat(p1.getTitle(), CoreMatchers.equalTo("S2A_MSIL2A_20190130T103251_N0211_R108_T32UMB_20190130T115213"));
        Assert.assertThat(p1.getInstrumentShortName(), CoreMatchers.equalTo("MSI"));
        Assert.assertThat(p1.getCloudCoverPercentage(), CoreMatchers.equalTo(98.053456));

    }

    @Test
    public void testDecodingEmptyResponse() throws ParserConfigurationException, SAXException, IOException, DecodingException {
        FeedDecoder decoder = new FeedDecoder();
        Document doc = readXml("/os-response-empty.xml");
        SearchResult result = decoder.parse(doc);
        
        Assert.assertThat(result.getTotalResults(), CoreMatchers.is(0));
        Assert.assertThat(result.getItemsPerPage(), CoreMatchers.is(0));
        Assert.assertThat(result.getStartIndex(), CoreMatchers.is(0));
        
        Assert.assertThat(result.getProducts().size(), CoreMatchers.is(0));
    }
    
    private Document readXml(String osresponsexml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        return fac.newDocumentBuilder().parse(getClass().getResourceAsStream(osresponsexml));
    }
    
}
