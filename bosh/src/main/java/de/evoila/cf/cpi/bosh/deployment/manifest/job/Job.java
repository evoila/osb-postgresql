package de.evoila.cf.cpi.bosh.deployment.manifest.job;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.evoila.cf.cpi.bosh.deployment.manifest.network.Network;
import de.evoila.cf.cpi.bosh.deployment.manifest.ResourcePool;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
    private String name;
    private String persistent_disk;
    private int instances;
    private String resource_pool;
    private List<Template> templates;
    private List<Network> networks;
    private Map<String, Object> properties;


    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getPersistent_disk () {
        return persistent_disk;
    }

    public void setPersistent_disk (String persistent_disk) {
        this.persistent_disk = persistent_disk;
    }

    public int getInstances () {
        return instances;
    }

    public void setInstances (int instances) {
        this.instances = instances;
    }

    public String getResource_pool () {
        return resource_pool;
    }

    public void setResource_pool (String resource_pool) {
        this.resource_pool = resource_pool;
    }

    public List<Template> getTemplates () {
        if(templates == null)
            templates = new ArrayList<>();
        return templates;
    }

    public void setTemplates (List<Template> templates) {
        this.templates = templates;
    }

    public List<Network> getNetworks () {
        if(networks == null)
            networks = new ArrayList<>();
        return networks;
    }

    public void setNetworks (List<Network> networks) {
        this.networks = networks;
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
