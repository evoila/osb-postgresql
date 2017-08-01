/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.repository.RouteBindingRepository;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Repository
public class RouteBindingRepositoryImpl extends CrudRepositoryImpl<RouteBinding, String>
		implements RouteBindingRepository {

	@Autowired
	@Qualifier("jacksonRouteBindingRedisTemplate")
	private RedisTemplate<String, RouteBinding> redisTemplate;

	@Override
	protected RedisTemplate<String, RouteBinding> getRedisTemplate() {
		return this.redisTemplate;
	}

	private static final String PREFIX = "route-binding-";

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
	public String getRouteBindingId(String bindingId) {
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
	public void addRouteBinding(RouteBinding binding) {
		save(binding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.evoila.cf.broker.repository.BindingRepository#
	 * containsInternalBindingId(java.lang.String)
	 */
	@Override
	public boolean containsRouteBindingId(String bindingId) {
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
	public void deleteRouteBinding(String bindingId) {
		this.delete(bindingId);
	}
}
