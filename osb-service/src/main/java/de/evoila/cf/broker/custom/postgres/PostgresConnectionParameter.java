package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.util.ServiceInstanceUtils;

import java.util.List;

public class PostgresConnectionParameter {
    List<ServerAddress> serverAddresses;
    UsernamePasswordCredential usernamePasswordCredential;
    String database;

    public List<ServerAddress> getServerAddresses() {
        return serverAddresses;
    }

    public UsernamePasswordCredential getUsernamePasswordCredential() {
        return usernamePasswordCredential;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setServerAddresses(ServiceInstance serviceInstance, Plan plan) {
        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        this.serverAddresses=ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
    }

    public void setUsernamePasswordCredential(ServiceInstance serviceInstance, Plan plan,
                                                                    UsernamePasswordCredential usernamePasswordCredential,
                                                                    ExistingEndpointBean existingEndpointBean) {
        if (plan.getPlatform() == Platform.BOSH) {
            if (usernamePasswordCredential == null) {
                usernamePasswordCredential = new UsernamePasswordCredential(
                        serviceInstance.getUsername(),
                        serviceInstance.getPassword());
            }
        } else if (plan.getPlatform() == Platform.EXISTING_SERVICE) {
            if (usernamePasswordCredential == null) {
                usernamePasswordCredential = new UsernamePasswordCredential(
                        existingEndpointBean.getUsername(),
                        existingEndpointBean.getPassword());
            }
        }
        this.usernamePasswordCredential=usernamePasswordCredential;
    }

}
