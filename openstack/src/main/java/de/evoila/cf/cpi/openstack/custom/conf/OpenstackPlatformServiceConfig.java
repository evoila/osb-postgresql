/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.evoila.cf.cpi.openstack.custom.CustomIpAccessor;
import de.evoila.cf.cpi.openstack.custom.CustomStackHandler;
import de.evoila.cf.cpi.openstack.custom.DefaultIpAccessor;
import de.evoila.cf.cpi.openstack.custom.IpAccessor;
import de.evoila.cf.cpi.openstack.custom.StackHandler;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Configuration
public class OpenstackPlatformServiceConfig {

	@Value("${openstack.networkId}")
	private String networkId;

	@ConditionalOnMissingBean(CustomIpAccessor.class)
	@Bean
	public IpAccessor ipAccessor() {
		return defaultIpAccessor();
	}

	@Bean
	public DefaultIpAccessor defaultIpAccessor() {
		return new DefaultIpAccessor(networkId);
	}

	@ConditionalOnMissingBean(CustomStackHandler.class)
	@Bean
	public StackHandler stackHandler() {
		return new StackHandler();
	}
}
