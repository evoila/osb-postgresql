package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.RabbitMQSecurityKeyBean;

@Service
@ConfigurationProperties(prefix="rabbitmq.security.key")
public class RabbitMQSecurityKeyBeanImpl implements RabbitMQSecurityKeyBean {

	private int length;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}	
}
