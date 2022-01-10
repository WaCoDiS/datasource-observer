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
package de.wacodis.sentinel.apihub;

import de.wacodis.observer.decode.DecodingException;
import de.wacodis.sentinel.apihub.decode.FeedDecoder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author matthes
 */
public class ApiHubResponseConverter implements HttpMessageConverter<SearchResult> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApiHubResponseConverter.class);

    private final FeedDecoder decoder;
    private final DocumentBuilderFactory factory;

    public ApiHubResponseConverter() {
        this.factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        this.decoder = new FeedDecoder();
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.equals(SearchResult.class)
                && (mediaType == null || mediaType.isCompatibleWith(MediaType.APPLICATION_XML)
                || mediaType.isCompatibleWith(MediaType.TEXT_XML)
                || mediaType.isCompatibleWith(MediaType.APPLICATION_ATOM_XML));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_ATOM_XML,
                MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public SearchResult read(Class<? extends SearchResult> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputMessage.getBody());
            return this.decoder.parse(doc);
        } catch (SAXException | ParserConfigurationException ex) {
            LOG.warn(ex.getMessage());
            throw new IOException("Could not parse XML document", ex);
        } catch (DecodingException ex) {
            LOG.warn(ex.getMessage());
            throw new IOException("Could not process contents of XML document", ex);
        }
    }

    @Override
    public void write(SearchResult t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
    }
    
}
