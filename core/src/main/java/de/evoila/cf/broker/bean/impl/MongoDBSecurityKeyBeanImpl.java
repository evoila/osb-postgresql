package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.MongoDBSecurityKeyBean;

@Service
@ConfigurationProperties(prefix="mongodb.security.key")
public class MongoDBSecurityKeyBeanImpl implements MongoDBSecurityKeyBean {

	private int length;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
}
