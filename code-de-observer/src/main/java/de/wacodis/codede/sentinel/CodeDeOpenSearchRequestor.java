package de.wacodis.codede.sentinel;

import de.wacodis.codede.sentinel.exception.HttpConnectionException;
import de.wacodis.observer.decode.DecodingException;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for requesting CODE-DE Orthorectified images of the Sentinal satellites.
 *
 * @author <a href="mailto:tim.kurowski@hs-bochum.de">Tim Kurowski</a>
 * @author <a href="mailto:christian.koert@hs-bochum.de">Christian Koert</a>
 */
@Component
public class CodeDeOpenSearchRequestor {

    private final static Logger LOG = LoggerFactory.getLogger(CodeDeOpenSearchRequestor.class);

    /**
     * Performs a query with the given paramerters.
     *
     * @param params all necessary parameters for the OpenSearch request
     * @return metadata for the found satellite images
     * @throws DecodingException
     * @throws HttpConnectionException
     */
    public List<CodeDeProductsMetadata> request(CodeDeRequestParams params) throws DecodingException, HttpConnectionException {
        try {
            CodeDeResponseResolver resolver = new CodeDeResponseResolver();
            int pages = 1;
            List<CodeDeProductsMetadata> productsMetadata = new ArrayList<>();    // result
            for (int k = 1; k <= pages; k++) {
                LOG.info("Building connection parameters for the " + k + ". GET-request");
                String getRequestUrl = CodeDeOpenSearchRequestorBuilder.buildGetRequestUrl(params, k);
                LOG.info("Request CODE-DE API: {}", getRequestUrl);

                InputStream inputStream = sendOpenSearchRequest(getRequestUrl);
                Document getResponseDoc = resolver.getDocument(inputStream);
                LOG.debug("CODE-DE response document: {}", getResponseDoc.getTextContent());

                if (k == 1) {
                    pages = resolver.getNumberOfPages(getResponseDoc);
                }
                NodeList nodeList = resolver.getEntryNodes(getResponseDoc);

                for (int i = 0; i < nodeList.getLength(); i++) {
                    CodeDeProductsMetadata metadataObject = new CodeDeProductsMetadata();
                    Node node = nodeList.item(i);

                    String downloadLink = resolver.getDownloadLink(node);
                    float cloudCoverage = resolver.getCloudCoverage(node);
                    String identifier = resolver.getIdentifier(node);
                    List<DateTime> timeFrame = resolver.getTimeFrame(node);
                    List<Float> bbox = resolver.getBbox(node);

                    metadataObject.setDownloadLink(downloadLink);
                    metadataObject.setCloudCover(cloudCoverage);
                    metadataObject.setDatasetId(identifier);
                    metadataObject.setStartDate(timeFrame.get(0));
                    metadataObject.setEndDate(timeFrame.get(1));
                    metadataObject.setBbox(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
                    productsMetadata.add(metadataObject);
                }

            }
            return productsMetadata;
        } catch (XPathExpressionException | SAXException e) {
            throw new DecodingException("Could not process OpenSearch response", e);
        } catch (IOException e) {
            throw new HttpConnectionException("Connection to server failed", e);
        }
    }

    /**
     * Delivers the content of the GET response.
     *
     * @param getRequestUrl string containing the URL of the GET request
     * @return content of the GET response as an Inputstream
     * @throws ClientProtocolException
     * @throws IOException
     */
    private InputStream sendOpenSearchRequest(String getRequestUrl) throws ClientProtocolException, IOException {

        // contact http-client
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(getRequestUrl);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity(); // fill http-Object (status, parameters, content)
        return entity.getContent();
    }

}