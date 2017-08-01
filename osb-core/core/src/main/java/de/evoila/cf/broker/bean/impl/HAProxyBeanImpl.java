/**
 * 
 */
package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.HAProxyBean;

/**
 * @author Rene Schollmeyer
 *
 */
@Service
@ConfigurationProperties(prefix="haproxy")
public class HAProxyBeanImpl implements HAProxyBean {

	private String uri;
	
	private Auth auth;
	
	public static class Auth {
		private String token;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public Auth getAuth() {
		return auth;
	}

	public void setAuth(Auth auth) {
		this.auth = auth;
	}

	public String getAuthToken() {
		return auth.getToken();
	}
}
