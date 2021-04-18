package de.evoila.cf.broker.custom.postgres;

import java.util.Map;

public class CustomParameters {
    private String dns;
    private String cert;

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }
}
