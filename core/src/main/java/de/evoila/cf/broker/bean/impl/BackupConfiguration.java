package de.evoila.cf.broker.bean.impl;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix="backup")
@ConditionalOnProperty(prefix = "backup", value = {"uri"}, havingValue = "")

public class BackupConfiguration {
    private String authToken;
    private String uri;
    private String password;
    private String user;

    public String getAuthToken () {
        return authToken;
    }

    public String getUri () {
        return uri;
    }

    public String getPassword () {
        return password;
    }

    public String getUser () {
        return user;
    }
}
