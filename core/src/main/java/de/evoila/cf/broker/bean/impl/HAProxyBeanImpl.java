/**
 * 
 */
package de.evoila.cf.broker.bean.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.HAProxyBean;

/**
 * @author Rene Schollmeyer
 *
 */

@Service
public class HAProxyBeanImpl extends HAProxyBean {
	
	@Value("${haproxy.uri}")
	private String uri;
	
	@Value("${haproxy.auth.token}")
	private String authToken;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
}
