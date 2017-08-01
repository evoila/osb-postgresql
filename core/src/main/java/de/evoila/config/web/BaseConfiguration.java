/**
 * 
 */
package de.evoila.config.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.evoila.cf.broker.model.Catalog;
import de.evoila.cf.config.web.cors.CORSFilter;

/**
 * @author Johannes Hiemer.
 *
 */
@Configuration
@EnableConfigurationProperties(Catalog.class)
public class BaseConfiguration {

    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new CORSFilter());
        registration.addUrlPatterns("/*");
        registration.setName("corsFilter");
        return registration;
    }

}
