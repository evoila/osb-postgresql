package de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.evoila.cf.cpi.bosh.deployment.manifest.Stemcell;

import java.util.Map;

@JsonIgnoreProperties(
      ignoreUnknown = true
)
public class JobV2 {
    private String name;
    private String release;
    private Map<String, Object> consumes;
    private Map<String, Object> provides;
    private Map<String, Object> properties;
    private String vm_type;
    private String vm_extensions;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getRelease () {
        return release;
    }

    public void setRelease (String release) {
        this.release = release;
    }

    public Map<String, Object> getConsumes () {
        return consumes;
    }

    public void setConsumes (Map<String, Object> consumes) {
        this.consumes = consumes;
    }

    public Map<String, Object> getProvides () {
        return provides;
    }

    public void setProvides (Map<String, Object> provides) {
        this.provides = provides;
    }

    public Map<String, Object> getProperties () {
        return properties;
    }

    public void setProperties (Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getVm_type () {
        return vm_type;
    }

    public void setVm_type (String vm_type) {
        this.vm_type = vm_type;
    }

    public String getVm_extensions () {
        return vm_extensions;
    }

    public void setVm_extensions (String vm_extensions) {
        this.vm_extensions = vm_extensions;
    }
}
