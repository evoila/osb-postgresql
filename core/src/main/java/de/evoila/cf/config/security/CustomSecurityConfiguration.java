package de.evoila.cf.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Johannes Hiemer, cloudscale.
 * 
 */
@Configuration
@EnableWebSecurity
public class CustomSecurityConfiguration extends WebSecurityConfigurerAdapter  {

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Autowired
    protected void configureGlobal(AuthenticationManagerBuilder auth,
    								@Value("${login.username}") String username,
    								@Value("${login.password}") String password,
    								@Value("${login.role}") String role) throws Exception {
        auth
            .inMemoryAuthentication()
            	.withUser(username).password(password).roles(role);
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
        	.antMatchers(HttpMethod.GET, "/info").permitAll()
        	.antMatchers(HttpMethod.GET, "/health").permitAll()
        	.antMatchers("/**").hasRole("USER")
        	.anyRequest().authenticated()
        .and()
        	.httpBasic()
        .and()
        	.csrf().disable();
    }
}
