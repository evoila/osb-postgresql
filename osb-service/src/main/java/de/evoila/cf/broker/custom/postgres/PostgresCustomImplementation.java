/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;
import sun.jvm.hotspot.debugger.Address;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgresCustomImplementation implements CustomExistingService {

    private Logger log = LoggerFactory.getLogger(PostgresCustomImplementation.class);

	public void setUpBindingUserPrivileges(PostgresDbService jdbcService, String username, String database) throws SQLException {
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO \""+ database + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO \""+ database + "\"");
	}
	
	public void bindRoleToDatabase(PostgresDbService jdbcService, String username, String password, String database, boolean isAdmin)
			throws SQLException {

		if (isAdmin){
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\"");
		} else {
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\" WITH INHERIT");
		}
				
		jdbcService.executeUpdate("ALTER ROLE \"" + username + "\" LOGIN password '" + password + "'");
		
		if (isAdmin){
			jdbcService.executeUpdate("ALTER DATABASE \"" + database + "\" OWNER TO \"" + username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON SCHEMA public TO \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public To \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public To \""+ username + "\"");

			
		} else {
			jdbcService.executeUpdate("GRANT \"" + database + "\" TO \"" + username + "\"");
		}
	}

	public void unbindRoleFromDatabase(PostgresDbService jdbcService, String roleName, String fallBackRoleName) throws SQLException {
		jdbcService.checkValidUUID(roleName);
		jdbcService.checkValidUUID(fallBackRoleName);
		jdbcService.executeUpdate("REVOKE \""+ fallBackRoleName + "\" FROM \"" + roleName + "\"");
		jdbcService.executeUpdate("REASSIGN OWNED BY \"" + roleName + "\" TO \"" + fallBackRoleName + "\"");
		jdbcService.executeUpdate("DROP OWNED BY \"" + roleName + "\"");
		jdbcService.executeUpdate("DROP ROLE \"" + roleName + "\"");
	}

	@Override
	public CustomExistingServiceConnection connection(List<String> hosts, int port, String database, String username,
			String password) throws Exception {
		PostgresDbService jdbcService = new PostgresDbService();

        List<ServerAddress> serverAddresses = new ArrayList<>();
        for (String address : hosts) {
            serverAddresses.add(new ServerAddress("", address, port));
            log.info("Opening connection to " + address + ":" + port);
        }

		jdbcService.createConnection(username, password, database, serverAddresses);
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
