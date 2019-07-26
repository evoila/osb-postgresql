package de.evoila.cf.broker.backup;

import de.evoila.cf.broker.bean.BackupConfiguration;
import de.evoila.cf.broker.custom.postgres.PostgreConnectionHandler;
import de.evoila.cf.broker.custom.postgres.PostgreSQLUtils;
import de.evoila.cf.broker.custom.postgres.PostgresCustomImplementation;
import de.evoila.cf.broker.custom.postgres.PostgresDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.BackupCustomService;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 */
@Service
@ConditionalOnBean(BackupConfiguration.class)
public class BackupCustomServiceImpl implements BackupCustomService {

    private BackupConfiguration backupTypeConfiguration;

    private ServiceInstanceRepository serviceInstanceRepository;

    private PostgresCustomImplementation postgresCustomImplementation;

    private ServiceDefinitionRepository serviceDefinitionRepository;

    private CredentialStore credentialStore;

    private PostgreConnectionHandler postgreConnectionHandler;

    public BackupCustomServiceImpl(BackupConfiguration backupTypeConfiguration,
                                   ServiceInstanceRepository serviceInstanceRepository,
                                   PostgresCustomImplementation postgresCustomImplementation,
                                   ServiceDefinitionRepository serviceDefinitionRepository,
                                   CredentialStore credentialStore,
                                   PostgreConnectionHandler postgreConnectionHandler) {
        this.backupTypeConfiguration = backupTypeConfiguration;
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.postgresCustomImplementation = postgresCustomImplementation;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
        this.credentialStore = credentialStore;
        this.postgreConnectionHandler = postgreConnectionHandler;
    }

    @Override
    public Map<String, String> getItems(String serviceInstanceId) throws ServiceInstanceDoesNotExistException,
            ServiceDefinitionDoesNotExistException {
        ServiceInstance serviceInstance = this.validateServiceInstanceId(serviceInstanceId);

        Plan plan = serviceDefinitionRepository.getPlan(serviceInstance.getPlanId());

        Map<String, String> result = new HashMap<>();
        if (plan.getPlatform().equals(Platform.BOSH)) {
            UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
            PostgresDbService postgresDbService = postgreConnectionHandler.createExtendedRootUserConnection( serviceInstance, plan,PostgreSQLUtils.dbName(serviceInstance.getId()));

            try {
                Map<String, String> databases = postgresDbService.executeSelect("SELECT datname FROM pg_database", "datname");

                for(Map.Entry<String, String> database : databases.entrySet())
                    result.put(database.getValue(), database.getValue());
            } catch(SQLException ex) {
                new ServiceBrokerException("Could not load databases", ex);
            }
        } else if (plan.getPlatform().equals(Platform.EXISTING_SERVICE)) {
            result.put(serviceInstanceId, serviceInstanceId);
        }

        return result;
    }

    private ServiceInstance validateServiceInstanceId(String serviceInstanceId) throws ServiceInstanceDoesNotExistException {
        ServiceInstance instance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);

        if(instance == null || instance.getHosts().size() <= 0) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }

        return instance;
    }

}
