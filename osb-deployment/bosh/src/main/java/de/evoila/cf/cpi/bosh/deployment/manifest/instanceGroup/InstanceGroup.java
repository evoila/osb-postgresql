package de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup;

import ch.qos.logback.core.spi.LifeCycle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.evoila.cf.cpi.bosh.deployment.manifest.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(
      ignoreUnknown = true
)
public class InstanceGroup {
    private String name;
    private List<String> azs;
    private int instances;
    private List<JobV2> jobs;
    private String stemcell;
    private List<NetworkV2> networks;
    private Update update;
    private LifeCycle lifecycle;
    private Map<String, Object> properties;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public List<String> getAzs () {
        if(azs == null)
            azs = new ArrayList<>();
        return azs;
    }

    public void setAzs (List<String> azs) {
        this.azs = azs;
    }

    public int getInstances () {
        return instances;
    }

    public void setInstances (int instances) {
        this.instances = instances;
    }

    public List<JobV2> getJobs () {
        if(jobs == null)
            jobs = new ArrayList<>();
        return jobs;
    }

    public void setJobs (List<JobV2> jobs) {
        this.jobs = jobs;
    }

    public String getStemcell () {
        return stemcell;
    }

    public void setStemcell (String stemcell) {
        this.stemcell = stemcell;
    }

    public List<NetworkV2> getNetworks () {
        if(networks == null)
            networks = new ArrayList<>();
        return networks;
    }

    public void setNetworks (List<NetworkV2> networks) {
        this.networks = networks;
    }

    public Update getUpdate () {
        return update;
    }

    public void setUpdate (Update update) {
        this.update = update;
    }

    public LifeCycle getLifecycle () {
        return lifecycle;
    }

    public void setLifecycle (LifeCycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Map<String, Object> getProperties () {
        if(properties == null)
            properties = new HashMap<>();
        return properties;
    }

    public void setProperties (Map<String, Object> properties) {
        this.properties = properties;
    }
}
