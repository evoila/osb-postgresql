/**
 * 
 */
package de.evoila.cf.broker.service.custom;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.service.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.service.postgres.jdbc.PostgresDbService;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;
import de.evoila.cf.cpi.existing.ExistingServiceFactory;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
@ConditionalOnProperty(prefix = "existing.endpoint", name = { "port", "username", "password",
		"database" }, havingValue = "")
public class PostgreSQLExistingServiceFactory extends ExistingServiceFactory {

	@Autowired
	private PostgresCustomImplementation postgres;

	protected CustomExistingService getCustomExistingService() {
		return postgres;
	}

	public void createDatabase(PostgresDbService connection, String database) throws PlatformException {
		try {
			connection.executeUpdate("CREATE DATABASE \"" + database + "\" ENCODING 'UTF8'");
			// connection.executeUpdate("REVOKE all on database " + database + "
			// from public");
			connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM PUBLIC");
			connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM PUBLIC");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not add to database");
		}
	}

	public void deleteDatabase(PostgresDbService connection, String database) throws PlatformException {
		try {
			String newDBOwner = getUsername();
			// connection.executeUpdate("REVOKE all on database \"" + database +
			// "\" from public");
			connection.executeUpdate("ALTER DATABASE\"" + database + "\" OWNER TO \"" + newDBOwner + "\"");
			connection.executeUpdate("REASSIGN OWNED BY \"" + database + "\" TO \"" + newDBOwner + "\"");
			connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM \"" + database + "\"");
			connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM \"" + database + "\"");
			connection.executeUpdate("SELECT * FROM pg_stat_activity WHERE pg_stat_activity WHERE datname = '" + database + "';");
			connection.executeUpdate("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '" + database + "' AND pid <> pg_backend_pid();");
			connection.executeUpdate("UPDATE pg_database SET datallowconn = 'false' WHERE datname = '" + database + "';");
			connection.executeUpdate("ALTER DATABASE\"" + database + "\" CONNECTION LIMIT 1;");
			connection.executeUpdate("DROP OWNED BY \"" + database+ "\"");
			connection.executeUpdate("DROP ROLE \"" + database + "\"");
			connection.executeUpdate("DROP DATABASE \"" + database + "\"");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not remove from database");
		}
	}

	@Override
	protected void deleteInstance(CustomExistingServiceConnection connection, String instanceId)
			throws PlatformException {
		if (connection instanceof PostgresDbService)
			deleteDatabase((PostgresDbService) connection, instanceId);
	}

	@Override
	protected void createInstance(CustomExistingServiceConnection connection, String instanceId)
			throws PlatformException {
		if (connection instanceof PostgresDbService)
			createDatabase((PostgresDbService) connection, instanceId);
	}
}
