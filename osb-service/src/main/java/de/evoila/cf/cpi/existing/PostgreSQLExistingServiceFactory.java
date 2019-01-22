/**
 * 
 */
package de.evoila.cf.cpi.existing;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
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

    RandomString usernameRandomString = new RandomString(10);
    RandomString passwordRandomString = new RandomString(15);

    private PostgresCustomImplementation postgresCustomImplementation;

    private ExistingEndpointBean existingEndpointBean;

	public PostgreSQLExistingServiceFactory(PostgresCustomImplementation postgresCustomImplementation, ExistingEndpointBean existingEndpointBean,
											PlatformRepository platformRepository, ServicePortAvailabilityVerifier portAvailabilityVerifier) {
		super(platformRepository, portAvailabilityVerifier, existingEndpointBean);
		this.postgresCustomImplementation = postgresCustomImplementation;
	    this.existingEndpointBean = existingEndpointBean;
    }

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
		String database=serviceInstance.getId();
		PostgresDbService postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, database);
		try {
		    postgresCustomImplementation.dropAllExtensions(postgresDbService);
		} catch (SQLException e) {
			log.error(String.format("Extension drop (%s) failed while dropping the database %s", database), e);
		}
		postgresDbService.closeIfConnected();
		postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan);
        postgresCustomImplementation.deleteDatabase(postgresDbService, serviceInstance.getUsername(), database, serviceInstance.getUsername());
	}

    @Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {
        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
		String database = serviceInstance.getId();
		String generalRole = database;

	    serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

	    PostgresDbService postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan);

        postgresCustomImplementation.createDatabase(postgresDbService, database);

        try {
			postgresCustomImplementation.createGeneralRole(serviceInstance, plan, postgresDbService, serviceInstance.getId(), serviceInstance.getId());
			postgresCustomImplementation.bindRoleToDatabase(serviceInstance,plan,postgresDbService,
					username, password, database, generalRole,true);

			// close connection to postgresql db / open connection to bind db
			// necessary for installing db specific extensions (as admin)
			postgresDbService.closeIfConnected();
			postgresDbService = postgresCustomImplementation.connection(serviceInstance, plan, database);
			postgresCustomImplementation.createExtensions(postgresDbService);
		} catch(SQLException ex) {
            throw new PlatformException(ex);
        }

        return serviceInstance;
	}

    @Override
    public ServiceInstance getInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
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
