package de.evoila.cf.cpi.bosh.deployment.manifest.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network  {
    private String name;
    private List<Subnet> subnets = new ArrayList<>();

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public List<Subnet> getSubnets () {
        return subnets;
    }

    public void setSubnets (List<Subnet> subnets) {
        this.subnets = subnets;
    }
}
