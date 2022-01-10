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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author matthes
 */
@Configuration
public class ApiHubConfig {
    
    @Bean
    public RestTemplate apiHubRestTemplate(@Value("${datasource-observer.sentinelhub.base-url}") String baseUri,
            @Value("${datasource-observer.sentinelhub.user}") String user,
            @Value("${datasource-observer.sentinelhub.password}") String pw) {
        return new RestTemplateBuilder()
                .rootUri(baseUri)
                .basicAuthorization(user, pw)
                .messageConverters(new ApiHubResponseConverter())
                .build();
    }
    
}
