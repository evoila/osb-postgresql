package de.evoila.cf.cpi.bosh.connection;

import io.bosh.client.DirectorClient;
import io.bosh.client.SpringDirectorClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Created by reneschollmeyer, evoila on 09.10.17.
 */
public class BoshConnectionFactory {

    private final static Logger log = LoggerFactory.getLogger(BoshConnectionFactory.class);

    protected static DirectorClient directorClient;

    private static String host;
    private static String username;
    private static String password;

    private static BoshConnectionFactory instance = null;

    public BoshConnectionFactory setCredentials(String username, String password) {
        BoshConnectionFactory.username = username;
        BoshConnectionFactory.password = password;
        return instance;
    }

    public BoshConnectionFactory authenticate(String host) {
        BoshConnectionFactory.host = host;
        Assert.notNull(host, "Host may not be empty, when initializing");
        Assert.notNull(username, "Username may not be empty, when initializing");
        Assert.notNull(password, "Password may not be empty, when initializing");

        authenticate();
        return instance;
    }

    private static void authenticate() {
        directorClient = new SpringDirectorClientBuilder()
                .withHost(host)
                .withCredentials(username, password)
                .build();
    }

    public static DirectorClient connection() {
        authenticate();
        Assert.notNull(directorClient, "Connection must be initialized before calling any methods on it");
        return directorClient;
    }
}
