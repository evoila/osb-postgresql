package de.evoila.cf.broker.custom.postgres;

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
    boolean ssl;

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

    public boolean getSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

        public void setServerAddresses(ServiceInstance serviceInstance, Plan plan) {
        String ingressInstanceGroup = plan.getMetadata().getIngressInstanceGroup();
        this.serverAddresses=ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), ingressInstanceGroup);
    }

    public void setUsernamePasswordCredential(UsernamePasswordCredential usernamePasswordCredential) {
        this.usernamePasswordCredential=usernamePasswordCredential;
    }
}
