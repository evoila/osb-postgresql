package de.evoila.cf.cpi.bosh.deployment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import io.bosh.client.deployments.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeploymentManager {
    private final ObjectReader reader;
    private final ObjectMapper mapper;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public DeploymentManager() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        this.reader = mapper.readerFor(Manifest.class);
    }

    protected void replaceParameters (Manifest manifest, Plan plan) {
        manifest.getProperties().putAll(plan.getMetadata());
        HashMap<String, HashMap<String, List<String>>> params = new HashMap<>();
        HashMap<String, List<String>> p = new HashMap<>();
        List<String> l = new ArrayList<>();
        l.add("root");
        p.put("additional_roles", l);
        params.put("auth", p);
        manifest.getProperties().put("mongodb", params);
    }

    public Deployment createDeployment (ServiceInstance instance, Plan plan) throws IOException, URISyntaxException {
        Deployment deployment = new Deployment();
        deployment.setName(instance.getId());
        Manifest manifest = readTemplate("bosh/manifest.yml");
        manifest.setName(instance.getId());
        replaceParameters(manifest,plan);
        deployment.setRawManifest(generateManifest(manifest));
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
        Manifest manifest = mapper.readValue(deployment.getRawManifest(), Manifest.class);
        replaceParameters(manifest,plan);
        deployment.setRawManifest(generateManifest(manifest));
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
