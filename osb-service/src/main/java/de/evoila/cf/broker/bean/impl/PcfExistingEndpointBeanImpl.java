package de.evoila.cf.broker.bean.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.ServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Profile("pcf")
@Configuration
@ConfigurationProperties(prefix="existing.endpoint")
public class PcfExistingEndpointBeanImpl implements ExistingEndpointBean {

    private ObjectMapper objectMapper = new ObjectMapper();

    public String pcfHosts;

    private List<ServerAddress> hosts = new ArrayList<>();

    private int port = 5432;

    private String username;

    private String password;

    private String database;

    private String deployment;

    public String getPcfHosts() {
        return pcfHosts;
    }

    @Value("${pgpool.hosts}")
    public void setPcfHosts(String pcfHosts) throws IOException {
        List<String> pcfHostList = objectMapper.readValue(pcfHosts, new TypeReference<List<String>>(){});
        for (String host : pcfHostList) {
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
