/**
 * 
 */
package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.AuthenticationPropertiesBean;

/**
 * @author René Schollmeyer
 *
 */
@Service
@ConfigurationProperties(prefix="login")
public class AuthenticationPropertiesBeanImpl implements AuthenticationPropertiesBean {

	private String username;
	private String password;
	private String role;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
}
