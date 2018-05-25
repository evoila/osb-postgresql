/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.bosh.InstanceGroupNotFoundException;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.cpi.existing.PostgreSQLExistingServiceFactory;
import io.bosh.client.deployments.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private Optional<ExistingEndpointBean> existingEndpointBean;
    private Optional<PostgresBoshPlatformService> postgresBoshPlatformService;

	PostgreSQLBindingService(PostgresCustomImplementation customImplementation,
                             PostgreSQLExistingServiceFactory existingServiceFactory,
                             Optional<ExistingEndpointBean> existingEndpointBean,
                             Optional<PostgresBoshPlatformService> postgresBoshPlatformService){
		Assert.notNull(customImplementation, "PostgresCustomImplementation may not be null");
		Assert.notNull(existingServiceFactory, "PostgreSQLExistingServiceFactory may not be null");
		this.existingServiceFactory = existingServiceFactory;
		this.postgresCustomImplementation = customImplementation;
		this.existingEndpointBean = existingEndpointBean;
		this.postgresBoshPlatformService = postgresBoshPlatformService;
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
    protected void deleteBinding(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
        PostgresDbService jdbcService = connection(serviceInstance, plan);

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
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) throws ServiceBrokerException {
		PostgresDbService jdbcService = connection(serviceInstance, plan);

        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
        String database = serviceInstance.getId();

		try {
		    postgresCustomImplementation.bindRoleToDatabase(jdbcService, username, password, database, false);

		    // TODO: Why are we doin this?
		    jdbcService.closeIfConnected();
			jdbcService.createConnection(username, password, database, serviceInstance.getHosts());

			postgresCustomImplementation.setUpBindingUserPrivileges(jdbcService, username, password);
            if(plan.getPlatform() == Platform.BOSH && postgresBoshPlatformService.isPresent()) {
                postgresBoshPlatformService.get().createPgPoolUser(serviceInstance);
            }
		} catch (SQLException e) {
		    log.error(String.format("Creating Binding(%s) failed while creating the ne postgres user. Could not update database", bindingId));
            throw new ServiceBrokerException("Could not update database");
		} catch (IOException | JSchException| NoSuchAlgorithmException e) {
            log.error(String.format("Creating Binding(%s) failed while creating the pgpool user. Connections to postgresql vms fauiled", bindingId));
            throw new ServiceBrokerException("Error creating pgpool user");
        } catch (InstanceGroupNotFoundException e) {
            log.error(String.format("Creating Binding(%s) failed while creating the pgpool user. %s", bindingId, e.getMessage()));
            throw new ServiceBrokerException(String.format("Creating Binding(%s) failed while creating the pgpool user. %s", bindingId, e.getMessage()));
        } finally {
            jdbcService.closeIfConnected();
        }

        String endpoint = ServiceInstanceUtils.connectionUrl(serviceInstance.getHosts());

		// When host is not empty, it is a service key
		if (host != null)
		    endpoint = host.getIp() + ":" + host.getPort();

        String dbURL = String.format("postgres://%s:%s@%s/%s", username, password, endpoint, database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put(URI, dbURL);
		credentials.put(USERNAME, username);
		credentials.put(PASSWORD, password);
		credentials.put(DATABASE, database);

		return credentials;
	}

    private PostgresDbService connection(ServiceInstance serviceInstance, Plan plan)  {
        PostgresDbService jdbcService = new PostgresDbService();

        if(plan.getPlatform() == Platform.BOSH && postgresBoshPlatformService.isPresent()) {

            List<ServerAddress> serverAddresses = serviceInstance.getHosts();
            String ingressInstanceGroup= plan.getMetadata().getIngressInstanceGroup();
            if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
                serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),ingressInstanceGroup);
            }

            jdbcService.createConnection(serviceInstance.getUsername(),
                                         serviceInstance.getPassword(),
                                         "admin",
                                         serverAddresses);


        } else if (plan.getPlatform() == Platform.EXISTING_SERVICE && existingEndpointBean.isPresent()) {
            ExistingEndpointBean existingEndpointBean = this.existingEndpointBean.get();
            jdbcService.createConnection(existingEndpointBean.getUsername(),existingEndpointBean.getPassword(),
                                         existingEndpointBean.getDatabase(),serviceInstance.getHosts()
            );
        }

        return jdbcService;
    }

}
