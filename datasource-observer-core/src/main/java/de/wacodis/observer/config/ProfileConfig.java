package de.wacodis.observer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("spring.profiles")
public class ProfileConfig {

	public static String activeProfile;
	
	public String getActive() {
		return activeProfile;
	}
	public void setActive(String activeProfile) {
		this.activeProfile = activeProfile;
	}
	
	
}