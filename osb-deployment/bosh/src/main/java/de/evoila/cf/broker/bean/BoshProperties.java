package de.evoila.cf.broker.bean;

import de.evoila.cf.cpi.bosh.deployment.manifest.Stemcell;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Created by reneschollmeyer, evoila on 09.10.17.
 */
@Service
@ConfigurationProperties(prefix = "bosh")
@ConditionalOnProperty(prefix = "bosh", name = {"host", "username", "password", "stemcellVersion", "stemcellOs"}, havingValue = "")
public class BoshProperties {

    private String host;

    private String username;

    private String password;
    private String stemcellVersion;
    private String stemcellOs;

    public String getHost () {
        return host;
    }

    public void setHost (String host) {
        this.host = host;
    }

    public String getUsername () {
        return username;
    }

    public void setUsername (String username) {
        this.username = username;
    }

    public String getPassword () {
        return password;
    }

    public void setPassword (String password) {
        this.password = password;
    }

    public String getStemcellVersion () {
        return stemcellVersion;
    }

    public void setStemcellVersion (String stemcellVersion) {
        this.stemcellVersion = stemcellVersion;
    }

    public String getStemcellOs () {
        return stemcellOs;
    }

    public void setStemcellOs (String stemcellOs) {
        this.stemcellOs = stemcellOs;
    }
}
