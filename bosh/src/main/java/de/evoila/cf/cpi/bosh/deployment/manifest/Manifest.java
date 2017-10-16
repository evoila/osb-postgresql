package de.evoila.cf.cpi.bosh.deployment.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.evoila.cf.cpi.bosh.deployment.manifest.job.Job;
import de.evoila.cf.cpi.bosh.deployment.manifest.network.Network;
import io.bosh.client.releases.Release;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {

        private String director_uuid;
        private String name;
        private List<Release> releases = new ArrayList<>();
        private Update update;
        private List<Stemcell> stemcells = new ArrayList<>();
        private List<InstanceGroup> instance_groups = new ArrayList<>();
        private Map<String, Object> properties = new HashMap<>();

        private List<Job> jobs = new ArrayList<>();

        private List<Network> networks = new ArrayList<>();
        private List<ResourcePool> resource_pools = new ArrayList<>();

        public String getDirector_uuid () {
                return director_uuid;
        }

        public void setDirector_uuid (String director_uuid) {
                this.director_uuid = director_uuid;
        }

        public String getName () {
                return name;
        }

        public void setName (String name) {
                this.name = name;
        }

        public List<Release> getReleases () {
                return releases;
        }

        public void setReleases (List<Release> releases) {
                this.releases = releases;
        }

        public Update getUpdate () {
                return update;
        }

        public void setUpdate (Update update) {
                this.update = update;
        }

        public List<Stemcell> getStemcells () {
                return stemcells;
        }

        public void setStemcells (List<Stemcell> stemcells) {
                this.stemcells = stemcells;
        }

        public List<InstanceGroup> getInstance_groups () {
                return instance_groups;
        }

        public void setInstance_groups (List<InstanceGroup> instance_groups) {
                this.instance_groups = instance_groups;
        }

        public Map<String, Object> getProperties () {
                return properties;
        }

        public void setProperties (Map<String, Object> properties) {
                this.properties = properties;
        }

        public List<Job> getJobs () {
                return jobs;
        }

        public void setJobs (List<Job> jobs) {
                this.jobs = jobs;
        }

        public List<Network> getNetworks () {
                return networks;
        }

        public void setNetworks (List<Network> networks) {
                this.networks = networks;
        }

        public List<ResourcePool> getResource_pools () {
                return resource_pools;
        }

        public void setResource_pools (List<ResourcePool> resource_pools) {
                this.resource_pools = resource_pools;
        }

}
