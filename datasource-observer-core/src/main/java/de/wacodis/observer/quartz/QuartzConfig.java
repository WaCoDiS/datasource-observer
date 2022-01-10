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

import java.util.Properties;
import javax.sql.DataSource;
import org.quartz.spi.JobFactory;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 *
 * @author Matthes Rieke (m.rieke@52north.org)
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean quartzScheduler(ApplicationContext applicationContext, QuartzProperties qp) {
        SchedulerFactoryBean quartzScheduler = new SchedulerFactoryBean();

        Properties props = new Properties();
        props.putAll(qp.getProperties());

        quartzScheduler.setQuartzProperties(props);
        quartzScheduler.setOverwriteExistingJobs(true);

        quartzScheduler.setJobFactory(jobFactory(applicationContext));
        quartzScheduler.setDataSource(dataSource());

        return quartzScheduler;
    }

    @Bean
    @ConfigurationProperties("spring.datasource.quartz-data-source")
    @QuartzDataSource
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

}
