package de.evoila.cf.broker.configuration;

import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by reneschollmeyer on 09.08.17.
 */
@Configuration
@ConditionalOnProperty(name="SPRING_RABBITMQ_HOST",havingValue="",matchIfMissing=false)
@Import({RabbitAutoConfiguration.class, BusAutoConfiguration.class})
class RabbitOnConditionalConfiguration{

}
