/**
 * 
 */
package de.evoila.cf.broker.persistence.mongodb.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import de.evoila.cf.broker.model.ServiceInstanceBinding;

/**
 * @author Christian Brinker, evoila.
 *
 */
public interface MongoDBBindingRepository extends MongoRepository<ServiceInstanceBinding, String> {

}
