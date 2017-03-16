/**
 * 
 */
package de.evoila.cf.broker.service.postgres;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

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
	
	public void setUpBindingUserPrivileges(PostgresDbService jdbcService, String database, String userId, String password) throws SQLException {
		
		jdbcService.checkValidUUID(userId);
		
		//jdbcService.executeUpdate("GRANT \"" + database + "\" TO \"" + userId + "\"");	
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + userId +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + userId +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + userId +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + userId +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO \""+ database + "\"");
	}
	
	public void bindRoleToDatabase(PostgresDbService jdbcService, String database, String userId, String password, boolean isAdmin)
			throws SQLException {
		jdbcService.checkValidUUID(userId);
		
		if (isAdmin){
			jdbcService.executeUpdate("CREATE ROLE \"" + userId + "\"");
		} else {
			jdbcService.executeUpdate("CREATE ROLE \"" + userId + "\" WITH INHERIT");
		}
				
		jdbcService.executeUpdate("ALTER ROLE \"" + userId + "\" LOGIN password '" + password + "'");
		
		if (isAdmin){
			jdbcService.executeUpdate("ALTER DATABASE \"" + database + "\" OWNER TO \"" + userId + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON SCHEMA public TO \""+ userId + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \""+ userId + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public To \""+ userId + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public To \""+ userId + "\"");

			
		} else {
			jdbcService.executeUpdate("GRANT \"" + database + "\" TO \"" + userId + "\"");	
		}
	}

	public void unbindRoleFromDatabase(PostgresDbService jdbcService, String roleName, String fallBackRoleName) throws SQLException {
		jdbcService.checkValidUUID(roleName);
		jdbcService.checkValidUUID(fallBackRoleName);
		//jdbcService.getHost()
		//jdbcService.closeIfConnected();
		//jdbcService.createConnection(host, port, database, username, password)
		jdbcService.executeUpdate("REVOKE \""+ fallBackRoleName + "\" FROM \"" + roleName + "\"");
		jdbcService.executeUpdate("REASSIGN OWNED BY \"" + roleName + "\" TO \"" + fallBackRoleName + "\"");
		jdbcService.executeUpdate("DROP OWNED BY \"" + roleName + "\"");
		jdbcService.executeUpdate("DROP ROLE \"" + roleName + "\"");
	}

	@Override
	public CustomExistingServiceConnection connection(List<String> hosts, int port, String database, String username,
			String password) throws Exception {
		String host = hosts.get(0);
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
