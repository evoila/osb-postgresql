package de.evoila.cf.cpi.bosh.deployment.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup.JobV2;
import de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup.NetworkV2;
import io.bosh.client.releases.Job;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroup {

    private String name;
    private int instances;

    private String vm_type;
    private String stemcell;

    private String lifecycle;
    private String persistent_disk_type;
    private List<String> azs;
    private List<NetworkV2> networks;
    private List<JobV2> jobs;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public int getInstances () {
        return instances;
    }

    public void setInstances (int instances) {
        this.instances = instances;
    }

    public String getVm_type () {
        return vm_type;
    }

    public void setVm_type (String vm_type) {
        this.vm_type = vm_type;
    }

    public String getStemcell () {
        return stemcell;
    }

    public void setStemcell (String stemcell) {
        this.stemcell = stemcell;
    }

    public String getLifecycle () {
        return lifecycle;
    }

    public void setLifecycle (String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getPersistent_disk_type () {
        return persistent_disk_type;
    }

    public void setPersistent_disk_type (String persistent_disk_type) {
        this.persistent_disk_type = persistent_disk_type;
    }

    public List<String> getAzs () {
        return azs;
    }

    public void setAzs (List<String> azs) {
        this.azs = azs;
    }

    public List<NetworkV2> getNetworks () {
        return networks;
    }

    public void setNetworks (List<NetworkV2> networks) {
        this.networks = networks;
    }

    public List<JobV2> getJobs () {
        return jobs;
    }

    public void setJobs (List<JobV2> jobs) {
        this.jobs = jobs;
    }
}
