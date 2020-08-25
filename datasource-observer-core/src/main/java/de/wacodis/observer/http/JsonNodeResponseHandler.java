package de.wacodis.observer.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * HTTP response handler for parsing a {@link JsonNode} from a HTTP response
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class JsonNodeResponseHandler implements ResponseHandler<JsonNode> {
    private final static Logger LOG = LoggerFactory.getLogger(JsonNodeResponseHandler.class);

    private ObjectMapper mapper;

    public JsonNodeResponseHandler() {
        mapper = new ObjectMapper();
    }

    @Override
    public JsonNode handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return mapper.readTree(entity.getContent());
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }
}
