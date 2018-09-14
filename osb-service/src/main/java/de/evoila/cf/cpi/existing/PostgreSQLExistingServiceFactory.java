/**
 * 
 */
package de.evoila.cf.cpi.existing;

import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Plan;
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
 * @author Christian Brinker, evoila.
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

	private void createDatabase(PostgresDbService connection, String database) throws PlatformException {
		try {
			connection.executeUpdate("CREATE DATABASE \"" + database + "\" ENCODING 'UTF8'");
			connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM PUBLIC");
			connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM PUBLIC");
		} catch (SQLException e) {
			throw new PlatformException("Could not add to database", e);
		}
	}

    private void deleteDatabase(PostgresDbService connection, String username, String database) throws PlatformException {
		try {
			connection.executeUpdate("ALTER DATABASE\"" + database + "\" OWNER TO \"" + username + "\"");
//			connection.executeUpdate("REASSIGN OWNED BY \"" + database + "\" TO \"" + username + "\"");
			connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM \"" + username + "\"");
			connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM \"" + username + "\"");
			connection.executeUpdate("SELECT * FROM pg_stat_activity WHERE datname = '" + database + "';");
			connection.executeUpdate("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '" + database + "' AND pid <> pg_backend_pid();");
			connection.executeUpdate("UPDATE pg_database SET datallowconn = 'false' WHERE datname = '" + database + "';");
			connection.executeUpdate("ALTER DATABASE\"" + database + "\" CONNECTION LIMIT 1;");
//			connection.executeUpdate("DROP OWNED BY \"" + database+ "\"");
//			connection.executeUpdate("DROP ROLE \"" + database + "\"");
			connection.executeUpdate("DROP DATABASE \"" + database + "\"");
		} catch (SQLException e) {
			throw new PlatformException("Could not remove from database", e);
		}
	}

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
	    PostgresDbService postgresDbService = this.connection(serviceInstance, plan);
	    deleteDatabase(postgresDbService, serviceInstance.getUsername(), serviceInstance.getId());
	}

	@Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {
        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();

	    serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

	    PostgresDbService postgresDbService = this.connection(serviceInstance, plan);

        createDatabase(postgresDbService, serviceInstance.getId());

        try {
            postgresCustomImplementation.bindRoleToDatabase(postgresDbService,
                    username, password, serviceInstance.getId(), true);

            postgresDbService.executeUpdate("ALTER ROLE \"" + username + "\" CREATEROLE");
        } catch(SQLException ex) {
            throw new PlatformException(ex);
        }

        return serviceInstance;
	}

    private PostgresDbService connection(ServiceInstance serviceInstance, Plan plan) {
        PostgresDbService jdbcService = new PostgresDbService();

        if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHosts());

        return jdbcService;
    }

    public void createPgPoolUser(PostgresBoshPlatformService postgresBoshPlatformService, String username, String password) throws JSchException {{
		postgresBoshPlatformService.createPgPoolUser(
			this.existingEndpointBean.getDeployment(),
			this.existingEndpointBean.getHosts().get(0).getName(),
			this.existingEndpointBean.getHosts(),
			username,
			password
		);
		}
	}
}
