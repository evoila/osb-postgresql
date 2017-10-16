package de.evoila.cf.cpi.bosh.deployment.manifest.network;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Subnet {
    private Map<String, Object> cloud_properties;
    private List<String> dns;
    private String gateway;
    private String range;
    private List<String> reserved;
    private List<String> static_ips;

    public Map<String, Object> getCloud_properties () {
        return cloud_properties;
    }

    public void setCloud_properties (Map<String, Object> cloud_properties) {
        this.cloud_properties = cloud_properties;
    }

    public List<String> getDns () {
        if(dns == null)
            this.dns = new ArrayList<>();
        return dns;
    }

    public void setDns (List<String> dns) {
        this.dns = dns;
    }

    public String getGateway () {
        return gateway;
    }

    public void setGateway (String gateway) {
        this.gateway = gateway;
    }

    public String getRange () {
        return range;
    }

    public void setRange (String range) {
        this.range = range;
    }

    public List<String> getReserved () {
        if(reserved == null)
            this.reserved = new ArrayList<>();
        return reserved;
    }

    public void setReserved (List<String> reserved) {
        this.reserved = reserved;
    }

    public List<String> getStatic_ips () {
        if(static_ips == null)
            this.static_ips = new ArrayList<>();
        return static_ips;
    }

    public void setStatic_ips (List<String> static_ips) {
        this.static_ips = static_ips;
    }
}
