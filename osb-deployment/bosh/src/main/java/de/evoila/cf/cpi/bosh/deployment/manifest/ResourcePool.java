package de.evoila.cf.cpi.bosh.deployment.manifest;

import java.util.HashMap;
import java.util.Map;

public class ResourcePool {
    private String name;
    private String network;
    private Stemcell stemcell;
    private Map<String, Object> cloud_properties = new HashMap<>();

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getNetwork () {
        return network;
    }

    public void setNetwork (String network) {
        this.network = network;
    }

    public Stemcell getStemcell () {
        return stemcell;
    }

    public void setStemcell (Stemcell stemcell) {
        this.stemcell = stemcell;
    }

    public Map<String, Object> getCloud_properties () {
        return cloud_properties;
    }

    public void setCloud_properties (Map<String, Object> cloud_properties) {
        this.cloud_properties = cloud_properties;
    }
}
