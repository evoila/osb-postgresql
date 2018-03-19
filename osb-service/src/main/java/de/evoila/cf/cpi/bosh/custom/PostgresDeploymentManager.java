package de.evoila.cf.cpi.bosh.custom;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;

import java.util.Map;

public class PostgresDeploymentManager extends DeploymentManager{

    public PostgresDeploymentManager(BoshProperties properties) {
        super(properties);
    }

    protected void replaceParameters (ServiceInstance instance, Manifest manifest, Plan plan, Map<String, String> customParameters) {

    }

}