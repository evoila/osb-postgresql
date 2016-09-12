/**
 * 
 */
package de.evoila.cf.cpi.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import de.evoila.cf.broker.model.Catalog;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceDefinition;
import de.evoila.cf.broker.model.VolumeUnit;

/**
 * @author Johannes Hiemer.
 *
 */
@Configuration
@ComponentScan(basePackages = { "de.evoila.cf.cpi", "de.evoila.cf.broker.cpi" })
public class IntegrationTestConfiguration {
	
	@Bean
	public PropertyPlaceholderConfigurer properties() {
		PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] { new ClassPathResource("application.properties") };
		propertyPlaceholderConfigurer.setLocations(resources);
		propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
		return propertyPlaceholderConfigurer;
	}
	
	@Bean
	public Catalog catalog() {
		Catalog catalog = new Catalog(Arrays.asList(serviceDefinition()));

		return catalog;
	}
	
	@Bean
	public Map<String, String> customProperties() {
		Map<String, String> customProperties = new HashMap<String, String>();
		customProperties.put("database_name", "admin");
		
		return customProperties;
	}
	
	@Bean
	public ServiceDefinition serviceDefinition() {
		Plan dockerPlan = new Plan("docker", "PostgreSQL-Docker-25MB",
				"The most basic PostgreSQL plan currently available. Providing"
						+ "25 MB of capcity in a PostgreSQL DB.", Platform.DOCKER, 25, VolumeUnit.M, null, 4);
		Plan openstackPlan = new Plan("openstack", "PostgreSQL-VM-500MB",
				"The most basic PostgreSQL plan currently available. Providing"
						+ "500 MB of capcity in a PostgreSQL DB.", Platform.OPENSTACK, 500, VolumeUnit.M, "3", 10);

		ServiceDefinition serviceDefinition = new ServiceDefinition("postgres", "postgres", "PostgreSQL Instances",
				true, Arrays.asList(dockerPlan, openstackPlan));

		return serviceDefinition;
	}

}
