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
package de.wacodis.observer.quartz;

import java.text.ParseException;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent;
import de.wacodis.observer.model.AbstractWacodisJobExecutionEvent.EventTypeEnum;
import de.wacodis.observer.model.WacodisJobDefinition;
import de.wacodis.observer.model.WacodisJobDefinitionExecution;
import de.wacodis.observer.model.WacodisJobDefinitionTemporalCoverage;
import exception.InvalidWacodisJobParameterException;

/**
 * Used to validate incoming Wacodis job definitions with respect to temporal coverage and execution settings
 * @author CDB
 *
 */
public class WacodisJobValidator {

	private static final Logger LOG = LoggerFactory.getLogger(WacodisJobValidator.class);
	
	public static void validateJobParameters(WacodisJobDefinition job) throws Exception{
		// check job parameters to ensure that all necessary parameters are set (depending on the type of job)
    	LOG.info("Start validation of WacodisJobDefinition.");
		validateTemporalCoverage(job);
		
		LOG.info("WacodisJobDefinition validation succeeded without issues.");
	}

	public static void validateTemporalCoverage(WacodisJobDefinition job) throws InvalidWacodisJobParameterException {
		// bei SingleExecutionEvent muss duration angegeben werden
		//  wenn previousExecution==true muss execution pattern angegeben werden
		
		WacodisJobDefinitionTemporalCoverage temporalCoverage = job.getTemporalCoverage();
		WacodisJobDefinitionExecution execution = job.getExecution();
		AbstractWacodisJobExecutionEvent event = execution.getEvent();
		if(event != null && event.getEventType().equals(EventTypeEnum.SINGLEJOBEXECUTIONEVENT)) {
			// make sure that duration property is set
			String durationString = temporalCoverage.getDuration();
			if(durationString == null || durationString.isEmpty()) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type '" + EventTypeEnum.SINGLEJOBEXECUTIONEVENT + "' is missing required parameter value for parameter 'temporalCoverage.duration'");
			}
			if(! isValidIso8601DurationString(durationString)) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type '" + EventTypeEnum.SINGLEJOBEXECUTIONEVENT + "' has invalid parameter value for parameter 'temporalCoverage.duration'. The value of '" + durationString + "' cannot be parsed as ISO8601 duration");				
			}
		}
		else if (temporalCoverage.getPreviousExecution() != null && temporalCoverage.getPreviousExecution()) {
			// make sure that pattern execution is set
			String patternString = execution.getPattern();
			if(patternString == null || patternString.isEmpty()) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type 'pattern execution' with 'previousExecution=true' is missing required parameter value for parameter 'execution.pattern'");
			}
			if(! isValidCronPatternString(patternString)) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type 'pattern execution' with 'previousExecution=true' has invalid parameter value for parameter 'execution.pattern'. The value of '" + patternString + "' cannot be parsed as CRON pattern");				
			}
		}
		// case no previosExecution and not event of type SingleTimeExecution
		else if ((temporalCoverage.getPreviousExecution() == null || !temporalCoverage.getPreviousExecution()) && (event == null || !event.getEventType().equals(EventTypeEnum.SINGLEJOBEXECUTIONEVENT))) {
			// make sure that pattern execution is set
			String patternString = execution.getPattern();
			if(patternString == null || patternString.isEmpty()) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type 'pattern execution' with 'previousExecution=false' is missing required parameter value for parameter 'execution.pattern'");
			}
			if(! isValidCronPatternString(patternString)) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type 'pattern execution' with 'previousExecution=false' has invalid parameter value for parameter 'execution.pattern'. The value of '" + patternString + "' cannot be parsed as CRON pattern");				
			}
			
			// make sure that duration property is set
			String durationString = temporalCoverage.getDuration();
			if(! isValidIso8601DurationString(durationString)) {
				throw new InvalidWacodisJobParameterException("Wacodis job of type 'pattern execution' with 'previousExecution=false' has invalid parameter value for parameter 'temporalCoverage.duration'. The value of '" + durationString + "' cannot be parsed as ISO8601 duration");				
			}
		}
	}

	public static boolean isValidCronPatternString(String patternString) {
		boolean isValidCronPattern = false;
		
		try {
			CronExpression cronExpr = createQuartzCronSchedule(patternString);
			isValidCronPattern = true;
		} catch (Exception e) {
			LOG.debug("Error on parsing patternString '{}' as CRON pattern. Error message is:\n'{}'", patternString, e.getMessage());
		}
		
		return isValidCronPattern;
	}

	public static boolean isValidIso8601DurationString(String durationString) {
		// try parse duration String. Expect it to be ISO8601 duration format
		
		// according to https://stackoverflow.com/questions/39411811/parsing-iso-8601-duration-format-to-joda-duration-illegalargumentexception 
		// both possibilities, Duration and Period, should be checked
		boolean isValidIso8601Duration = false;
		try {
			Duration jodaDuration = Duration.parse(durationString);
			isValidIso8601Duration = true;
		} catch (Exception e) {
			LOG.debug("Error on parsing durationString '{}' as Joda Time Duration. Error message is:\n'{}'", durationString, e.getMessage());
		}
		
		try {
			Period jodaPeriod = Period.parse(durationString);
			isValidIso8601Duration = true;
		} catch (Exception e) {
			LOG.debug("Error on parsing durationString '{}' as Joda Time Period. Error message is:\n'{}'", durationString, e.getMessage());
		}
		
		return isValidIso8601Duration;
	}
	
	/**
     * Creates a Quartz cron expression from a unix UNIX cron expression pattern
     * in the default system timezone
     *
     * @param executionPattern the UNIX cron expression
     * @return cron expression for Quartz that matches the UNIX cron expression
     * @throws ParseException
     */
    public static CronExpression createQuartzCronSchedule(String executionPattern) throws ParseException {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        Cron cron = parser.parse(executionPattern);

        CronMapper cronMapper = CronMapper.fromUnixToQuartz();
        Cron quartzCron = cronMapper.map(cron);

        return new CronExpression(quartzCron.asString());
    }

	
}
