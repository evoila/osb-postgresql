package de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(
      ignoreUnknown = true
)
public class NetworkV2 {
    String name;
    List<String> static_ips;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public List<String> getStatic_ips () {
        if(static_ips == null)
            static_ips = new ArrayList<>();
        return static_ips;
    }

    public void setStatic_ips (List<String> static_ips) {
        this.static_ips = static_ips;
    }
}
