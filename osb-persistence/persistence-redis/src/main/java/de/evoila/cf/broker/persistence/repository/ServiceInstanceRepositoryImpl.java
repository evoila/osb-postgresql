/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Repository
public class ServiceInstanceRepositoryImpl extends CrudRepositoryImpl<ServiceInstance, String>
		implements ServiceInstanceRepository {
	
	@Autowired
	@Qualifier("jacksonServiceInstanceRedisTemplate")
	private RedisTemplate<String, ServiceInstance> redisTemplate;
	
	@Override
	protected RedisTemplate<String, ServiceInstance> getRedisTemplate() {
		return this.redisTemplate;
	}
	
	private static final String PREFIX = "instance-";

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public ServiceInstance getServiceInstance(String instanceId) {
		return findOne(instanceId);
	}

	@Override
	public boolean containsServiceInstanceId(String serviceInstanceId) {
		return exists(serviceInstanceId);
	}

	@Override
	public void addServiceInstance(String id, ServiceInstance serviceInstance) {
		if (!id.equals(serviceInstance.getId())) {
			serviceInstance = new ServiceInstance(id, serviceInstance.getServiceDefinitionId(),
					serviceInstance.getPlanId(), serviceInstance.getOrganizationGuid(), serviceInstance.getSpaceGuid(),
					serviceInstance.getParameters(), serviceInstance.getDashboardUrl(),
					serviceInstance.getInternalId());
		}
		save(serviceInstance);
	}

	@Override
	public void deleteServiceInstance(String serviceInstanceId) {
		delete(serviceInstanceId);
	}

}
