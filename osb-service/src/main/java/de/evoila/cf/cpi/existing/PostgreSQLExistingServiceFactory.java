package de.evoila.cf.cpi.existing;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.security.credentials.CredentialStore;
import de.evoila.cf.security.credentials.DefaultCredentialConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Brinker, Johannes Hiemer, Marco Hennig.
 */
@Service
@ConditionalOnBean(ExistingEndpointBean.class)
public class PostgreSQLExistingServiceFactory extends ExistingServiceFactory {

    private PostgresCustomImplementation postgresCustomImplementation;

    private ExistingEndpointBean existingEndpointBean;

    private CredentialStore credentialStore;

	public PostgreSQLExistingServiceFactory(PostgresCustomImplementation postgresCustomImplementation,
                                            ExistingEndpointBean existingEndpointBean,
											PlatformRepository platformRepository,
                                            ServicePortAvailabilityVerifier portAvailabilityVerifier,
                                            CredentialStore credentialStore) {
		super(platformRepository, portAvailabilityVerifier, existingEndpointBean);
		this.postgresCustomImplementation = postgresCustomImplementation;
	    this.existingEndpointBean = existingEndpointBean;
        this.credentialStore = credentialStore;
    }

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
		String database = serviceInstance.getId();
		PostgresDbService postgresDbService = postgresCustomImplementation
                .connection(serviceInstance, plan, null, database);
		try {
		    postgresCustomImplementation.dropAllExtensions(postgresDbService);
		} catch (SQLException e) {
			log.error(String.format("Extension drop (%s) failed while dropping the database %s", database), e);
		}

		postgresDbService.closeIfConnected();
		postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, null);
        postgresCustomImplementation.deleteDatabase(postgresDbService, serviceInstance.getUsername(), database, serviceInstance.getUsername());
        credentialStore.deleteCredentials(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS);
	}

    @Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {

	    if (existingEndpointBean.getBackupCredentials() != null)
	        credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS,
                existingEndpointBean.getBackupCredentials().getUsername(), existingEndpointBean.getBackupCredentials().getPassword());

	    credentialStore.createUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS, serviceInstanceUsernamePasswordCredential.getUsername(),
                serviceInstanceUsernamePasswordCredential.getPassword());

        serviceInstance.setUsername(serviceInstanceUsernamePasswordCredential.getUsername());

		String database = serviceInstance.getId();
		String generalRole = database;

	    PostgresDbService postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan,
                new UsernamePasswordCredential(existingEndpointBean.getUsername(), existingEndpointBean.getPassword()));
        postgresCustomImplementation.createDatabase(postgresDbService, database);

        try {
			postgresCustomImplementation.createGeneralRole(postgresDbService, serviceInstance.getId(), serviceInstance.getId());
			postgresCustomImplementation.bindRoleToDatabase(postgresDbService,
					serviceInstanceUsernamePasswordCredential.getUsername(),
                    serviceInstanceUsernamePasswordCredential.getPassword(),
                    database, generalRole,true);

			// close connection to postgresql db / open connection to bind db
			// necessary for installing db specific extensions (as admin)
			postgresDbService.closeIfConnected();
			postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan,
                    new UsernamePasswordCredential(existingEndpointBean.getUsername(), existingEndpointBean.getPassword()), database);
			postgresCustomImplementation.createExtensions(postgresDbService);
		} catch(SQLException ex) {
            throw new PlatformException(ex);
        }

        return serviceInstance;
	}

    @Override
    public ServiceInstance getInstance(ServiceInstance serviceInstance, Plan plan) {
        return serviceInstance;
    }

    public void createPgPoolUser(PostgresBoshPlatformService postgresBoshPlatformService,
                                 String instanceIngressGroup, List<ServerAddress> hosts,
                                 UsernamePasswordCredential usernamePasswordCredential) throws JSchException {

		postgresBoshPlatformService.createPgPoolUser(
			this.existingEndpointBean.getDeployment(), instanceIngressGroup, hosts,
			usernamePasswordCredential.getUsername(), usernamePasswordCredential.getPassword());
	}
}
