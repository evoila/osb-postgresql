/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgresCustomImplementation {

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
		jdbcService.executeUpdate("REVOKE \""+ fallBackRoleName + "\" FROM \"" + roleName + "\"");
		jdbcService.executeUpdate("REASSIGN OWNED BY \"" + roleName + "\" TO \"" + fallBackRoleName + "\"");
		jdbcService.executeUpdate("DROP OWNED BY \"" + roleName + "\"");
		jdbcService.executeUpdate("DROP ROLE \"" + roleName + "\"");
	}

}
