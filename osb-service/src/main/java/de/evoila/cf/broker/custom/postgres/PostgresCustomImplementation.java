/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class PostgresCustomImplementation {

    private Logger log = LoggerFactory.getLogger(PostgresCustomImplementation.class);

	@Value("${pgpool.enabled}")
	private boolean pgpoolEnabled;

	private ExistingEndpointBean existingEndpointBean;

    private PostgresBoshPlatformService postgresBoshPlatformService;

    private List<String> extensionsToInstall = Arrays.asList("fuzzystrmatch", "postgis", "postgis_topology", "address_standardizer", "postgis_tiger_geocoder");

	public PostgresCustomImplementation(ExistingEndpointBean existingEndpointBean, PostgresBoshPlatformService postgresBoshPlatformService) {
    	this.existingEndpointBean = existingEndpointBean;
    	this.postgresBoshPlatformService = postgresBoshPlatformService;
	}

    public boolean isPgpoolEnabled(){
		return pgpoolEnabled;
	}

	public boolean checkIfRoleExists(PostgresDbService jdbcService, String roleName) throws SQLException {
		Map<String, String> existingRoles = jdbcService.executeSelect("SELECT rolname FROM pg_roles WHERE rolname='" + roleName + "'", "rolname");
		return existingRoles.containsValue(roleName);
	}

	public void setupRoleTrigger(ServiceInstance serviceInstance, Plan plan, String database, String generalrole) throws SQLException {
		PostgresDbService jdbcService_tmp = this.connection(serviceInstance, plan, database);

		String createFunction="CREATE OR REPLACE FUNCTION trg_set_owner() " +
					 " RETURNS event_trigger " +
					 " LANGUAGE plpgsql " +
					 "AS $$ " +
					 "DECLARE " +
					 "  obj record; " +
					 "  types varchar[] := ARRAY['TYPE','TABLE','SEQUENCE','INDEX','SCHEMA','FUNCTION','DOMAIN','VIEW']; " +
					 "  type varchar; " +
					 "BEGIN " +
					 "  FOREACH type IN ARRAY types LOOP" +
					 "    FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands() WHERE command_tag like 'CREATE ' || type LOOP " +
					 "      EXECUTE format('ALTER %s %s OWNER TO \"%s\"', obj.object_type, obj.object_identity, '" + generalrole + "'); " +
					 "    END LOOP; " +
					 "  END LOOP; " +
					 "END; " +
					 "$$;";
		String createTrigger="CREATE EVENT TRIGGER trg_set_owner " +
							 "ON ddl_command_end " +
							 "WHEN tag IN ('CREATE TYPE','CREATE TABLE','CREATE SEQUENCE','CREATE INDEX','CREATE SCHEMA','CREATE FUNCTION','CREATE DOMAIN','CREATE VIEW') " +
							 "EXECUTE PROCEDURE trg_set_owner();";

		jdbcService_tmp.executeUpdate(createFunction);
		jdbcService_tmp.executeUpdate(createTrigger);
		jdbcService_tmp.closeIfConnected();
	}

	public void createGeneralRole(ServiceInstance serviceInstance, Plan plan, PostgresDbService jdbcService, String generalRole, String database) throws SQLException {
		if (!checkIfRoleExists(jdbcService, generalRole)) {
			jdbcService.executeUpdate("CREATE ROLE \"" + generalRole + "\" NOLOGIN");
			jdbcService.executeUpdate("GRANT CREATE ON DATABASE \"" + database + "\" TO \"" + generalRole + "\"");
			jdbcService.executeUpdate("GRANT CONNECT ON DATABASE \"" + database + "\" TO \"" + generalRole + "\"");
		}
		setupRoleTrigger(serviceInstance, plan, database, generalRole);
	}

	public void createExtensions(PostgresDbService jdbcService) throws SQLException {
		Map<String, String> availableExtensions = jdbcService.executeSelect("SELECT name FROM pg_available_extensions", "name");

		for (String extension : extensionsToInstall) {
			if (availableExtensions.containsValue(extension)) {
				jdbcService.executeUpdate("CREATE EXTENSION IF NOT EXISTS \"" + extension + "\"");
			}
		}
	}

	public void dropAllExtensions(PostgresDbService jdbcService) throws SQLException {
		Map<String, String> installedExtensions = jdbcService.executeSelect("SELECT name FROM pg_available_extensions where installed_version is not null and name not like 'plpgsql'", "name");

		for(Map.Entry<String, String> extension : installedExtensions.entrySet()) {
			jdbcService.executeUpdate("DROP EXTENSION \"" + extension.getValue() + "\" CASCADE");
		}
	}

	public void setUpBindingUserPrivileges(PostgresDbService jdbcService, String username, String generalRole) throws SQLException {
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO \"" + generalRole + "\"");

		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON TABLES TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON SEQUENCES TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON FUNCTIONS TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON TYPES TO \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON SCHEMAS TO \"" + generalRole + "\"");
	}

	public void breakDownBindingUserPrivileges(PostgresDbService jdbcService, String username, String generalRole) throws SQLException {
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM \""+ generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM \""+ generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM \""+ generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON TYPES FROM \""+ generalRole + "\"");

		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON TABLES FROM \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON SEQUENCES FROM \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON FUNCTIONS FROM \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON TYPES FROM \"" + generalRole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON SCHEMAS FROM \"" + generalRole + "\"");

		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON SCHEMA public FROM \"" + username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \"" + username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM \"" + username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM \"" + username + "\"");
	}
	
	public void bindRoleToDatabase(ServiceInstance serviceInstance, Plan plan, PostgresDbService jdbcService, String username, String password,
                                   String database, String generalRole, boolean isAdmin)
			throws SQLException {

		if (isAdmin){
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\" WITH LOGIN password '" + password + "' IN ROLE \"" + generalRole + "\"");

            jdbcService.executeUpdate("ALTER DATABASE \"" + database + "\" OWNER TO \"" + username + "\"");
            jdbcService.executeUpdate("ALTER ROLE \"" + username + "\" CREATEROLE");
		} else {
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\" WITH INHERIT LOGIN password '" + password + "' IN ROLE \"" + generalRole + "\"");
		}

        jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON SCHEMA public TO \"" + username + "\"");
        jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \"" + username + "\"");
        jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public To \"" + username + "\"");
        jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public To \"" + username + "\"");
    }

	public void unbindRoleFromDatabase(ServiceInstance serviceInstance, Plan plan,PostgresDbService jdbcService, String roleName) throws SQLException {
		Map<String, String> databases = jdbcService.executeSelect("SELECT datname FROM pg_database WHERE datistemplate = false and datname not like 'postgres'", "datname");

		for(Map.Entry<String, String> database : databases.entrySet()) {
			PostgresDbService jdbcService_tmp = this.connection(serviceInstance, plan, database.getValue());
			String generalRole = database.getValue();
			breakDownBindingUserPrivileges(jdbcService_tmp, roleName, generalRole);
			if(!checkIfRoleExists(jdbcService,generalRole)) {
				jdbcService_tmp.executeUpdate("CREATE ROLE \"" + generalRole + "\" NOLOGIN");
			}
			jdbcService_tmp.executeUpdate("REASSIGN OWNED BY \"" + roleName + "\" TO \"" + generalRole + "\"");
			jdbcService_tmp.executeUpdate("DROP OWNED BY \"" + roleName + "\"");
			jdbcService_tmp.executeUpdate("REVOKE ALL ON SCHEMA public FROM \"" + roleName + "\"");

			jdbcService_tmp.closeIfConnected();
		}

		jdbcService.executeUpdate("DROP ROLE \"" + roleName + "\"");
	}

	public List<ServerAddress> filterServerAddresses(ServiceInstance serviceInstance, Plan plan) {
		List<ServerAddress> serverAddresses = serviceInstance.getHosts();
		String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
		if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
			serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),ingressInstanceGroup);
		}
		return serverAddresses;
	}

    public void createDatabase(PostgresDbService connection, String database) throws PlatformException {
        try {
            connection.executeUpdate("CREATE DATABASE \"" + database + "\" ENCODING 'UTF8'");
            connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM PUBLIC");
            connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM PUBLIC");
        } catch (SQLException e) {
            throw new PlatformException("Could not create database", e);
        }
    }

    public void deleteDatabase(PostgresDbService connection, String username, String database, String admRole) throws PlatformException {
        try {
            String generalrole = database;

            connection.executeUpdate("ALTER DATABASE\"" + database + "\" OWNER TO \"" + username + "\"");
            connection.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"" + database + "\" FROM \"" + username + "\"");
            connection.executeUpdate("REVOKE CONNECT ON DATABASE \"" + database + "\" FROM \"" + username + "\"");
            connection.executeUpdate("SELECT * FROM pg_stat_activity WHERE datname = '" + database + "';");
            connection.executeUpdate("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '" + database + "' AND pid <> pg_backend_pid();");
            connection.executeUpdate("UPDATE pg_database SET datallowconn = 'false' WHERE datname = '" + database + "';");
            connection.executeUpdate("ALTER DATABASE\"" + database + "\" CONNECTION LIMIT 1;");
            connection.executeUpdate("DROP DATABASE \"" + database + "\"");
            connection.executeUpdate("DROP EVENT TRIGGER trg_set_owner");
            connection.executeUpdate("DROP FUNCTION trg_set_owner");
            connection.executeUpdate("DROP ROLE \"" + generalrole + "\"");
            connection.executeUpdate("REVOKE ALL PRIVILEGES ON SCHEMA PUBLIC FROM \"" + admRole + "\"");
            connection.executeUpdate("DROP ROLE \"" + admRole + "\"");
        } catch (SQLException e) {
            throw new PlatformException("Could not remove from database", e);
        }
    }

    public PostgresDbService connection(ServiceInstance serviceInstance, Plan plan, String database) {
        List<ServerAddress> serverAddresses=serviceInstance.getHosts();

		String username = "";
        String password = "";

        if(plan.getPlatform() == Platform.BOSH) {
            username = serviceInstance.getUsername();
            password = serviceInstance.getPassword();
            if(database == null) {
				database = "admin";
			}
			serverAddresses = filterServerAddresses(serviceInstance,plan);

		} else if (plan.getPlatform() == Platform.EXISTING_SERVICE) {
            if(serviceInstance.getHosts().size() == 0){
                serverAddresses = existingEndpointBean.getHosts();
            }

            username = existingEndpointBean.getUsername();
            password = existingEndpointBean.getPassword();

            if(database == null) {
                database = existingEndpointBean.getDatabase();
            }

		}

		PostgresDbService jdbcService = new PostgresDbService();
		jdbcService.createConnection(
				username,
				password,
				database,
				serverAddresses);

        return jdbcService;
    }

	public PostgresDbService connection(ServiceInstance serviceInstance, Plan plan) {
		return connection(serviceInstance, plan, null);
	}
}
