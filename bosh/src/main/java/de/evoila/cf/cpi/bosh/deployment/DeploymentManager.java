package de.evoila.cf.cpi.bosh.deployment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.xml.internal.ws.developer.Serialization;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import io.bosh.client.deployments.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.cloudfoundry.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public class DeploymentManager {
    private final ObjectReader reader;
    private final ObjectMapper mapper;
    private final Logger log = LoggerFactory.getLogger(this.getClass());



    DeploymentManager() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        this.reader = mapper.readerFor(Manifest.class);
    }

    protected String replaceParameters (String templateDeployment, Plan plan) { return templateDeployment;}

    public Deployment createDeployment (ServiceInstance instance, Plan plan) throws IOException, URISyntaxException {
        Deployment deployment = new Deployment();
        deployment.setName(instance.getId());
        String templateDeployment = accessTemplate("bosh/manifest.yml");

        String deploymentManifest = replaceParameters(templateDeployment, plan);

        deployment.setManifestMap(reader.readValue(deploymentManifest));
        return deployment;
    }

    public Manifest readTemplate(String path) throws IOException, URISyntaxException {
        String manifest = accessTemplate(path);
        return mapper.readValue(manifest,Manifest.class);
    }

    public String generateManifest(Manifest manifest) throws JsonProcessingException {
        return mapper.writeValueAsString(manifest);
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
            stringBuilder.append("\n");
            line = reader.readLine();
        }

        return stringBuilder.toString();
    }


}
