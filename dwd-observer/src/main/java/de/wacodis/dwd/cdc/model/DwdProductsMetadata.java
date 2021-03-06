/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.wacodis.dwd.cdc.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;


/**
 * Metadata for DWD station products
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public class DwdProductsMetadata {

	private String serviceUrl;
	private String layerName;
	private String parameter;
	private Envelope envelope;
	private DateTime startDate;
	private DateTime endDate;
	
	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	
	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}

	public DateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	public DateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}	
	
}
