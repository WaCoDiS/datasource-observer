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
package de.wacodis.sensorweb.encode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityRequest;
import org.n52.svalbard.encode.AbstractXmlEncoder;
import org.n52.svalbard.encode.Encoder;
import org.n52.svalbard.encode.EncoderRepository;
import org.n52.svalbard.encode.GetDataAvailabilityRequestEncoder;
import org.n52.svalbard.encode.SchemaRepository;
import org.n52.svalbard.encode.exception.EncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.x2003.x05.soapEnvelope.Body;
import org.w3.x2003.x05.soapEnvelope.Envelope;
import org.w3.x2003.x05.soapEnvelope.EnvelopeDocument;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

public class GetDataAvailabilityReqEncoder implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(GetDataAvailabilityReqEncoder.class.getName());

	public String encode(List<String> procedures, List<String> observedProperties,
			List<String> offerings, List<String> featureIdentifiers) throws EncodingException {

		// set up the request
		GetDataAvailabilityRequest request = new GetDataAvailabilityRequest();
		request.setVersion("2.0.0");
		request.setNamespace("1.0");
		request.setProcedure(Lists.newArrayList(procedures));
		request.setObservedProperty(Lists.newArrayList(observedProperties));
		request.setOfferings(Lists.newArrayList(offerings));
		request.setFeatureIdentifiers(Lists.newArrayList(featureIdentifiers));
		request.setResponseFormat("http://www.opengis.net/om/2.0");

		// set up the encoders
		GetDataAvailabilityRequestEncoder getDatEncoder = new GetDataAvailabilityRequestEncoder();
		prepareEncoders(getDatEncoder);
		
		// schema repository is mandatory for XML encoding
		getDatEncoder.setSchemaRepository(new SchemaRepository());

		// finally, encode the request
		XmlObject encoded = getDatEncoder.encode(request);

		// build soapRequest
		EnvelopeDocument envDoc = EnvelopeDocument.Factory.newInstance();
		Envelope env = envDoc.addNewEnvelope();
		Body body = env.addNewBody();
		body.set(encoded);

		LOG.info(envDoc.xmlText(new XmlOptions().setSavePrettyPrint()));
		return envDoc.xmlText();
	}
	
	private void prepareEncoders(AbstractXmlEncoder<?, ?>... encoders) {
		Supplier<XmlOptions> xoptions = Suppliers.ofInstance(new XmlOptions().setSavePrettyPrint());
		EncoderRepository encoderRepository = new EncoderRepository();
		List<Encoder<?, ?>> encoderList = new ArrayList<Encoder<?, ?>>();
		for(AbstractXmlEncoder<?, ?> e : encoders) {
			// http://www.opengis.net/om/2.0 - xmlOptions is mandatory for XML encoding
			e.setXmlOptions(xoptions);
			// encoder repository is used to search for (internally used) encoders
			// we need to add the encoders and initialize the repository
			encoderList.add(e);
			e.setEncoderRepository(encoderRepository);
		}
		encoderRepository.setEncoders(encoderList);
		encoderRepository.init();
	}

}
