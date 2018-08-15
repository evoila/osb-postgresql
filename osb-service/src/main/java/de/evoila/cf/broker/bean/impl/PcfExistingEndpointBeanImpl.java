package de.evoila.cf.broker.bean.impl;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.ServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Profile("pcf")
@Configuration
@ConfigurationProperties(prefix="existing.endpoint")
public class PcfExistingEndpointBeanImpl implements ExistingEndpointBean {

    public List<String> pcfHosts;

    private List<ServerAddress> hosts = new ArrayList<>();

    private int port;

    private String username;

    private String password;

    private String database;

    private String deployment;

    public List<String> getPcfHosts() {
        return pcfHosts;
    }

    @Value("${postgres.hosts}")
    public void setPcfHosts(List<String> pcfHosts) {
        for (String host : pcfHosts) {
            hosts.add(new ServerAddress("pgpool", host, this.port));
        }

        this.pcfHosts = pcfHosts;
    }

    @Override
    public List<ServerAddress> getHosts() {
        return hosts;
    }

    public void setHosts(List<ServerAddress> hosts) {
        this.hosts = hosts;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Override
    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }
}
