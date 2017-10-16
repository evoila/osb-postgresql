package de.evoila.cf.cpi.bosh.deployment.manifest.job;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {
    private String name;
    private String release;

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
}
