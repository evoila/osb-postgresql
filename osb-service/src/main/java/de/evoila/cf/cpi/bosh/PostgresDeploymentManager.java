package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import org.springframework.core.env.Environment;

import java.util.*;

public class PostgresDeploymentManager extends DeploymentManager {

    public PostgresDeploymentManager(BoshProperties properties, Environment environment) {
        super(properties, environment);
    }

    private RandomString randomStringPcp = new RandomString(15);
    private RandomString randomStringPassword = new RandomString(15);

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String, Object> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty())
            properties.putAll(customParameters);

        log.debug("Updating Deployment Manifest, replacing parameters");

        Map<String, Object> postgresManifestProperties = manifestProperties("postgres", manifest);
        Map<String, Object> pgpoolManifestProperties = manifestProperties("pgpool", manifest);

        HashMap<String, Object> pcp = (HashMap<String, Object>) pgpoolManifestProperties.get("pcp");
        HashMap<String, Object> postgres = (HashMap<String, Object>) postgresManifestProperties.get("postgres");
        HashMap<String, Object> postgresExporter = (HashMap<String, Object>) postgresManifestProperties.get("postgres_exporter");

        pcp.put("system_password", randomStringPcp.nextString());

        String password = randomStringPassword.nextString();
        List<HashMap<String, Object>> users = (List<HashMap<String, Object>>) postgres.get("users");

        HashMap<String, Object> userProperties = users.get(0);
        String username = userProperties.get("username").toString();
        userProperties.put("password", password);

        serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

        postgresExporter.put("password", password);

        List<Map<String, Object>> databases = new ArrayList<>();

        List<String> user= new ArrayList<>();
        user.add("admin");

        Map<String, Object> database = new HashMap<>();
        database.put("name", serviceInstance.getId());
        database.put("users", user);

        List<String> extensionsToInstall = Arrays.asList("postgis", "postgis_topology", "fuzzystrmatch", "address_standardizer", "postgis_tiger_geocoder");
        database.put("extensions", extensionsToInstall);
        databases.add(database);

        postgres.put("databases", databases);

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
