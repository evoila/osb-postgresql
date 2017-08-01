package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.DeploymentRepoBean;

@Service
@ConfigurationProperties(prefix="deployment.repo")
public class DeploymentRepoBeanImpl implements DeploymentRepoBean {

	private String service;
	
	private String monit;

	public String getService() {
		return service;
	}

	public String getMonit() {
		return monit;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setMonit(String monit) {
		this.monit = monit;
	}	
}
