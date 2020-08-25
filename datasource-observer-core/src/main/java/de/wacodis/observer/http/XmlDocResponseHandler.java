package de.wacodis.observer.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * HTTP response handler for parsing a XML {@link Document} from the response
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class XmlDocResponseHandler implements ResponseHandler<Document> {

    private final static Logger LOG = LoggerFactory.getLogger(XmlDocResponseHandler.class);

    private DocumentBuilderFactory factory;

    public XmlDocResponseHandler() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    @Override
    public Document handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(entity.getContent());
            } catch (SAXException | ParserConfigurationException ex) {
                LOG.warn(ex.getMessage());
                throw new IOException("Could not parse XML document", ex);
            }
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }

}
