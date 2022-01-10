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
package de.wacodis.observer.listener;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface ListenerChannel {
	
	//reference to applicatoin.yml's binding
	String JOBCREATION_INPUT = "job-creation";
	
	String JOBDELETION_INPUT = "job-deletion";

	@Input(JOBCREATION_INPUT)
	SubscribableChannel jobCreation();
	
	@Input(JOBDELETION_INPUT)
	SubscribableChannel jobDeletion();
	
}
