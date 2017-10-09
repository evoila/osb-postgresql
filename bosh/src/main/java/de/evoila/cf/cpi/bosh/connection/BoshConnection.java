package de.evoila.cf.cpi.bosh.connection;

import io.bosh.client.DirectorClient;
import io.bosh.client.SpringDirectorClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Created by reneschollmeyer, evoila on 09.10.17.
 */
public class BoshConnection {

    private final static Logger log = LoggerFactory.getLogger(BoshConnection.class);

    protected DirectorClient directorClient;

    private String host;
    private String username;
    private String password;

    public BoshConnection (String username, String password, String host){
        Assert.notNull(host, "Bosh Director Host may not be empty, when initializing");
        Assert.notNull(username, "Bosh Director Username may not be empty, when initializing");
        Assert.notNull(password, "Bosh Director Password may not be empty, when initializing");
        this.host = host;
        this.username = username;
        this.password = password;
    }


    public BoshConnection authenticate() {
        directorClient = new SpringDirectorClientBuilder()
                               .withHost(host)
                               .withCredentials(username, password)
                               .build();
    }


    public DirectorClient connection() {
        this.authenticate();
        Assert.notNull(directorClient, "Connection must be initialized before calling any methods on it");
        return directorClient;
    }
}
