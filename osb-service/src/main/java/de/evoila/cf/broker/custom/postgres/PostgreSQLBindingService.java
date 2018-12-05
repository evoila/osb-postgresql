/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.repository.*;
import de.evoila.cf.broker.service.AsyncBindingService;
import de.evoila.cf.broker.service.HAProxyService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.bosh.InstanceGroupNotFoundException;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.cpi.existing.PostgreSQLExistingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private PostgresBoshPlatformService postgresBoshPlatformService;

	PostgreSQLBindingService(PostgresCustomImplementation customImplementation,
                             PostgreSQLExistingServiceFactory existingServiceFactory,
                             PostgresBoshPlatformService postgresBoshPlatformService,
                             BindingRepository bindingRepository, ServiceDefinitionRepository serviceDefinitionRepository,
                             ServiceInstanceRepository serviceInstanceRepository, RouteBindingRepository routeBindingRepository,
                             HAProxyService haProxyService, JobRepository jobRepository,
                             AsyncBindingService asyncBindingService, PlatformRepository platformRepository) {
        super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository, routeBindingRepository,
                haProxyService, jobRepository, asyncBindingService, platformRepository);
	    Assert.notNull(customImplementation, "PostgresCustomImplementation may not be null");
		Assert.notNull(existingServiceFactory, "PostgreSQLExistingServiceFactory may not be null");
		this.existingServiceFactory = existingServiceFactory;
		this.postgresCustomImplementation = customImplementation;
		this.postgresBoshPlatformService = postgresBoshPlatformService;
	}

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void unbindService(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
        PostgresDbService jdbcService = postgresCustomImplementation.connection(serviceInstance, plan);

        try {
            String username = binding.getCredentials().get(USERNAME).toString();

            postgresCustomImplementation.unbindRoleFromDatabase(serviceInstance, plan, jdbcService, username);
        } catch (SQLException e) {
            throw new ServiceBrokerException("Could not remove from database");
        } finally {
            jdbcService.closeIfConnected();
        }
	}

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) throws ServiceBrokerException {

        String database = serviceInstance.getId();
        if (serviceInstanceBindingRequest.getParameters() != null) {
            String customBindingDatabase = (String) serviceInstanceBindingRequest.getParameters().get("database");

            if (!StringUtils.isEmpty(customBindingDatabase))
                database = customBindingDatabase;
        }


		PostgresDbService jdbcService = postgresCustomImplementation.connection(serviceInstance, plan);

        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
        String generalrole = database;

		try {
		    if(postgresCustomImplementation.isPgpoolEnabled()){
                if(plan.getPlatform() == Platform.BOSH) {
                    postgresBoshPlatformService.createPgPoolUser(serviceInstance, plan, username, password);
                } else if(plan.getPlatform() == Platform.EXISTING_SERVICE) {
                    existingServiceFactory.createPgPoolUser(postgresBoshPlatformService, username, password);
                }
            }
            postgresCustomImplementation.createGeneralRole(serviceInstance, plan, jdbcService, generalrole, database);
            postgresCustomImplementation.bindRoleToDatabase(serviceInstance, plan, jdbcService, username, password, database, generalrole, false);

            jdbcService.closeIfConnected();

 		    // close connection to postgresql db / open connection to bind db
            // necessary to set user specific privileges inside the db
		    jdbcService.closeIfConnected();
			jdbcService.createConnection(username, password, database, postgresCustomImplementation.filterServerAddresses(serviceInstance, plan));
			postgresCustomImplementation.setUpBindingUserPrivileges(jdbcService, username, generalrole);
		} catch (SQLException e) {
		    log.error(String.format("Creating Binding(%s) failed while creating the ne postgres user. Could not update database", bindingId), e);
            throw new ServiceBrokerException("Could not update database");
		} catch (IOException | JSchException e) {
            log.error(String.format("Creating Binding(%s) failed while creating the PgPool user. Connections to PostgreSQL VMs failed", bindingId), e);
            throw new ServiceBrokerException("Error creating PgPool user");
        } catch (InstanceGroupNotFoundException e) {
            log.error(String.format("Creating Binding(%s) failed while creating the PgPool user. %s", bindingId), e);
            throw new ServiceBrokerException(String.format("Creating Binding(%s) failed while creating the PgPool user. %s", bindingId));
        } finally {
            jdbcService.closeIfConnected();
        }

        List<ServerAddress> pgpool_hosts = serviceInstance.getHosts();
        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
            pgpool_hosts = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
        }

        String endpoint = ServiceInstanceUtils.connectionUrl(pgpool_hosts);

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

}
