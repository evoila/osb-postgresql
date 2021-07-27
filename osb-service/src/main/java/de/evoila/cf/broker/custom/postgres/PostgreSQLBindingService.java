package de.evoila.cf.broker.custom.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.*;
import de.evoila.cf.broker.service.AsyncBindingService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.bosh.InstanceGroupNotFoundException;
import de.evoila.cf.cpi.bosh.PostgresBoshPlatformService;
import de.evoila.cf.cpi.existing.PostgreSQLExistingServiceFactory;
import de.evoila.cf.security.credentials.CredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Johannes Hiemer.
 */
@Service
public class PostgreSQLBindingService extends BindingServiceImpl {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static String URI = "uri";

    private static String USERNAME = "user";

    private static String PASSWORD = "password";

    private static String DATABASE = "database";

    private PostgresCustomImplementation postgresCustomImplementation;

    private PostgreSQLExistingServiceFactory existingServiceFactory;

    private PostgresBoshPlatformService postgresBoshPlatformService;

    private CredentialStore credentialStore;

    private PostgreConnectionHandler postgreConnectionHandler;

    private ObjectMapper objectMapper;

    PostgreSQLBindingService(PostgresCustomImplementation customImplementation,
                             PostgreSQLExistingServiceFactory existingServiceFactory,
                             PostgresBoshPlatformService postgresBoshPlatformService,
                             BindingRepository bindingRepository, ServiceDefinitionRepository serviceDefinitionRepository,
                             ServiceInstanceRepository serviceInstanceRepository, RouteBindingRepository routeBindingRepository,
                             @Autowired(required = false) JobRepository jobRepository,
                             AsyncBindingService asyncBindingService, PlatformRepository platformRepository,
                             CredentialStore credentialStore,
                             PostgreConnectionHandler postgreConnectionHandler,
                             ObjectMapper objectMapper) {
        super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository, routeBindingRepository, jobRepository, asyncBindingService, platformRepository);
        Assert.notNull(customImplementation, "PostgresCustomImplementation may not be null");
        Assert.notNull(existingServiceFactory, "PostgreSQLExistingServiceFactory may not be null");
        this.existingServiceFactory = existingServiceFactory;
        this.postgresCustomImplementation = customImplementation;
        this.postgresBoshPlatformService = postgresBoshPlatformService;
        this.credentialStore = credentialStore;
        this.postgreConnectionHandler = postgreConnectionHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

    protected boolean ssl(ServiceInstance serviceInstance, Plan plan, boolean bindSsl) throws ServiceBrokerException {
        boolean ssl = true;
        Object sslProperty = null;
        if ((sslProperty = getMapProperty(plan.getMetadata().getCustomParameters(), "ssl", "enabled")) != null) {
            ssl = ((Boolean) sslProperty).booleanValue();
        }
        if ((sslProperty = getMapProperty(serviceInstance.getParameters(), "postgres", "ssl", "enabled")) != null) {
            ssl = ((Boolean) sslProperty).booleanValue();
        }

        if (ssl == false && bindSsl == true) {
            throw new ServiceBrokerException("Cannot use SSL on this Service Instance");
        } else {
            ssl = bindSsl;
        }
        return ssl;
    }

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) throws ServiceBrokerException {

        CustomParameters planParameters = objectMapper.convertValue(plan.getMetadata().getCustomParameters(), CustomParameters.class);
        Object sslProperty = null;
        boolean bindSsl = true;
        if ((sslProperty = getMapProperty(serviceInstanceBindingRequest.getParameters(), "ssl", "enabled")) != null) {
            bindSsl = ((Boolean) sslProperty).booleanValue();
        }
        boolean ssl = ssl(serviceInstance, plan, bindSsl);
        List<ServerAddress> hosts = new ArrayList<>();
        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        if (ingressInstanceGroup != null && ingressInstanceGroup.length() > 0) {
            hosts = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
        }


        String database = PostgreSQLUtils.dbName(serviceInstance.getId());
        if (serviceInstanceBindingRequest.getParameters() != null) {
            String customBindingDatabase = (String) serviceInstanceBindingRequest.getParameters().get("database");

            if (!StringUtils.isEmpty(customBindingDatabase))
                database = customBindingDatabase;
        }

        PostgresDbService jdbcService = null;

        credentialStore.createUser(serviceInstance, bindingId);
        UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, bindingId);
        String generalRole = database;

        try {
            jdbcService = postgreConnectionHandler.createExtendedRootUserConnection(serviceInstance,
                    plan,
                    database,
                    ssl);

            /* TODO:
                 this connection should be done with the BIND_ADMIN ("CREATE ROLE") user, not with the superuser
                 currently impossible due to missing pgpool config for this user
                 would have to be created in the existing service factory
            */
            postgresCustomImplementation.createGeneralRole(jdbcService, generalRole, database);
            postgresCustomImplementation.bindRoleToDatabase(jdbcService, usernamePasswordCredential.getUsername(),
                    usernamePasswordCredential.getPassword(), database, generalRole, false);
            jdbcService.closeIfConnected();

            // close connection to postgresql db / open connection to bind db
            // necessary to set user specific privileges inside the db
            jdbcService = postgreConnectionHandler.createExtendedBindUserConnection(serviceInstance,
                    plan,
                    database,
                    bindingId,
                    ssl);


            postgresCustomImplementation.setUpBindingUserPrivileges(jdbcService, usernamePasswordCredential.getUsername(), generalRole);
        } catch (SQLException e) {
            log.error(String.format("Creating Binding(%s) failed while creating the ne postgres user. Could not update database", bindingId), e);
            throw new ServiceBrokerException("Could not update database");
        } finally {
            jdbcService.closeIfConnected();
        }


        String endpoint = ServiceInstanceUtils.connectionUrl(hosts);
        if(planParameters.getDns() != null || planParameters.isShortDns() == true) {
            endpoint = serviceInstance.getId().replace("-","") + "." + planParameters.getDns();
        }
        
        // When host is not empty, it is a service key
        if (host != null)
            endpoint = host.getIp() + ":" + host.getPort();


        Map<String, Object> credentials = new HashMap<>();

        //Create url with secondary and all Hosts
        String sslParam = ssl ? "&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory" : "";
            String dbURL = String.format("postgres://%s:%s@%s/%s?%s", usernamePasswordCredential.getUsername(),
                    usernamePasswordCredential.getPassword(), endpoint, database, sslParam);
            credentials.put(URI, dbURL);

        credentials.put(USERNAME, usernamePasswordCredential.getUsername());
        credentials.put(PASSWORD, usernamePasswordCredential.getPassword());
        credentials.put(DATABASE, database);
        if (ssl) {
            credentials.put("sslmode", "verify-full");
            credentials.put("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        }
        return credentials;
    }

    @Override
    protected void unbindService(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
        PostgresDbService jdbcService = null;

        Object sslProperty = null;
        boolean bindSsl = true;
        if ((sslProperty = getMapProperty(binding.getParameters(), "ssl", "enabled")) != null) {
            bindSsl = ((Boolean) sslProperty).booleanValue();
        }
        boolean ssl = ssl(serviceInstance, plan, bindSsl);
        if (plan.getPlatform() == Platform.BOSH) {
            jdbcService = postgreConnectionHandler.createExtendedRootUserConnection(serviceInstance,
                    plan,
                    PostgreSQLUtils.dbName(serviceInstance.getId()),
                    ssl);
        } else if (plan.getPlatform() == Platform.EXISTING_SERVICE) {
            jdbcService = postgreConnectionHandler.createExtendedRootUserConnection(serviceInstance,
                    plan,
                    PostgreSQLUtils.dbName(serviceInstance.getId()),
                    ssl);
        } else {
            throw new ServiceBrokerException("Unknown platform utilized in plan");
        }

        try {
            UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, binding.getId());
            postgresCustomImplementation.unbindRoleFromDatabase(credentialStore, serviceInstance, plan, jdbcService, usernamePasswordCredential, ssl);
            credentialStore.deleteCredentials(serviceInstance, binding.getId());
        } catch (SQLException e) {
            throw new ServiceBrokerException("Could not remove from database");
        } finally {
            jdbcService.closeIfConnected();
        }
    }

    private Object getMapProperty(Map<String, Object> map, String... keys) {
        Map<String, Object> nextMap = map;
        Object objectMap = map;
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            map = (Map<String, Object>) objectMap;
            if (!map.containsKey(key)) {
                return null;
            }
            objectMap = map.get(key);
        }
        return objectMap;
    }
}
