package de.evoila.cf.cpi.existing;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Christian Brinker, Johannes Hiemer, Marco Hennig.
 *
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
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore.getUser(serviceInstance,
                CredentialConstants.ROOT_CREDENTIALS);

		String database = serviceInstance.getId();
		PostgresDbService postgresDbService = postgresCustomImplementation
                .connection(serviceInstance, plan, serviceInstanceUsernamePasswordCredential, database);
		try {
		    postgresCustomImplementation.dropAllExtensions(postgresDbService);
		} catch (SQLException e) {
			log.error(String.format("Extension drop (%s) failed while dropping the database %s", database), e);
		}

		postgresDbService.closeIfConnected();
		postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, null);
        postgresCustomImplementation.deleteDatabase(postgresDbService, serviceInstance.getUsername(), database, serviceInstance.getUsername());
	}

    @Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore
                .createUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        serviceInstance.setUsername(serviceInstanceUsernamePasswordCredential.getUsername());

		String database = serviceInstance.getId();
		String generalRole = database;

	    PostgresDbService postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, null);
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
			postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, null, database);
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

    public void createPgPoolUser(PostgresBoshPlatformService postgresBoshPlatformService, String username, String password) throws JSchException {{
		postgresBoshPlatformService.createPgPoolUser(
			this.existingEndpointBean.getDeployment(),
			this.existingEndpointBean.getHosts().get(0).getName(),
			this.existingEndpointBean.getHosts(),
			username,
			password);
		}
	}
}
