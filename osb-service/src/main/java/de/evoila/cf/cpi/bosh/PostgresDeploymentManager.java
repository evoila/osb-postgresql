package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.credential.PasswordCredential;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.util.MapUtils;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.core.env.Environment;

import java.util.*;

/**
 * @author Marco Hennig, Johannes Hiemer.
 */
public class PostgresDeploymentManager extends DeploymentManager {

    private CredentialStore credentialStore;

    public PostgresDeploymentManager(BoshProperties properties,
                                     Environment environment,
                                     CredentialStore credentialStore) {
        super(properties, environment);
        this.credentialStore = credentialStore;
    }

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String,
            Object> customParameters, boolean isUpdate) {
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty())
            properties.putAll(customParameters);

        if (!isUpdate) {
            log.debug("Updating Deployment Manifest, replacing parameters");

            Map<String, Object> postgresManifestProperties = manifestProperties("postgres", manifest);
            Map<String, Object> pgpoolManifestProperties = manifestProperties("pgpool", manifest);

            HashMap<String, Object> pcp = (HashMap<String, Object>) pgpoolManifestProperties.get("pcp");
            HashMap<String, Object> postgres = (HashMap<String, Object>) postgresManifestProperties.get("postgres");
            HashMap<String, Object> postgresExporter = (HashMap<String, Object>) postgresManifestProperties.get("postgres_exporter");

            PasswordCredential systemPassword = credentialStore.createPassword(serviceInstance, CredentialConstants.PGPOOL_SYSTEM_PASSWORD);
            pcp.put("system_password", systemPassword.getPassword());

            List<HashMap<String, Object>> users = (List<HashMap<String, Object>>) postgres.get("users");

            HashMap<String, Object> userProperties = users.get(0);

            UsernamePasswordCredential usernamePasswordCredential = credentialStore.createUser(serviceInstance,
                    CredentialConstants.ROOT_CREDENTIALS);
            userProperties.put("username", usernamePasswordCredential.getUsername());
            userProperties.put("password", usernamePasswordCredential.getPassword());

            serviceInstance.setUsername(usernamePasswordCredential.getUsername());

            postgresExporter.put("user", usernamePasswordCredential.getUsername());
            postgresExporter.put("password", usernamePasswordCredential.getPassword());

            List<Map<String, Object>> databases = new ArrayList<>();

            List<String> user = new ArrayList<>();
            user.add(usernamePasswordCredential.getUsername());

            Map<String, Object> database = new HashMap<>();
            database.put("name", serviceInstance.getId());
            database.put("users", user);

            List<String> extensionsToInstall = Arrays.asList("postgis", "postgis_topology",
                    "fuzzystrmatch", "address_standardizer",
                    "postgis_tiger_geocoder");
            database.put("extensions", extensionsToInstall);
            databases.add(database);

            postgres.put("databases", databases);
        } else if (isUpdate && customParameters != null && !customParameters.isEmpty()) {
            for (Map.Entry parameter : customParameters.entrySet()) {
                Map<String, Object> manifestProperties = manifestProperties(parameter.getKey().toString(), manifest);

                if (manifestProperties != null)
                    MapUtils.deepMerge(manifestProperties, customParameters);
            }

        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }

    private Map<String, Object> manifestProperties(String instanceGroup, Manifest manifest) {
        return manifest
            .getInstanceGroups()
            .stream()
            .filter(i -> {
                if (i.getName().equals(instanceGroup))
                    return true;
                return false;
            }).findFirst().get().getProperties();
    }

}
