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
package de.wacodis.sensorweb.decode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.janmayen.Producer;
import org.n52.janmayen.Producers;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.DataAvailability;
import org.n52.svalbard.decode.AbstractXmlDecoder;
import org.n52.svalbard.decode.Decoder;
import org.n52.svalbard.decode.DecoderRepository;
import org.n52.svalbard.decode.GetDataAvailabilityResponseDecoder;
import org.n52.svalbard.decode.GmlDecoderv321;
import org.n52.svalbard.decode.exception.DecodingException;
import org.w3.x2003.x05.soapEnvelope.Body;
import org.w3.x2003.x05.soapEnvelope.Envelope;
import org.w3.x2003.x05.soapEnvelope.EnvelopeDocument;

import net.opengis.sosgda.x10.GetDataAvailabilityResponseDocument;

public class GetDataAvailabilityResDecoder implements Serializable{
	
	private static final long serialVersionUID = 1L;

	public List<DataAvailability> decode(String soapResponse)
			throws XmlException, DecodingException {
		
		// load SOAPResponse via XMLBeans
		EnvelopeDocument doc = EnvelopeDocument.Factory.parse(soapResponse);
		Envelope soapEnv = doc.getEnvelope();
		Body soapBody = soapEnv.getBody();
		GetDataAvailabilityResponseDocument getDatAvaResDoc = GetDataAvailabilityResponseDocument.Factory
				.parse(soapBody.xmlText());

		// use ResponseDecoder-Implementation(s)
		GetDataAvailabilityResponseDecoder getDatResDecoder = new GetDataAvailabilityResponseDecoder();
		// set up decoders
		prepareDecoders(getDatResDecoder, new GmlDecoderv321());
		// decode
		GetDataAvailabilityResponse getDatRes = getDatResDecoder.decode(getDatAvaResDoc);
		List<DataAvailability> dataList = getDatRes.getDataAvailabilities();
		
		return dataList;
	}
	
	private void prepareDecoders(AbstractXmlDecoder<?, ?>... decoders) {
		Producer<XmlOptions> xoptions = Producers.forInstance(new XmlOptions().setSavePrettyPrint());
		DecoderRepository decoderRepository = new DecoderRepository();
		List<Decoder<?, ?>> decoderList = new ArrayList<Decoder<?, ?>>();
		for(AbstractXmlDecoder<?, ?> d : decoders) {
			// http://www.opengis.net/om/2.0 - xmlOptions is mandatory for XML encoding
			d.setXmlOptions(xoptions);
			// decoder repository is used to search for (internally used) decoders
			// we need to add the decoders and initialize the repository
			decoderList.add(d);
			d.setDecoderRepository(decoderRepository);
		}
		decoderRepository.setDecoders(decoderList);
		decoderRepository.init();
	}

}
