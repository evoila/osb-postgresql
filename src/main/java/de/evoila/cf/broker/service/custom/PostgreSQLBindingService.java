/**
 * 
 */
package de.evoila.cf.broker.service.custom;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.service.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.service.postgres.jdbc.PostgresDbService;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgreSQLBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PostgresCustomImplementation postgresCustomImplementation;

	private PostgresDbService connection(ServiceInstance serviceInstance) throws SQLException {
		Assert.notNull(serviceInstance, "ServiceInstance may not be null");
		String serviceInstanceId = serviceInstance.getId();
		Assert.notNull(serviceInstanceId, "Id of ServiceInstance may not be null");
		Assert.notNull(serviceInstance.getHosts(), "Host of ServiceInstance may not be null");

		ServerAddress host = serviceInstance.getHosts().get(0);
		PostgresDbService jdbcService = new PostgresDbService();
		jdbcService.createConnection(host.getIp(), host.getPort(), serviceInstanceId, serviceInstanceId, serviceInstanceId);
		return jdbcService;
	}

	public void create(ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
		PostgresDbService jdbcService;
		try {
			jdbcService = connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		String instanceId = serviceInstance.getId();

		try {
			postgresCustomImplementation.initServiceInstance(jdbcService, serviceInstance, serviceInstance.getId());

			jdbcService.executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not add to database");
		} finally {
            jdbcService.closeIfConnected();
        }
	}

	public void delete(ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
		PostgresDbService jdbcService;
		
		try {
			jdbcService = connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		String instanceId = serviceInstance.getId();

		try {
//			jdbcService.executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");
			jdbcService.executeUpdate("DROP DATABASE \"" + instanceId + "\"");
			jdbcService.executeUpdate("DROP ROLE \"" + instanceId + "\"");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not remove from database");
		} finally {
            jdbcService.closeIfConnected();
        }
		
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.service.impl.BindingServiceImpl#createCredentials(
	 * java.lang.String, de.evoila.cf.broker.model.ServiceInstance,
	 * de.evoila.cf.broker.model.ServerAddress)
	 */
	@Override
	protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
			ServerAddress host) throws ServiceBrokerException {
		PostgresDbService jdbcService;
		try {
			jdbcService = connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}
		
		String username = bindingId;
		String password = "";
		String hostIp = host.getIp();
		int hostPort = host.getPort();
		String database = serviceInstance.getId();
		
		try {
			password = postgresCustomImplementation.bindRoleToDatabaseAndGeneratePassword(jdbcService, serviceInstance.getId(), username);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not update database");
		} finally {
            jdbcService.closeIfConnected();
        }

		String dbURL = String.format("postgres://%s:%s@%s:%d/%s", username, password, hostIp, hostPort,
				database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("uri", dbURL);
		credentials.put("username", bindingId);
		credentials.put("password", password);
		credentials.put("host", hostIp);
		credentials.put("port", hostPort);
		credentials.put("database", database);

		return credentials;
	}

	@Override
	protected void deleteBinding(String bindingId, ServiceInstance serviceInstance) throws ServiceBrokerException {
		PostgresDbService jdbcService;
		try {
			jdbcService = connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		try {
			postgresCustomImplementation.unbindRoleFromDatabase(jdbcService, bindingId, serviceInstance.getId());
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not remove from database");
		} finally {
            jdbcService.closeIfConnected();
        }
	}

	@Override
	public ServiceInstanceBinding getServiceInstanceBinding(String id) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.service.impl.BindingServiceImpl#bindRoute(de.evoila.
	 * cf.broker.model.ServiceInstance, java.lang.String)
	 */
	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

}
