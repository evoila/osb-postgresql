package de.evoila.cf.cpi.bosh.deployment.manifest;

import java.util.HashMap;

public class Compilation {
    private int workers;
    private boolean reuse_compilation_vms;
    private String network;
    private HashMap<String, Object> cloud_properties;


    public int getWorkers () {
        return workers;
    }

    public void setWorkers (int workers) {
        this.workers = workers;
    }

    public boolean isReuse_compilation_vms () {
        return reuse_compilation_vms;
    }

    public void setReuse_compilation_vms (boolean reuse_compilation_vms) {
        this.reuse_compilation_vms = reuse_compilation_vms;
    }

    public String getNetwork () {
        return network;
    }

    public void setNetwork (String network) {
        this.network = network;
    }

    public HashMap<String, Object> getCloud_properties () {
        return cloud_properties;
    }

    public void setCloud_properties (HashMap<String, Object> cloud_properties) {
        this.cloud_properties = cloud_properties;
    }
}
