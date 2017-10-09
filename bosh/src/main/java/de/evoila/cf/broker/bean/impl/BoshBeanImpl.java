package de.evoila.cf.broker.bean.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Created by reneschollmeyer, evoila on 09.10.17.
 */
@Service
@ConfigurationProperties(prefix = "bosh")
@ConditionalOnProperty(prefix = "bosh",
                       name = {"host", "username", "password"},
                       havingValue = "")
public class BoshBeanImpl {

    private String host;

    private String username;

    private String password;

    public String getHost() { return host; }

    public void setHost(String host) { this.host = host; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
