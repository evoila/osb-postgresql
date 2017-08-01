/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.persistence.mongodb.repository.ClusterStackMapping;
import de.evoila.cf.broker.persistence.mongodb.repository.StackMappingRepository;
import de.evoila.cf.cpi.openstack.fluent.HeatFluent;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
@Primary
@ConditionalOnBean(OpenstackBean.class)
public class PostgreSqlIpAccessor extends CustomIpAccessor {

	@SuppressWarnings("unused")
	private HeatFluent heatFluent;

	@Autowired
	private StackMappingRepository stackMappingRepo;

	@Autowired
	private DefaultIpAccessor defaultIpAccessor;

	@Override
	public List<ServerAddress> getIpAddresses(String instanceId) throws PlatformException {
		ClusterStackMapping stackMapping = stackMappingRepo.findOne(instanceId);

		if (stackMapping != null) {
			return stackMapping.getServerAddresses();
		} else {
			return defaultIpAccessor.getIpAddresses(instanceId);
		}
	}

	@Autowired
	public void setHeatFluent(HeatFluent heatFluent) {
		this.heatFluent = heatFluent;
	}
}
