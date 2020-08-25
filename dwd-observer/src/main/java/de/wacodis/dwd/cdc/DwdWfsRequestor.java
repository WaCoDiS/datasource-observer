/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.wacodis.dwd.cdc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.wacodis.dwd.cdc.model.DwdProductsMetadata;
import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import de.wacodis.dwd.cdc.model.Envelope;
import de.wacodis.dwd.cdc.model.SpatioTemporalExtent;
import de.wacodis.observer.http.XmlDocResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class is responsible for requesting DWD FeatureServices for stationary
 * weather data.
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@Component
public class DwdWfsRequestor implements InitializingBean {

    final static Logger LOG = LoggerFactory.getLogger(DwdWfsRequestor.class);

    private DocumentBuilderFactory dbf;

    private CloseableHttpClient httpClient;

    @Autowired
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Performs a query with the given parameters
     *
     * @param url    DWD CDC FeatureService URL
     * @param params Paramaters for the FeatureService URL
     * @return metadata for the found stationary weather data
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public DwdProductsMetadata request(String url, DwdWfsRequestParams params)
            throws IOException, ParserConfigurationException, SAXException {
        DwdResponseResolver responseResolver = new DwdResponseResolver();
        String typeName = DwdWfsRequestorBuilder.TYPE_NAME_PREFIX + params.getTypeName();
        DwdProductsMetadata metadata = new DwdProductsMetadata();

        DwdWfsRequestorBuilder wfsRequest = new DwdWfsRequestorBuilder(params);

        String getPostBody = wfsRequest.createGetFeaturePost().xmlText();
        Document getFeatureDoc = sendWfsRequest(url, getPostBody);

        if (!responseResolver.responseContainsFeatureCollection(getFeatureDoc)) {
            return null;
        }
        SpatioTemporalExtent timeAndBbox = responseResolver.generateSpatioTemporalExtent(getFeatureDoc, typeName);

        String capPostBody = wfsRequest.createGetCapabilitiesPost().xmlText();
        Document getCapDoc = sendWfsRequest(url, capPostBody);

        if (responseResolver.responseContainsCapabilities(getCapDoc)) {
            // typename and clearname
            String[] featureClearName = responseResolver.requestTypeName(getCapDoc, typeName);

            metadata.setLayerName(featureClearName[0]);
            metadata.setParameter(featureClearName[1]);
        }

        // bbox
        Envelope envelope = timeAndBbox.getbBox();
        metadata.setEnvelope(envelope);

        // timeframe
        ArrayList<DateTime> timeFrame = timeAndBbox.getTimeFrame();
        metadata.setStartDate(timeFrame.get(0));
        metadata.setEndDate(timeFrame.get(1));

        // serviceurl
        metadata.setServiceUrl(url);

        return metadata;
    }

    /**
     * Delivers the post response depending on the outputformat (xml)
     *
     * @param url         serviceURL
     * @param requestBody post message (xml)
     * @return httpContent post response
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws ClientProtocolException
     */
    protected Document sendWfsRequest(String url, String requestBody)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {

        // create HTTP POST message body
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("content-type", "application/xml");

        StringEntity entity = new StringEntity(requestBody);
        httpPost.setEntity(entity);

        Document responseDoc = httpClient.execute(httpPost, new XmlDocResponseHandler());
        return responseDoc;

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dbf = DocumentBuilderFactory.newInstance();
    }
}
