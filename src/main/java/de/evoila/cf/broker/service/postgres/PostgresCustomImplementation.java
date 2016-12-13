/**
 * 
 */
package de.evoila.cf.broker.service.postgres;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;

import org.springframework.stereotype.Service;

import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.service.postgres.jdbc.PostgresDbService;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgresCustomImplementation implements CustomExistingService {

	public void initServiceInstance(PostgresDbService jdbcService, ServiceInstance serviceInstance, String... databases)
			throws SQLException {
		String serviceInstanceId = serviceInstance.getId();
		ServerAddress host = serviceInstance.getHosts().get(0);
		jdbcService.createConnection(host.getIp(), host.getPort(), serviceInstanceId, serviceInstanceId, serviceInstanceId);
		jdbcService.executeUpdate("CREATE ROLE \"" + serviceInstanceId + "\"");
		for (String database : databases) {
			jdbcService.executeUpdate(
					"CREATE DATABASE \"" + database + "\" OWNER \"" + serviceInstanceId + "\" ENCODING 'UTF8'");
			jdbcService.executeUpdate(
					"REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM PUBLIC");
			jdbcService.executeUpdate("REVOKE ALL ON SCHEMA public FROM public");
			jdbcService.executeUpdate(
					"REVOKE CONNECT ON DATABASE \"" + database + "\" FROM PUBLIC");
		}
	}

	public void deleteRole(PostgresDbService jdbcService, String instanceId) throws SQLException {
		jdbcService.checkValidUUID(instanceId);
		jdbcService.executeUpdate("DROP ROLE IF EXISTS \"" + instanceId + "\"");
	}

	public String bindRoleToDatabaseAndGeneratePassword(PostgresDbService jdbcService, String database, String userId) throws SQLException {
		SecureRandom random = new SecureRandom();
		String passwd = new BigInteger(130, random).toString(32);
		
		bindRoleToDatabase(jdbcService, database, userId, passwd, false);
		
		return passwd;
	}
	
	public void bindRoleToDatabase(PostgresDbService jdbcService, String database, String userId, String password, boolean isAdmin)
			throws SQLException {
		jdbcService.checkValidUUID(userId);

		jdbcService.executeUpdate("CREATE ROLE \"" + userId + "\"");
		jdbcService.executeUpdate("ALTER ROLE \"" + userId + "\" LOGIN password '" + password + "'");
//		jdbcService.executeUpdate("GRANT \"" + database + "\" TO \"" + userId + "\"");

		jdbcService
				.executeUpdate("GRANT ALL PRIVILEGES ON DATABASE \"" + database + "\" TO \"" + userId + "\"" + ((isAdmin)?" WITH GRANT OPTION":""));
//		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + database
//				+ "\" IN SCHEMA public GRANT ALL ON TABLES TO \"" + database + "\"");
//		jdbcService.executeUpdate(
//				"GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO \"" + database + "\"");
//		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + database
//				+ "\" IN SCHEMA public GRANT ALL ON SEQUENCES TO \"" + database + "\"");
	}

	public void unbindRoleFromDatabase(PostgresDbService jdbcService, String roleName, String fallBackRoleName) throws SQLException {
		jdbcService.checkValidUUID(roleName);
		jdbcService.checkValidUUID(fallBackRoleName);
		jdbcService.executeUpdate("REASSIGN OWNED BY \"" + roleName + "\" TO \"" + fallBackRoleName + "\"");
		jdbcService.executeUpdate("DROP ROLE \"" + roleName + "\"");
	}

	@Override
	public CustomExistingServiceConnection connection(String host, int port, String database, String username,
			String password) throws Exception {
		PostgresDbService jdbcService = new PostgresDbService();
		jdbcService.createConnection(host, port, database, username, password);
		return jdbcService;
	}

	@Override
	public void bindRoleToInstanceWithPassword(CustomExistingServiceConnection connection, String database,
			String username, String password) throws Exception {
		if(connection instanceof PostgresDbService) {
			PostgresDbService postgresConnection = (PostgresDbService) connection;
			
			bindRoleToDatabase(postgresConnection, database, username, password, true);
			postgresConnection.executeUpdate("ALTER ROLE \"" + username + "\" CREATEROLE");
		}
	}
}
