package de.evoila.cf.broker.custom.postgres;

import java.util.Map;

public class CustomParameters {
    private Map<String, String> dns;
    private String cert;

    public Map<String, String> getDns() {
        return dns;
    }

    public void setDns(Map<String, String> dns) {
        this.dns = dns;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }
}
