/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.repository.BindingRepository;

/**
 * @author Patrick Weber, evoila.
 *
 */
@Service
public class BindingRepositoryImpl implements BindingRepository {

	@Autowired
	MongoRepository<ServiceInstanceBinding, String> bindingRepository;
	
	private static final String PREFIX = "binding-";

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
		return bindingRepository.findOne(bindingId).getServiceInstanceId();
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
		bindingRepository.save(binding);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.evoila.cf.broker.repository.BindingRepository#
	 * containsInternalBindingId(java.lang.String)
	 */
	@Override
	public boolean containsInternalBindingId(String bindingId) {
		return bindingRepository.exists(bindingId);
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
		bindingRepository.delete(bindingId);
	}

	@Override
	public ServiceInstanceBinding findOne(String bindingId) {
		return bindingRepository.findOne(bindingId);
	}

}
