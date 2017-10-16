package de.evoila.cf.cpi.bosh.manifest;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class ManifestTest {
    public static final String DIRECTOR_UUID = "9b61cd26-8e25-4272-b45d-340eaaf47f08";
    public static final String DEPLOYMENT_NAME = "deployment-name";
    public static final String RELEASE_NAME = "release";

    public static final int CANARIES = 1;
    public static final String UPDATE_WATCH_TIME = "15000-30000";
    public static final String CANARY_WATCH_TIME = "9000-55000";
    public static final int MAX_IN_FLIGHT = 2;

    public static final String R_POOL_NAME = "R-Pool";
    public static final String R_NETWORK_NAME = "default";

    public static final String STEMCELL_NAME = "bosh-warden-boshlite-ubuntu-trusty-go_agent";
    public static final String STEMCELL_VERSION = "latest";

    public static final String JOB_NAME = "job1";
    public static final int INSTANCES = 3;
    public static final String JOB_PERSISTENT_DISK = "10_240";
    public static final String JOB_RESOURCE_POOL_NAME = "R-Pool";

    public static final String TEMPLATE1 = "default";
    public static final String TEMPLATE2 = "mongodb3";

    public static final String GATEWAY = "10.241.143.1";
    public static final String RANGE = "10.241.143.0/24";
    public static final String DNS = "10.254.174.10";
    public static final String N_CP_NAME = "Network";
    public static final String NETWORK_NAME = "Network";
    public static final String STATIC_IP = "10.241.143.44";
    public static final String RESERVED = "10.241.143.44";


    public String readFile(String path) throws IOException, URISyntaxException {
        InputStream inputStream = new ClassPathResource(path).getInputStream();
        return readFile(inputStream);
    }

    protected String readFile(InputStream inputStream) throws IOException, URISyntaxException {
        BufferedReader reader =new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();

        String line = reader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
            line = reader.readLine();
        }

        return stringBuilder.toString();
    }

}
