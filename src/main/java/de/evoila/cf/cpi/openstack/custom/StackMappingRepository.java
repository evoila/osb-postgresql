package de.evoila.cf.cpi.openstack.custom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.repository.MongoRepository;

import de.evoila.cf.broker.bean.OpenstackBean;


/**
 * @author Christian Brinker, evoila.
 *
 */
@ConditionalOnBean(OpenstackBean.class)
public interface StackMappingRepository extends MongoRepository<PostgreSqlStackMapping, String> {

}
