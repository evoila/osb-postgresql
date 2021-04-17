package de.evoila.cf.broker.custom.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.ConnectionUserType;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PostgreConnectionHandler {

    private ExistingEndpointBean existingEndpointBean;

    private CredentialStore credentialStore;

    private ObjectMapper objectMapper;

    public PostgreConnectionHandler(ExistingEndpointBean existingEndpointBean,CredentialStore credentialStore, ObjectMapper objectMapper) {
        this.existingEndpointBean = existingEndpointBean;
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
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

    private PostgresConnectionParameter prepareRootUserConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database, boolean ssl){
        return prepareConnectionParameter(serviceInstance,plan,database,ConnectionUserType.ROOT_USER,null,ssl);
    }
    
    private PostgresConnectionParameter prepareRootUserConnectionParameter(ServiceInstance serviceInstance, Plan plan, List<ServerAddress> serverAddresses, String database, boolean ssl){
        return prepareConnectionParameter(serviceInstance,plan,serverAddresses,database,ConnectionUserType.ROOT_USER,null,ssl);
    }

    private PostgresConnectionParameter prepareBindUserConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database, String bindingId,boolean ssl){
        return prepareConnectionParameter(serviceInstance,plan,database,ConnectionUserType.BIND_USER,bindingId,ssl);
    }



    private PostgresConnectionParameter prepareConnectionParameter(ServiceInstance serviceInstance, Plan plan, List<ServerAddress> serverAddress, String database,
                                                                   ConnectionUserType connectionType, String bindingId, boolean ssl) {

        CustomParameters planParameters = objectMapper.convertValue(plan.getMetadata().getCustomParameters(), CustomParameters.class);
        PostgresConnectionParameter connectionParameter = new PostgresConnectionParameter();
        if (planParameters.getDns() == null) {
            connectionParameter.setServerAddresses(serverAddress);
        } else {
            List<ServerAddress> addresses = Arrays.asList((new ServerAddress(
                    serverAddress.get(0).getName(),
                    (serviceInstance.getId().replace("-","") + "." + planParameters.getDns().get("0")),
                    serverAddress.get(0).getPort()
                    )));
            connectionParameter.setServerAddresses(addresses);
        }

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
        connectionParameter.setSsl(ssl);
        return connectionParameter;
    }


    private PostgresConnectionParameter prepareConnectionParameter(ServiceInstance serviceInstance, Plan plan, String database,
                                                                   ConnectionUserType connectionType, String bindingId,boolean ssl) {

        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
        List<ServerAddress> serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
        return prepareConnectionParameter(serviceInstance,plan,serverAddresses,database,connectionType,bindingId,ssl);
    }

    private PostgresDbService establishSimpleConnection(PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();

        jdbcService.createSimpleConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getSsl(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }

    private PostgresDbService establishExtendedConnection(PostgresConnectionParameter connectionParameter) {
        PostgresDbService jdbcService = new PostgresDbService();

        jdbcService.createExtendedConnection(
                connectionParameter.getUsernamePasswordCredential().getUsername(),
                connectionParameter.getUsernamePasswordCredential().getPassword(),
                connectionParameter.getDatabase(),
                connectionParameter.getSsl(),
                connectionParameter.getServerAddresses());
        return jdbcService;
    }


    public PostgresDbService createExtendedRootUserConnection(ServiceInstance serviceInstance, Plan plan, String database,boolean ssl) {
        return establishExtendedConnection(prepareRootUserConnectionParameter(serviceInstance,plan,database, ssl));
    }

    public PostgresDbService createExtendedRootUserConnection(ServiceInstance serviceInstance, Plan plan, List<ServerAddress> serverAddresses, String database,boolean ssl) {
        return establishExtendedConnection(prepareRootUserConnectionParameter(serviceInstance,plan,serverAddresses,database, ssl));
    }
    
    public PostgresDbService createExtendedBindUserConnection(ServiceInstance serviceInstance, Plan plan, String database, String bindingId, boolean ssl) {
        return establishExtendedConnection(prepareBindUserConnectionParameter(serviceInstance,plan,database,bindingId,ssl));
    }

    public PostgresDbService createSimpleRootUserConnection (ServiceInstance serviceInstance, Plan plan, String database, boolean ssl) {
        return establishSimpleConnection(prepareRootUserConnectionParameter(serviceInstance,plan,database,ssl));
    }

    public PostgresDbService createSimpleBindUserConnection (ServiceInstance serviceInstance, Plan plan, String database, String bindingId, boolean ssl) {
        return establishSimpleConnection(prepareBindUserConnectionParameter(serviceInstance,plan,database,bindingId,ssl));
    }
}
