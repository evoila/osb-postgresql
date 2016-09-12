package de.evoila.cf.cpi.docker;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by dennismuller on 01/07/16.
 */
@Configuration
public class SampleConfiguration {

	@Bean
	public DirectExchange exchange() {
		return new DirectExchange("docker-volume-service", false, false);
	}

}
