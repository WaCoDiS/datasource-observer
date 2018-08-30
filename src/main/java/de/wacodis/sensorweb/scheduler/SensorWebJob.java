package de.wacodis.sensorweb.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.exception.EncodingException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.minlog.Log;

import de.wacodis.sensorweb.observer.ObservationObserver;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SensorWebJob implements Job{
	
	
	private static final Logger log = LoggerFactory.getLogger(SensorWebJob.class);

	private DateTime date;	//fake date in past for test
	
	private final String FLUGGS_URL = "http://fluggs.wupperverband.de/sos2/sos/soap";
	private final String N52_URL = "http://sensorweb.demo.52north.org/52n-sos-webapp/service";
	
	private List<String> procedures, observedProperties, offerings, featureIdentifiers;
	private DateTime dateOfLastObs, dateOfNextToLastObs;
	

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		procedures = (ArrayList<String>) context.getMergedJobDataMap().get("procedures");
		observedProperties = (ArrayList<String>) context.getMergedJobDataMap().get("observedProperties");
		offerings = (ArrayList<String>) context.getMergedJobDataMap().get("offerings");
		featureIdentifiers = (ArrayList<String>) context.getMergedJobDataMap().get("featureIdentifiers");
		ObservationObserver observer = new ObservationObserver(N52_URL, procedures, observedProperties, offerings, featureIdentifiers);

		try {
			log.info("called SensorWebJob's execute()");
			log.info("SensorWebJob's Observer = {}", observer != null);
			
			dateOfLastObs = (DateTime) context.getMergedJobDataMap().get("dateOfLastObs");
			dateOfNextToLastObs = (DateTime) context.getMergedJobDataMap().get("dateOfNextToLastObs");
			
			if(dateOfLastObs != null && dateOfNextToLastObs != null) {
				observer.setDateOfLastObs(dateOfLastObs);
				observer.setDateOfNextToLastObs(dateOfNextToLastObs);
			} else {
				observer.setDateOfLastObs(new DateTime(2012, 11, 19, 12, 0, 0));		//for test only				
			}
			
			
			if(observer.checkForAvailableUpdates()) {
				dateOfLastObs = observer.getDateOfLastObs();
				dateOfNextToLastObs = observer.getDateOfNextToLastObs();
				observer.updateObservations(dateOfNextToLastObs, dateOfLastObs);
				List<OmObservation> results = observer.getObservations();
				//-->... notify broker?
				for(OmObservation o : results) {
					System.out.println(o.getValue().getValue());
				}
				//<--
			}
			
			JobDataMap data = context.getJobDetail().getJobDataMap();
			data.put("dateOfLastObs", dateOfLastObs);
			data.put("dateOfNextToLastObs", dateOfNextToLastObs);
			
		} catch (EncodingException | DecodingException | XmlException e) {
			log.info(e.getMessage(), e);
		}
		
	}
	
	public void setDate(DateTime date) {		//for test only
		this.date = date;
	}

	public void setProcedures(List<String> procedures) {
		this.procedures = procedures;
	}

	public void setObservedProperties(List<String> observedProperties) {
		this.observedProperties = observedProperties;
	}

	public void setOfferings(List<String> offerings) {
		this.offerings = offerings;
	}

	public void setFeatureIdentifiers(List<String> featureIdentifiers) {
		this.featureIdentifiers = featureIdentifiers;
	}
	
	

	
	
}
