/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServerAddress;
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

	public void createGeneralRole(PostgresDbService jdbcService, String generalrole, String database) throws SQLException {
		if(!checkIfRoleExists(jdbcService,generalrole)) {
			jdbcService.executeUpdate("CREATE ROLE \"" + generalrole + "\" NOLOGIN");
			jdbcService.executeUpdate("GRANT CREATE ON DATABASE \"" + database + "\" TO \"" + generalrole + "\"");
			jdbcService.executeUpdate("GRANT CONNECT ON DATABASE \"" + database + "\" TO \"" + generalrole + "\"");
		}
	}

	public void createExtensions(PostgresDbService jdbcService) throws SQLException {
		Map<String, String> availableExtensions = jdbcService.executeSelect("SELECT name FROM pg_available_extensions", "name");

		for(String extension:extensionsToInstall) {
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

	public void setUpBindingUserPrivileges(PostgresDbService jdbcService, String username, String generalrole) throws SQLException {
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO \"" + generalrole + "\"");

		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON TABLES TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON SEQUENCES TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON FUNCTIONS TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON TYPES TO \"" + generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username + "\" GRANT ALL PRIVILEGES ON SCHEMAS TO \"" + generalrole + "\"");
	}

	public void breakDownBindingUserPrivileges(PostgresDbService jdbcService, String username, String generalrole) throws SQLException {
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" IN SCHEMA public REVOKE ALL PRIVILEGES ON TYPES FROM \""+ generalrole + "\"");

		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON TABLES FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON SEQUENCES FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON FUNCTIONS FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON TYPES FROM \""+ generalrole + "\"");
		jdbcService.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE \"" + username +"\" REVOKE ALL ON SCHEMAS FROM \""+ generalrole + "\"");

		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON SCHEMA public FROM \""+ username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \""+ username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM \""+ username + "\"");
		jdbcService.executeUpdate("REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM \""+ username + "\"");
	}
	
	public void bindRoleToDatabase(ServiceInstance serviceInstance, Plan plan, PostgresDbService jdbcService, String username, String password, String database, String generalRole, boolean isAdmin)
			throws SQLException {

		if (isAdmin){
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\" WITH LOGIN password '" + password + "' IN ROLE \"" + generalRole + "\"");
		} else {
			jdbcService.executeUpdate("CREATE ROLE \"" + username + "\" WITH INHERIT LOGIN password '" + password + "' IN ROLE \"" + generalRole + "\"");
		}

		if (isAdmin){
			jdbcService.executeUpdate("ALTER DATABASE \"" + database + "\" OWNER TO \"" + username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON SCHEMA public TO \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public To \""+ username + "\"");
			jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public To \""+ username + "\"");
			jdbcService.executeUpdate("ALTER ROLE \"" + username + "\" CREATEROLE");
		}
	}

	public void unbindRoleFromDatabase(ServiceInstance serviceInstance, Plan plan,PostgresDbService jdbcService, String roleName) throws SQLException {
		Map<String, String> databases = jdbcService.executeSelect("SELECT datname FROM pg_database WHERE datistemplate = false and datname not like 'postgres'", "datname");

		for(Map.Entry<String, String> database : databases.entrySet()) {
			PostgresDbService jdbcService_tmp = this.connection(serviceInstance, plan, database.getValue());
			String generalRole=database.getValue();
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

	public List<ServerAddress> filterServerAddresses (ServiceInstance serviceInstance, Plan plan) {
		List<ServerAddress> serverAddresses = serviceInstance.getHosts();
		String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
		if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
			serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),ingressInstanceGroup);
		}
		return serverAddresses;
	}

    public PostgresDbService connection(ServiceInstance serviceInstance, Plan plan, String database) {
        List<ServerAddress> serverAddresses=serviceInstance.getHosts();

		String username="";
        String password="";

        if(plan.getPlatform() == Platform.BOSH) {
            username=serviceInstance.getUsername();
            password=serviceInstance.getPassword();
            if(database==null) {
				database = "admin";
			}
			serverAddresses = filterServerAddresses(serviceInstance,plan);

		} else if (plan.getPlatform() == Platform.EXISTING_SERVICE) {
			username=existingEndpointBean.getUsername();
			password=existingEndpointBean.getPassword();
			if(database==null) {
				database = existingEndpointBean.getDatabase();
			}
			if(serviceInstance.getHosts().size() == 0){
				serverAddresses=existingEndpointBean.getHosts();
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
		return connection(serviceInstance,plan,null);
	}
}
