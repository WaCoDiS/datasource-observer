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
package de.wacodis.observer.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.wacodis.observer.model.AbstractDataEnvelopeAreaOfInterest;
import de.wacodis.observer.model.WacodisJobDefinition;

@Component
public class BboxHelper {
	
	private Logger logger = LoggerFactory.getLogger(BboxHelper.class);
	
	@Autowired
	private List<JobFactory> jobFactories;
	
	public BboxHelper () {
		
	};
	
	public BboxHelper (List<JobFactory> jobFactories) {
		this.jobFactories = jobFactories;
	};

	public String appendAreaOfInterestToQuartzJobKey(WacodisJobDefinition job, String jobId) {
		List<Float> areaOfInterestExtent = job.getAreaOfInterest().getExtent();
		String extent = getBboxStringFromAreaOfInterest(areaOfInterestExtent);
		jobId = String.join("_", jobId, extent);
		
		return jobId;
	}

	public String getBboxStringFromAreaOfInterest(List<Float> areaOfInterestExtent) {
		String extent = areaOfInterestExtent.stream()
		        .map(String::valueOf)
		        .collect(Collectors.joining(","));
		return extent;
	}

	public String getQuartzKeyPartWithoutBbox(JobDetail jobDetailCandidate) {
		// BBOX is appended to jobId as "_<BBOX>"
		// hence get last index of "_" to create substring
		
		JobKey key = jobDetailCandidate.getKey();
		String name = key.getName();
		
		int lastIndex = name.lastIndexOf("_");
		
		String substring = name.substring(0, lastIndex);
		
		return substring;
	}

	public boolean anyQuartzJobIntersectsNewWacodisJobBBOX(List<JobKey> possibleQuartzJobKeys,
			JobDetail jobDetailCandidate) {
		boolean intersects = false;
		
		try {
			String bbox_newWacodisJob = getBboxSubstringFromQuartzJobKey(jobDetailCandidate.getKey());
			
			for (JobKey jobKey : possibleQuartzJobKeys) {
				try {
					String quartzJobCandidateBboxString = getBboxSubstringFromQuartzJobKey(jobKey);
					
					if(intersects(bbox_newWacodisJob, quartzJobCandidateBboxString)) {
						intersects = true;
						break;
					}
					
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
				
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}		
		
		return intersects;
	}

	private boolean intersects(String bboxString_newWacodisJob, String bboxString_quartzJob) {
		// input strings follow pattern: "minLon,minLat,maxLon,maxLat" in EPSG:4326
		
		logger.debug("Spatially compare BBOXes {} and {}", bboxString_quartzJob, bboxString_newWacodisJob);
		
		// an intersection of two BBOXes is only true, if BBoxes are not disjoint and actually touch each other
		
		String[] bbox_wacodisJob_stringSplit = bboxString_newWacodisJob.split(",");
		String[] bbox_quartzJob_stringSplit = bboxString_quartzJob.split(",");
		
		// minLon, minLat, maxLon, maxLat
		Float[] bbox_wacodisJob = toFloatArray(bbox_wacodisJob_stringSplit);
		Float[] bbox_quartzJob = toFloatArray(bbox_quartzJob_stringSplit);
		
		return ! ( bbox_quartzJob[0] >= bbox_wacodisJob[2] || bbox_quartzJob[2] <= bbox_wacodisJob[0] || bbox_quartzJob[3] <= bbox_wacodisJob[1] || bbox_quartzJob[1] >= bbox_wacodisJob[3]);		
	}

	private Float[] toFloatArray(String[] bbox_stringSplit) {
		Float[] array = new Float[bbox_stringSplit.length];
		
		for (int i = 0; i < bbox_stringSplit.length; i++) {
			array[i] = Float.parseFloat(bbox_stringSplit[i]);
		}
		
		return array;
	}

	public String getBboxSubstringFromQuartzJobKey(JobKey key) throws Exception {
		// BBOX is appended to jobId as "_<BBOX>" where "<BBOX>" == "minLon,minLat,maxLon,maxLat"
		// hence get last index of "_" to create substring
		String name = key.getName();

		int lastIndex = name.lastIndexOf("_");

		if(lastIndex != -1) {
			return name.substring(lastIndex+1);
		}
		else {
			throw new Exception("The Quartz JobKey '" + key.toString() + "' does not include a BBOX extent.");
		}
	}

	public JobKey findQuartzJobKeyIntersectingNewWacodisJobBBOX(List<JobKey> possibleQuartzJobKeys,
			JobDetail jobDetailCandidate) {
		JobKey result = null;
		try {
			String bbox_newWacodisJob = getBboxSubstringFromQuartzJobKey(jobDetailCandidate.getKey());
			
			for (JobKey jobKey : possibleQuartzJobKeys) {
				try {
					String quartzJobCandidateBboxString = getBboxSubstringFromQuartzJobKey(jobKey);
					
					if(intersects(bbox_newWacodisJob, quartzJobCandidateBboxString)) {
						result = jobKey;
						break;
					}
					
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
				
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}	
	
		return result;
	}

	public JobDetail expandBboxOfExistingQuartzJob(JobDetail existingQuartzJob, JobDetail jobDetail, String aoiKey) throws Exception {
		String bboxSubstring_wacodisJob = getBboxSubstringFromQuartzJobKey(jobDetail.getKey());
		String bboxSubstring_quartzJob = getBboxSubstringFromQuartzJobKey(existingQuartzJob.getKey());
		
		String expandedBbox = getExpandedBbox(bboxSubstring_quartzJob, bboxSubstring_wacodisJob);
		
		JobDetail newJobDetail = createNewQuartzJobWithExpandedBbox(existingQuartzJob, expandedBbox, aoiKey);
		
		return newJobDetail;
	}

	private JobDetail createNewQuartzJobWithExpandedBbox(JobDetail existingQuartzJob, String expandedBbox, String aoiKey) {
		String quartzKeyPartWithoutBbox = getQuartzKeyPartWithoutBbox(existingQuartzJob);
		String newJobKeyName = String.join("_", quartzKeyPartWithoutBbox, expandedBbox);
		
		JobDataMap jobDataMap = existingQuartzJob.getJobDataMap();
		
		JobDetail newJobDetail = existingQuartzJob.getJobBuilder().withIdentity(newJobKeyName, existingQuartzJob.getKey().getGroup()).usingJobData(jobDataMap).storeDurably(true).build();
		newJobDetail = modifyJobDetailBboxForJobExecution(newJobDetail, expandedBbox, aoiKey);
		return newJobDetail;
	}

	private JobDetail modifyJobDetailBboxForJobExecution(final JobDetail jobDetail, String expandedBbox, String aoiKey) {
		
		// can only have one item, as jobDetail corresponds to exaclty one JobFactory
		List<JobFactory> factories = jobFactories.stream()
        .filter(jf -> jf.supportsJobDetail(jobDetail))
        .collect(Collectors.toList());
		
		factories.get(0).modifyBboxParameter(jobDetail, expandedBbox);
		
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		
		//data.put(AOI_KEY, job.getAreaOfInterest());
		AbstractDataEnvelopeAreaOfInterest areaOfInterest = new AbstractDataEnvelopeAreaOfInterest();
		Float[] extent = toFloatArray(expandedBbox.split(","));		
		areaOfInterest.setExtent(Arrays.asList(extent));
		jobDataMap.put(aoiKey, areaOfInterest);
		
		return jobDetail;		
	}

	private String getExpandedBbox(String bboxSubstring_quartzJob, String bboxSubstring_wacodisJob) {
		
		logger.debug("Create expanded BBOX from input BBOXes {} and {}", bboxSubstring_quartzJob, bboxSubstring_wacodisJob);
		
		String[] bbox_wacodisJob_stringSplit = bboxSubstring_wacodisJob.split(",");
		String[] bbox_quartzJob_stringSplit = bboxSubstring_quartzJob.split(",");
		
		// minLon, minLat, maxLon, maxLat
		Float[] bbox_wacodisJob = toFloatArray(bbox_wacodisJob_stringSplit);
		Float[] bbox_quartzJob = toFloatArray(bbox_quartzJob_stringSplit);
		
		Float[] bbox_expanded = bbox_quartzJob;
		
		// minLon
		if(bbox_wacodisJob[0] < bbox_expanded[0]) {
			bbox_expanded[0] = bbox_wacodisJob[0];
		}
		// minLat
		if(bbox_wacodisJob[1] < bbox_expanded[1]) {
			bbox_expanded[1] = bbox_wacodisJob[1];
		}
		// maxLon
		if(bbox_wacodisJob[2] > bbox_expanded[2]) {
			bbox_expanded[2] = bbox_wacodisJob[2];
		}
		// maxLat
		if(bbox_wacodisJob[3] > bbox_expanded[3]) {
			bbox_expanded[3] = bbox_wacodisJob[3];
		}
		
		String extent = bbox_expanded[0] + "," + bbox_expanded[1] + "," + bbox_expanded[2] + "," + bbox_expanded[3];
		
		return extent;
	}

	public void addWacodisJobIdAndBBOXToJobDataMap(JobDetail quartzJob, UUID wacodisJobId,
			AbstractDataEnvelopeAreaOfInterest areaOfInterest) {
		String bbox = this.getBboxStringFromAreaOfInterest(areaOfInterest.getExtent());
		logger.info("Associated WACODIS job management: add WACODIS job ID '{}' and BBOX '{}' to the quartz job with key '{}'. Set wocodisJobId as map key and the BBOX string as map value.", wacodisJobId, bbox, quartzJob.getKey());

        quartzJob.getJobDataMap().put(wacodisJobId.toString(), bbox);	
	}
	
	public String removeWacodisJobIdAndBBOXFromJobDataMap(JobDetail quartzJob, UUID wacodisJobId) {
		JobDataMap jobDataMap = quartzJob.getJobDataMap();
		Object bbox = jobDataMap.get(wacodisJobId.toString());
		logger.info("Associated WACODIS job management: remove WACODIS job ID '{}' and its BBOX '{}' from the quartz job with key '{}'. ", wacodisJobId, bbox, quartzJob.getKey());
        jobDataMap.remove(wacodisJobId.toString());	
        
        return (String) bbox;
	}

	public JobDetail regenerateBboxForQuartzJob(JobDetail quartzJob, UUID wacodisJobId, 
			HashSet<UUID> remainingWacodisJobIds, String aoiKey) {
		
		JobDataMap jobDataMap = quartzJob.getJobDataMap();
		
		// get all relevant and existing BBOX strings
		// then create merged BBOXes
		// make new quartzJob from the other with the updated jobDataMap
		List<String> bboxes = new ArrayList<String>();
		
		for (UUID remainingWacodisJobId : remainingWacodisJobIds) {
			if(jobDataMap.containsKey(remainingWacodisJobId.toString())) {
				bboxes.add((String) jobDataMap.get(remainingWacodisJobId.toString()));
			}
		}
		
		String newBbox = bboxes.get(0);
		
		if(bboxes.size() > 1) {
			for (int i = 1; i < bboxes.size(); i++) {
				newBbox = getExpandedBbox(newBbox, bboxes.get(i));
			}
		}
		
		JobDetail newJobDetail = createNewQuartzJobWithExpandedBbox(quartzJob, newBbox, aoiKey);
		
		return newJobDetail;
	}
	
	

}
