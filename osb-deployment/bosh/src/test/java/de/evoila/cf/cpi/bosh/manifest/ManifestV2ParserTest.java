package de.evoila.cf.cpi.bosh.manifest;

import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.*;
import de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup.JobV2;
import de.evoila.cf.cpi.bosh.deployment.manifest.job.Job;
import de.evoila.cf.cpi.bosh.deployment.manifest.job.Template;
import de.evoila.cf.cpi.bosh.deployment.manifest.network.Network;
import de.evoila.cf.cpi.bosh.deployment.manifest.network.Subnet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { DeploymentManager.class})
public class ManifestV2ParserTest extends ManifestTest {


    public static final String AZ1 = "z1";
    public static final String VMTYPE = "default";
    public static final String NETWORK_NAME = R_NETWORK_NAME;
    public static final String STEMCELL = "default";
    public static final String PERSISTENT_DISK_TYPE = "default";
    public static final String INST_GRP_NAME = "mongodb3";

    private static final String STEMCELL_ALIAS = "default";
    public static final String STEMCELL_OS = "ubuntu-trusty";
    @Autowired
    DeploymentManager deploymentManager;

    Manifest manifest;


    @Before public void before() throws IOException, URISyntaxException {
        manifest = deploymentManager.readTemplate("/manifestV2.yml");
    }

    @Test public void testManifestParameter() throws IOException, URISyntaxException {
        assertEquals(DIRECTOR_UUID, manifest.getDirector_uuid());
        assertEquals(DEPLOYMENT_NAME, manifest.getName());
    }

    @Test public void testUpdateParameter() throws IOException, URISyntaxException {
        Update update = manifest.getUpdate();
        assertEquals(CANARIES, update.getCanaries());
        assertEquals(CANARY_WATCH_TIME, update.getCanary_watch_time());
        assertEquals(UPDATE_WATCH_TIME, update.getUpdate_watch_time());
        assertEquals(MAX_IN_FLIGHT, update.getMax_in_flight());
    }

    @Test public void testInstanceGroup(){
        InstanceGroup job = manifest.getInstance_groups().get(0);
        assertEquals(INST_GRP_NAME, job.getName());
        assertEquals(INSTANCES, job.getInstances());
        assertEquals(AZ1, job.getAzs().get(0));
        assertEquals(VMTYPE, job.getVm_type());
        assertEquals(NETWORK_NAME, job.getNetworks().get(0).getName());
        assertEquals(STEMCELL, job.getStemcell());
        assertEquals(PERSISTENT_DISK_TYPE, job.getPersistent_disk_type());
    }

    @Test public void testJobs(){
        InstanceGroup group = manifest.getInstance_groups().get(0);
        JobV2 t = group.getJobs().get(0);
        assertEquals("mongodb3", t.getName());
        assertEquals("mongodb3", t.getRelease());
        t = group.getJobs().get(1);
        assertEquals("node-exporter", t.getName());
    }

    @Test public void testReleases(){
        assertEquals(RELEASE_NAME, manifest.getReleases().get(0).getName());
        assertEquals(RELEASE_VERSION, manifest.getReleases().get(0).getVersion());
    }

    @Test public void testStemCell(){
        assertEquals(STEMCELL_ALIAS, manifest.getStemcells().get(0).getAlias());
        assertEquals(STEMCELL_OS, manifest.getStemcells().get(0).getOs());
    }

}
