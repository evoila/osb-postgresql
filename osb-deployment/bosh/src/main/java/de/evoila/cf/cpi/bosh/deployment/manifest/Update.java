package de.evoila.cf.cpi.bosh.deployment.manifest;

public class Update {
    private int canaries;
    private String canary_watch_time;
    private String update_watch_time;

    private int max_in_flight;
    private boolean serial;

    public int getCanaries () {
        return canaries;
    }

    public void setCanaries (int canaries) {
        this.canaries = canaries;
    }

    public String getCanary_watch_time () {
        return canary_watch_time;
    }

    public void setCanary_watch_time (String canary_watch_time) {
        this.canary_watch_time = canary_watch_time;
    }

    public String getUpdate_watch_time () {
        return update_watch_time;
    }

    public void setUpdate_watch_time (String update_watch_time) {
        this.update_watch_time = update_watch_time;
    }

    public int getMax_in_flight () {
        return max_in_flight;
    }

    public void setMax_in_flight (int max_in_flight) {
        this.max_in_flight = max_in_flight;
    }

    public boolean isSerial () {
        return serial;
    }

    public void setSerial (boolean serial) {
        this.serial = serial;
    }
}
