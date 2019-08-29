package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.cpi.ConnectionUserType;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.stereotype.Service;

@Service
public class PostgreConnectionHandler {

    private ExistingEndpointBean existingEndpointBean;

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

    private PostgresConnectionParameter prepareRootUserConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database){
        return prepareConnectionParameter(serviceInstance,plan,database,ConnectionUserType.ROOT_USER,null);
    }

    private PostgresConnectionParameter prepareBindUserConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database, String bindingId){
        return prepareConnectionParameter(serviceInstance,plan,database,ConnectionUserType.BIND_USER,bindingId);
    }

    private PostgresConnectionParameter prepareConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database,
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

        if (database == null) {
            switch (plan.getPlatform()) {
                case BOSH:
                    database = PostgreSQLUtils.dbName(serviceInstance.getId());
                    break;
                case EXISTING_SERVICE:
                    database = existingEndpointBean.getDatabase();
                    break;
            }
        }

        connectionParameter.setDatabase(database);
        return connectionParameter;
    }

    private PostgresDbService establishSimpleConnection(PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();

        jdbcService.createSimpleConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }

    private PostgresDbService establishExtendedConnection(PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();

        jdbcService.createExtendedConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }


    public PostgresDbService createExtendedRootUserConnection(ServiceInstance serviceInstance, Plan plan, String database) {
        return establishExtendedConnection(prepareRootUserConnectionParameter(serviceInstance,plan,database));
    }

    public PostgresDbService createExtendedBindUserConnection(ServiceInstance serviceInstance, Plan plan, String database, String bindingId) {
        return establishExtendedConnection(prepareBindUserConnectionParameter(serviceInstance,plan,database,bindingId));
    }

    public PostgresDbService createSimpleRootUserConnection (ServiceInstance serviceInstance, Plan plan, String database) {
        return establishSimpleConnection(prepareRootUserConnectionParameter(serviceInstance,plan,database));
    }

    public PostgresDbService createSimpleBindUserConnection (ServiceInstance serviceInstance, Plan plan, String database, String bindingId) {
        return establishSimpleConnection(prepareBindUserConnectionParameter(serviceInstance,plan,database,bindingId));
    }
}
