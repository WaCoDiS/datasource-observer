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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wfs.x20.GetFeatureDocument;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class is responsible for requesting DWD FeatureServices for stationary
 * weather data.
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class DwdWfsRequestor {

	final static Logger LOG = LoggerFactory.getLogger(DwdWfsRequestor.class);

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
	public static DwdProductsMetadata request(String url, DwdWfsRequestParams params)
			throws IOException, ParserConfigurationException, SAXException {
		LOG.debug("Start Buildung Connection Parameters for WFS Service");
		String typeName = DwdWfsRequestorBuilder.TYPE_NAME_PREFIX + params.getTypeName();

		DwdWfsRequestorBuilder wfsRequest = new DwdWfsRequestorBuilder(params);
		LOG.debug("Start getFeature request");
		String getPostBody = wfsRequest.createGetFeaturePost().xmlText();
		InputStream getFeatureResponse = sendWfsRequest(url, getPostBody);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();
		Document doc = docBuilder.parse(getFeatureResponse);
	
		DwdResponseResolver resolver = new DwdResponseResolver();
		SpatioTemporalExtent timeAndBbox = resolver.generateSpatioTemporalExtent(doc, typeName);

		LOG.debug("Start getCapabilities request");
		String capPostBody = wfsRequest.createGetCapabilitiesPost().xmlText();
		InputStream capResponse = sendWfsRequest(url, capPostBody);
		
		// typename and clearname
		
		String[] featureClearName = resolver.requestTypeName(capResponse, typeName);

		LOG.debug("Building DwdProductsMetaData Object");
		// create DwdProductsMetaData
		DwdProductsMetadata metadata = new DwdProductsMetadata();
		metadata.setLayerName(featureClearName[0]);
		metadata.setParameter(featureClearName[1]);
		// bbox
		ArrayList<Float> extent = timeAndBbox.getbBox();
		metadata.setExtent(extent.get(0), extent.get(1), extent.get(2), extent.get(3));

		// timeframe
		ArrayList<DateTime> timeFrame = timeAndBbox.getTimeFrame();
		metadata.setStartDate(timeFrame.get(0));
		metadata.setEndDate(timeFrame.get(1));

		// serviceurl
		metadata.setServiceUrl(url);
		LOG.debug("End of request()-Method - Return DwdProductsMetaData Object");
		return metadata;
	}

	/**
	 * Delivers the post response depending on the outputformat (xml)
	 * 
	 * @param url serviceURL
	 * @param postRequest post message (xml)
	 * @return httpContent post response
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected static InputStream sendWfsRequest(String url, String postRequest)
			throws UnsupportedEncodingException, IOException, ClientProtocolException {
		// contact http-client
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader("content-type", "application/xml");
		// create PostMessage
		StringEntity entity = new StringEntity(postRequest);
		httpPost.setEntity(entity);
		HttpResponse response = httpclient.execute(httpPost);

		HttpEntity responseEntity = response.getEntity(); // fill http-Object (status, parameters, content)
		InputStream httpContent = responseEntity.getContent(); // ask for content
		return httpContent;
	}
}
