/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.persistence.mongodb.repository.ClusterStackMapping;
import de.evoila.cf.broker.persistence.mongodb.repository.ClusterStackMappingRepository;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.existing.PostgreSQLExistingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.util.*;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgreSQLBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(getClass());

    private static String URI = "uri";
    private static String USERNAME = "user";
    private static String PASSWORD = "password";
    private static String DATABASE = "database";
    private static String HOST = "host";
    private static String PORT = "port";

    private RandomString usernameRandomString = new RandomString(10);
    private RandomString passwordRandomString = new RandomString(15);

	private PostgresCustomImplementation postgresCustomImplementation;
	private PostgreSQLExistingServiceFactory existingServiceFactory;
	private Optional<ClusterStackMappingRepository> stackMappingRepository;

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	PostgreSQLBindingService(PostgresCustomImplementation customImplementation, PostgreSQLExistingServiceFactory existingServiceFactory, Optional<ClusterStackMappingRepository> stackMappingRepository){
		Assert.notNull(customImplementation, "PostgresCustomImplementation may not be null");
		Assert.notNull(existingServiceFactory, "PostgreSQLExistingServiceFactory may not be null");
		this.existingServiceFactory = existingServiceFactory;
		this.postgresCustomImplementation = customImplementation;
		this.stackMappingRepository = stackMappingRepository;
	}

    @Override
    public ServiceInstanceBinding getServiceInstanceBinding(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ServiceInstanceBinding bindService(String bindingId, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {

        List<ServerAddress> hosts = serviceInstance.getHosts();
        Map<String, Object> credentials = createCredentials(bindingId, serviceInstance, hosts, plan);

        return new ServiceInstanceBinding(bindingId, serviceInstance.getId(), credentials, null);
    }

    @Override
    protected void deleteBinding(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
        PostgresDbService jdbcService = jdbcService = connection(serviceInstance, plan);

        try {
            String username = binding.getCredentials().get(USERNAME).toString();
            postgresCustomImplementation.unbindRoleFromDatabase(jdbcService, username, username + "-fallback");
        } catch (SQLException e) {
            throw new ServiceBrokerException("Could not remove from database");
        } finally {
            jdbcService.closeIfConnected();
        }
    }

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
                                                    ServerAddress host, Plan plan) throws ServiceBrokerException {
        List<ServerAddress> hosts = new ArrayList<>();
        hosts.add(host);

        return createCredentials(bindingId, serviceInstance, hosts, plan);
    }

	protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
			List<ServerAddress> host, Plan plan) throws ServiceBrokerException {
		PostgresDbService jdbcService = connection(serviceInstance, plan);

        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
        String database = bindingId;

		try {
		    postgresCustomImplementation.bindRoleToDatabase(jdbcService, username, password, database, false);
			jdbcService.closeIfConnected();
			jdbcService.createConnection(username, password, database, serviceInstance.getHosts());
			postgresCustomImplementation.setUpBindingUserPrivileges(jdbcService, username, password);
		} catch (SQLException e) {
			throw new ServiceBrokerException("Could not update database");
		} finally {
            jdbcService.closeIfConnected();
        }

        String hostIp = serviceInstance.getHosts().get(0).getIp();
        int hostPort = serviceInstance.getHosts().get(0).getPort();

        String dbURL = String.format("postgres://%s:%s@%s:%d/%s", username, password, hostIp, hostPort, database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put(URI, dbURL);
		credentials.put(USERNAME, bindingId);
		credentials.put(PASSWORD, password);
		credentials.put(HOST, hostIp);
		credentials.put(PORT, hostPort);
		credentials.put(DATABASE, database);

		return credentials;
	}

    private PostgresDbService connection(ServiceInstance serviceInstance, Plan plan) {
        PostgresDbService jdbcService = new PostgresDbService();

        if(plan.getPlatform() == Platform.BOSH)
            jdbcService.createConnection(serviceInstance.getUsername(), serviceInstance.getPassword(),
                    "admin", serviceInstance.getHosts());
        else if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHostsWithServerAddress());

        return jdbcService;
    }

}
