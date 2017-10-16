package de.evoila.cf.cpi.bosh.deployment.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(
      ignoreUnknown = true
)
public class Stemcell {
    private String name;
    private String version;
    private String cid;
    private List<String> deployments = new ArrayList();

    public Stemcell() {
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getCid() {
        return this.cid;
    }

    public void setName (String name) {
        this.name = name;
    }

    public void setVersion (String version) {
        this.version = version;
    }

    public void setCid (String cid) {
        this.cid = cid;
    }

    public List<String> getDeployments () {
        return deployments;
    }

    public void setDeployments (List<String> deployments) {
        this.deployments = deployments;
    }

}