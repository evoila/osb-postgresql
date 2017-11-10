package de.evoila.cf.cpi.bosh.deployment.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(
      ignoreUnknown = true
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Stemcell {
    private String name;
    private String alias;
    private String version;
    private String cid;
    private String os;
    private List<String> deployments = new ArrayList();

    public Stemcell(){};

    public Stemcell (String alias, String stemcellVersion, String stemcellOs) {
        this.alias = alias;
        this.version = stemcellVersion;
        this.os = stemcellOs;
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

    public String getOs () {
        return os;
    }

    public void setOs (String os) {
        this.os = os;
    }

    public String getAlias () {
        return alias;
    }

    public void setAlias (String alias) {
        this.alias = alias;
    }
}