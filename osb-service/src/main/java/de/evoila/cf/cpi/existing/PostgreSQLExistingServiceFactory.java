package de.evoila.cf.cpi.existing;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.postgres.PostgreConnectionHandler;
import de.evoila.cf.broker.custom.postgres.PostgreSQLUtils;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.Platform;
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
import org.springframework.credhub.core.CredHubException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Christian Brinker, Johannes Hiemer, Marco Hennig.
 */
@Service
@ConditionalOnBean(ExistingEndpointBean.class)
public class PostgreSQLExistingServiceFactory extends ExistingServiceFactory {

    private PostgresCustomImplementation postgresCustomImplementation;

    private ExistingEndpointBean existingEndpointBean;

    private CredentialStore credentialStore;

    private PostgreConnectionHandler postgreConnectionHandler;

	public PostgreSQLExistingServiceFactory(PostgresCustomImplementation postgresCustomImplementation,
                                            ExistingEndpointBean existingEndpointBean,
											PlatformRepository platformRepository,
                                            ServicePortAvailabilityVerifier portAvailabilityVerifier,
                                            CredentialStore credentialStore,
											PostgreConnectionHandler postgreConnectionHandler) {
		super(platformRepository, portAvailabilityVerifier, existingEndpointBean);
		this.postgresCustomImplementation = postgresCustomImplementation;
	    this.existingEndpointBean = existingEndpointBean;
        this.credentialStore = credentialStore;
        this.postgreConnectionHandler = postgreConnectionHandler;
    }

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
		String database = PostgreSQLUtils.dbName(serviceInstance.getId());
		PostgresDbService postgresDbService = postgreConnectionHandler
                .createExtendedRootUserConnection(serviceInstance, plan, database,true);
		try {
		    postgresCustomImplementation.dropAllExtensions(postgresDbService);
		} catch (SQLException e) {
			log.error("Extension drop (%s) failed while dropping the database %s".formatted(database), e);
		}

		postgresDbService.closeIfConnected();
		postgresDbService = postgreConnectionHandler.createSimpleRootUserConnection(serviceInstance, plan, null,true);
        postgresCustomImplementation.deleteDatabase(postgresDbService, serviceInstance.getUsername(), database, serviceInstance.getUsername());

		deleteCredential(serviceInstance,CredentialConstants.ROOT_CREDENTIALS);
		deleteCredential(serviceInstance,DefaultCredentialConstants.BACKUP_CREDENTIALS);
		deleteCredential(serviceInstance,DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
	}

	private void deleteCredential(ServiceInstance serviceInstance, String credential) {
		try {

			credentialStore.deleteCredentials(serviceInstance, credential);
		} catch(CredHubException e) {
			log.error("The credentials " + credential + " did not exist or could not be deleted");
		}
	}

    @Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {

	    if (existingEndpointBean.getBackupCredentials() != null) {
			credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS,
					existingEndpointBean.getBackupCredentials().getUsername(), existingEndpointBean.getBackupCredentials().getPassword());
		}

	    credentialStore.createUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS,
				serviceInstanceUsernamePasswordCredential.getUsername(),
                serviceInstanceUsernamePasswordCredential.getPassword());

        serviceInstance.setUsername(serviceInstanceUsernamePasswordCredential.getUsername());

		String database = PostgreSQLUtils.dbName(serviceInstance.getId());

		serviceInstance.setHosts(existingEndpointBean.getHosts());

        try {
            PostgresDbService postgresDbService = postgreConnectionHandler.createExtendedRootUserConnection(serviceInstance, plan, null, true);

            postgresCustomImplementation.createDatabase(postgresDbService, database);

            // This is fucking ugly, but necessary due to the fact that replication in the PostgreSQL
            // Backend takes some time for streaming replication
            TimeUnit.SECONDS.sleep(10);

			postgresCustomImplementation.createGeneralRole(postgresDbService, database, database);

			//TODO: Create pgpool user for bind admin
			postgresCustomImplementation.bindRoleToDatabase(postgresDbService,
					serviceInstanceUsernamePasswordCredential.getUsername(),
                    serviceInstanceUsernamePasswordCredential.getPassword(),
                    database, database,true);

			// close connection to postgresql db / open connection to bind db
			// necessary for installing db specific extensions (as admin)
			postgresDbService.closeIfConnected();
			postgresDbService = postgreConnectionHandler.createExtendedRootUserConnection(serviceInstance, plan,database, true);
			postgresCustomImplementation.createExtensions(postgresDbService);
		} catch(SQLException | InterruptedException ex) {
            throw new PlatformException(ex);
        }

        return serviceInstance;
	}

    @Override
    public ServiceInstance getInstance(ServiceInstance serviceInstance, Plan plan) {
        return serviceInstance;
    }
}
