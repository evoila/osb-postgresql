package de.evoila.cf.config.security;

import de.evoila.cf.broker.bean.AuthenticationPropertiesBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * @author Johannes Hiemer, cloudscale.
 * 
 */
@Configuration
@EnableWebSecurity
public class CustomSecurityConfiguration extends WebSecurityConfigurerAdapter  {

	@Autowired
	private AuthenticationPropertiesBean authentication;
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Autowired
    protected void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth
			.inMemoryAuthentication()
			.withUser(authentication.getUsername())
			.password(authentication.getPassword())
			.roles(authentication.getRole());
	}

    @Bean 
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        	.authorizeRequests()
        	.antMatchers("/v2/endpoint").authenticated()
				.antMatchers("/v2/catalog").authenticated()
				.antMatchers("/v2/service_instance/**").authenticated()
        	.antMatchers(HttpMethod.GET, "/info").authenticated()
        	.antMatchers(HttpMethod.GET, "/health").authenticated()
				.antMatchers(HttpMethod.GET, "/error").authenticated()
        	.anyRequest().authenticated()
        .and()
        	.httpBasic()
        .and()
        	.csrf().disable();
    }

	@Configuration
	@Order(1)
	public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Bean
		public SessionRegistry sessionRegistry() {
			return new SessionRegistryImpl();
		}

		@Bean
		public HttpSessionEventPublisher httpSessionEventPublisher() {
			return new HttpSessionEventPublisher();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					.antMatchers("/v2/dashboard/{serviceInstanceId}").permitAll()
					.antMatchers("/v2/dashboard/{serviceInstanceId}/confirm").permitAll()
					.antMatchers("/v2/dashboard/manage/**").authenticated()
					.and()
						.sessionManagement()
						.maximumSessions(1);
		}
	}
}
