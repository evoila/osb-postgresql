package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.cpi.ConnectionUserType;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostgreConnectionHandler {

    private ExistingEndpointBean existingEndpointBean;
    private Logger log = LoggerFactory.getLogger(PostgresCustomImplementation.class);

    private CredentialStore credentialStore;

    public PostgreConnectionHandler(ExistingEndpointBean existingEndpointBean,CredentialStore credentialStore) {
        this.existingEndpointBean = existingEndpointBean;
        this.credentialStore = credentialStore;
    }

    private UsernamePasswordCredential getBindUserCredentials (ServiceInstance serviceInstance, CredentialStore credentialStore, String bindingId) {
        return credentialStore.getUser(serviceInstance, bindingId);
    }

    private UsernamePasswordCredential getRootUserCredentials(ServiceInstance serviceInstance, Plan plan, CredentialStore credentialStore) {
        switch (plan.getPlatform()) {
            case BOSH:
                return credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
            case EXISTING_SERVICE:
                return new UsernamePasswordCredential(existingEndpointBean.getUsername(),existingEndpointBean.getPassword());
            default:
                return null;
        }
    }

    private PostgresConnectionParameter prepareRootUserConnectionParameter(String action,ServiceInstance serviceInstance, Plan plan, String database){
        return prepareConnectionParameter(action,serviceInstance,plan,database,ConnectionUserType.ROOT_USER,null);
    }

    private PostgresConnectionParameter prepareBindUserConnectionParameter(String action,ServiceInstance serviceInstance, Plan plan, String database, String bindingId){
        return prepareConnectionParameter(action,serviceInstance,plan,database,ConnectionUserType.BIND_USER,bindingId);
    }

    private PostgresConnectionParameter prepareConnectionParameter(String action,ServiceInstance serviceInstance, Plan plan, String database,
                                                                   ConnectionUserType connectionType, String bindingId) {

        PostgresConnectionParameter connectionParameter = new PostgresConnectionParameter();
        connectionParameter.setServerAddresses(serviceInstance, plan);

        switch (connectionType) {
            case ROOT_USER:
                connectionParameter.setUsernamePasswordCredential(getRootUserCredentials(serviceInstance,plan,credentialStore));
                break;
            case BIND_USER:
                connectionParameter.setUsernamePasswordCredential(getBindUserCredentials(serviceInstance,credentialStore,bindingId));
        }

        log.error("REAL_CREDS: " + connectionParameter.getUsernamePasswordCredential().getUsername() + "/" + connectionParameter.getUsernamePasswordCredential().getPassword());
        if (database == null) {
            log.error("HIER - PREP_START - "+action+": USER="+connectionParameter.getUsernamePasswordCredential().getUsername()+" DB=null");
            switch (plan.getPlatform()) {
                case BOSH:
                    database = PostgreSQLUtils.dbName(serviceInstance.getId());
                    break;
                case EXISTING_SERVICE:
                    database = existingEndpointBean.getDatabase();
                    break;
            }
        } else {
            log.error("HIER - PREP_START - "+action+": USER="+connectionParameter.getUsernamePasswordCredential().getUsername()+" DB="+database);
        }

        log.error("HIER - PREP_END - "+action+": USER="+connectionParameter.getUsernamePasswordCredential().getUsername()+" DB="+database);

        connectionParameter.setDatabase(database);
        return connectionParameter;
    }

    private PostgresDbService establishSimpleConnection(String action, PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();
        log.error("HIER - SIM - "+action+": "+connectionParameter.getUsernamePasswordCredential().getUsername() + " / " + connectionParameter.getDatabase()+ "-"+connectionParameter.getUsernamePasswordCredential().getPassword());

        jdbcService.createSimpleConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }

    private PostgresDbService establishExtendedConnection(String action, PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();
        log.error("HIER - EXT - "+action+": "+connectionParameter.getUsernamePasswordCredential().getUsername() + " / " + connectionParameter.getDatabase() + "-"+connectionParameter.getUsernamePasswordCredential().getPassword());

        jdbcService.createExtendedConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }


    public PostgresDbService createExtendedRootUserConnection(String action, ServiceInstance serviceInstance, Plan plan, String database) {
        return establishExtendedConnection(action,prepareRootUserConnectionParameter(action,serviceInstance,plan,database));
    }

    public PostgresDbService createExtendedBindUserConnection(String action, ServiceInstance serviceInstance, Plan plan, String database, String bindingId) {
        return establishExtendedConnection(action,prepareBindUserConnectionParameter(action,serviceInstance,plan,database,bindingId));
    }

    public PostgresDbService createSimpleRootUserConnection (String action, ServiceInstance serviceInstance, Plan plan, String database) {
        return establishSimpleConnection(action,prepareRootUserConnectionParameter(action,serviceInstance,plan,database));
    }

    public PostgresDbService createSimpleBindUserConnection (String action, ServiceInstance serviceInstance, Plan plan, String database, String bindingId) {
        return establishSimpleConnection(action,prepareBindUserConnectionParameter(action,serviceInstance,plan,database,bindingId));
    }
}
