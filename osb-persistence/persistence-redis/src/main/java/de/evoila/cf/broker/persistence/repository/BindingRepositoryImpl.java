/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.repository.BindingRepository;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Repository
public class BindingRepositoryImpl extends CrudRepositoryImpl<ServiceInstanceBinding, String>
		implements BindingRepository {

	@Autowired
	@Qualifier("jacksonServiceInstanceBindingRedisTemplate")
	private RedisTemplate<String, ServiceInstanceBinding> redisTemplate;

	@Override
	protected RedisTemplate<String, ServiceInstanceBinding> getRedisTemplate() {
		return this.redisTemplate;
	}

	private static final String PREFIX = "binding-";

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.repository.BindingRepository#getInternalBindingId(
	 * java.lang.String)
	 */
	@Override
	public String getInternalBindingId(String bindingId) {
		return this.findOne(bindingId).getServiceInstanceId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.repository.BindingRepository#addInternalBinding(java.
	 * lang.String, java.lang.String)
	 */
	@Override
	public void addInternalBinding(ServiceInstanceBinding binding) {
		save(binding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.evoila.cf.broker.repository.BindingRepository#
	 * containsInternalBindingId(java.lang.String)
	 */
	@Override
	public boolean containsInternalBindingId(String bindingId) {
		return exists(bindingId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.repository.BindingRepository#deleteBinding(java.lang.
	 * String)
	 */
	@Override
	public void deleteBinding(String bindingId) {
		this.delete(bindingId);
	}
}
