package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresDeploymentManager extends DeploymentManager {

    public PostgresDeploymentManager(BoshProperties properties) {
        super(properties);
    }

    private RandomString randomStringPcp = new RandomString(15);
    private RandomString randomStringPassword = new RandomString(15);

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String, Object> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty())
            properties.putAll(customParameters);

        log.debug("Updating Deployment Manifest, replacing parameters");

        HashMap<String, Object> manifestProperties = (HashMap<String, Object>) manifest
                .getInstanceGroups()
                .stream()
                .filter(i -> {
                  if (i.getName().equals("postgres"))
                      return true;
                  return false;
                }).findFirst().get().getProperties();

        HashMap<String, Object> pcp = (HashMap<String, Object>) manifestProperties.get("pcp");
        HashMap<String, Object> pgppool = (HashMap<String, Object>) manifestProperties.get("pgpool");
        HashMap<String, Object> postgresExporter = (HashMap<String, Object>) manifestProperties.get("postgres_exporter");

        pcp.put("system_password", randomStringPcp.nextString());

        String password = randomStringPassword.nextString();
        List<HashMap<String, Object>> users = (List<HashMap<String, Object>>) pgppool.get("users");

        HashMap<String, Object> userProperties = users.get(0);
        String username = userProperties.get("username").toString();
        userProperties.put("password", password);

        serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

        postgresExporter.put("password", password);

        this.updateInstanceGroupConfiguration(manifest, plan);
    }

}
