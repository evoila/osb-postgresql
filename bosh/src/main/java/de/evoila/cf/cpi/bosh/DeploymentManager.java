package de.evoila.cf.cpi.bosh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import io.bosh.client.deployments.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Map;

public abstract class DeploymentManager {
    private final ObjectReader reader;
    private final ObjectMapper mapper;
    private final Logger log = LoggerFactory.getLogger(this.getClass());



    DeploymentManager() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.reader = mapper.readerFor(Map.class);
    }

    protected abstract String replaceParameters (String templateDeployment, Plan plan);

    public Deployment newDeployment (ServiceInstance instance, Plan plan) throws IOException, URISyntaxException {
        Deployment deployment = new Deployment();
        deployment.setName(instance.getId());
        String templateDeployment = accessTemplate("bosh/manifest.yml");

        String deploymentManifest = replaceParameters(templateDeployment, plan);

        deployment.setManifestMap(reader.readValue(deploymentManifest));
        return deployment;
    }

    public Deployment updateDeployment (Deployment deployment, Plan plan) throws IOException {
        String deploymentManifest = replaceParameters(deployment.getRawManifest(), plan);
        deployment.setManifestMap(reader.readValue(deploymentManifest));
        return deployment;
    }

    private String accessTemplate(final String templatePath) throws IOException, URISyntaxException {
        InputStream inputStream = new ClassPathResource(templatePath).getInputStream();
        return this.readTemplateFile(inputStream);
    }

    private String readTemplateFile(InputStream inputStream) throws IOException, URISyntaxException {
        BufferedReader reader =new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();

        String line = reader.readLine();
        while (line != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }


}
