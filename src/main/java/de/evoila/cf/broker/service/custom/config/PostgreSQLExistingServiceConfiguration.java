/**
 * 
 */
package de.evoila.cf.broker.service.custom.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import de.evoila.cf.broker.service.custom.PostgreSQLExistingServiceFactory;

/**
 * @author Sebastian Boeing, evoila.
 *
 */

@Configuration
@EnableConfigurationProperties(value={PostgreSQLExistingServiceFactory.class})
public class PostgreSQLExistingServiceConfiguration {

}




