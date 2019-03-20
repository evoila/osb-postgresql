package de.evoila.cf.broker.custom.postgres;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.*;
import de.evoila.cf.broker.service.AsyncBindingService;
import de.evoila.cf.broker.service.HAProxyService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.InstanceGroupNotFoundException;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.cpi.existing.PostgreSQLExistingServiceFactory;
import de.evoila.cf.security.credentials.CredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	private PostgresCustomImplementation postgresCustomImplementation;

	private PostgreSQLExistingServiceFactory existingServiceFactory;

    private PostgresBoshPlatformService postgresBoshPlatformService;

    private CredentialStore credentialStore;

	PostgreSQLBindingService(PostgresCustomImplementation customImplementation,
                             PostgreSQLExistingServiceFactory existingServiceFactory,
                             PostgresBoshPlatformService postgresBoshPlatformService,
                             BindingRepository bindingRepository, ServiceDefinitionRepository serviceDefinitionRepository,
                             ServiceInstanceRepository serviceInstanceRepository, RouteBindingRepository routeBindingRepository,
                             @Autowired( required = false ) HAProxyService haProxyService, JobRepository jobRepository,
                             AsyncBindingService asyncBindingService, PlatformRepository platformRepository,
                             CredentialStore credentialStore) {
        super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository, routeBindingRepository,
                haProxyService, jobRepository, asyncBindingService, platformRepository);
	    Assert.notNull(customImplementation, "PostgresCustomImplementation may not be null");
		Assert.notNull(existingServiceFactory, "PostgreSQLExistingServiceFactory may not be null");
		this.existingServiceFactory = existingServiceFactory;
		this.postgresCustomImplementation = customImplementation;
		this.postgresBoshPlatformService = postgresBoshPlatformService;
		this.credentialStore = credentialStore;
	}

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) throws ServiceBrokerException {
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore
                .getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        String database = serviceInstance.getId();
        if (serviceInstanceBindingRequest.getParameters() != null) {
            String customBindingDatabase = (String) serviceInstanceBindingRequest.getParameters().get("database");

            if (!StringUtils.isEmpty(customBindingDatabase))
                database = customBindingDatabase;
        }

		PostgresDbService jdbcService = postgresCustomImplementation.connection(serviceInstance, plan, serviceInstanceUsernamePasswordCredential);

        credentialStore.createUser(serviceInstance, bindingId);
        UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, bindingId);
        String generalrole = database;

		try {
		    if (postgresCustomImplementation.isPgpoolEnabled()) {
                if (plan.getPlatform() == Platform.BOSH) {
                    postgresBoshPlatformService.createPgPoolUser(serviceInstance, plan,
                            usernamePasswordCredential.getUsername(),
                            usernamePasswordCredential.getPassword());
                } else if (plan.getPlatform() == Platform.EXISTING_SERVICE) {
                    existingServiceFactory.createPgPoolUser(postgresBoshPlatformService,
                            usernamePasswordCredential.getUsername(),
                            usernamePasswordCredential.getPassword());
                }
            }
            postgresCustomImplementation.createGeneralRole(jdbcService, generalrole, database);
            postgresCustomImplementation.bindRoleToDatabase(jdbcService, usernamePasswordCredential.getUsername(),
                    usernamePasswordCredential.getPassword(), database, generalrole, false);
            jdbcService.closeIfConnected();

 		    // close connection to postgresql db / open connection to bind db
            // necessary to set user specific privileges inside the db
			jdbcService.createConnection(usernamePasswordCredential.getUsername(),
                    usernamePasswordCredential.getPassword(), database, postgresCustomImplementation.filterServerAddresses(serviceInstance, plan));
			postgresCustomImplementation.setUpBindingUserPrivileges(jdbcService, usernamePasswordCredential.getUsername(), generalrole);
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

        List<ServerAddress> pgpoolHosts = serviceInstance.getHosts();
        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
            pgpoolHosts = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
        }

        String endpoint = ServiceInstanceUtils.connectionUrl(pgpoolHosts);

        // When host is not empty, it is a service key
		if (host != null)
		    endpoint = host.getIp() + ":" + host.getPort();

        String dbURL = String.format("postgres://%s:%s@%s/%s", usernamePasswordCredential.getUsername(),
                usernamePasswordCredential.getPassword(), endpoint, database);

		Map<String, Object> credentials = new HashMap<>();
		credentials.put(URI, dbURL);
		credentials.put(USERNAME, usernamePasswordCredential.getUsername());
		credentials.put(PASSWORD, usernamePasswordCredential.getPassword());
		credentials.put(DATABASE, database);

		return credentials;
	}

    @Override
    protected void unbindService(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore
                .getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        PostgresDbService jdbcService = postgresCustomImplementation.connection(serviceInstance, plan, serviceInstanceUsernamePasswordCredential);

        try {
            UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, binding.getId());

            postgresCustomImplementation.unbindRoleFromDatabase(jdbcService, usernamePasswordCredential.getUsername());
            credentialStore.deleteCredentials(serviceInstance, binding.getId());
        } catch (SQLException e) {
            throw new ServiceBrokerException("Could not remove from database");
        } finally {
            jdbcService.closeIfConnected();
        }
    }

}
