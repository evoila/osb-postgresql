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
@ConditionalOnProperty(prefix = "existing.endpoint", name = { "host", "port", "username", "password",
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
			// connection.executeUpdate("REVOKE all on database \"" + database +
			// "\" from public");
			connection.executeUpdate("DROP DATABASE \"" + database + "\"");
			connection.executeUpdate("DROP ROLE \"" + database + "\"");
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
