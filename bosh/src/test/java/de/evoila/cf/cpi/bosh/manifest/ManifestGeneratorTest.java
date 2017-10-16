package de.evoila.cf.cpi.bosh.manifest;

import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.cpi.bosh.deployment.manifest.ResourcePool;
import de.evoila.cf.cpi.bosh.deployment.manifest.Stemcell;
import de.evoila.cf.cpi.bosh.deployment.manifest.Update;
import de.evoila.cf.cpi.bosh.deployment.manifest.job.Job;
import de.evoila.cf.cpi.bosh.deployment.manifest.job.Template;

import de.evoila.cf.cpi.bosh.deployment.manifest.network.Network;
import de.evoila.cf.cpi.bosh.deployment.manifest.network.Subnet;
import io.bosh.client.releases.Release;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { DeploymentManager.class})
public class ManifestGeneratorTest extends ManifestTest {

    @Autowired
    DeploymentManager deploymentManager;

    Manifest manifest;


    @Before public void before() throws IOException, URISyntaxException {
        manifest = new Manifest();
        manifest.setDirector_uuid(DIRECTOR_UUID);
        manifest.setName(DEPLOYMENT_NAME);
        manifest.getProperties().put("TEST", "TEST");
        Release release = new Release();
        release.setName(RELEASE_NAME);
        manifest.getReleases().add(release);

        Update u = new Update();
        u.setCanaries(CANARIES);
        u.setCanary_watch_time(CANARY_WATCH_TIME);
        u.setMax_in_flight(MAX_IN_FLIGHT);
        u.setUpdate_watch_time(UPDATE_WATCH_TIME);
        manifest.setUpdate(u);

        Stemcell stemcell = new Stemcell();
        stemcell.setVersion(STEMCELL_VERSION);
        stemcell.setName(STEMCELL_NAME);

        ResourcePool r = new ResourcePool();
        r.setName(R_POOL_NAME);
        r.setName(R_NETWORK_NAME);
        r.setStemcell(stemcell);
        r.getCloud_properties().put("TEST", "TEST");
        r.setNetwork(R_NETWORK_NAME);
        r.setName(R_POOL_NAME);

        Subnet subnet = new Subnet();
        subnet.getDns().add(DNS);
        subnet.setRange(RANGE);
        subnet.getStatic_ips().add(STATIC_IP);
        subnet.getReserved().add(RESERVED);

        Network network = new Network();
        network.setName(NETWORK_NAME);
        network.getSubnets().add(subnet);

        Template t = new Template();
        t.setName(TEMPLATE1);
        t.setRelease(TEMPLATE2);
        Network jobNetwork = new Network();
        jobNetwork.setName(NETWORK_NAME);

        Job job = new Job();
        job.setInstances(INSTANCES);
        job.setName(JOB_NAME);
        job.setPersistent_disk(JOB_PERSISTENT_DISK);
        job.setResource_pool(R_POOL_NAME);
        job.getTemplates().add(t);
        job.getNetworks().add(jobNetwork);
        job.getProperties().put("TEST","TEST");

        manifest.getNetworks().add(network);
        manifest.getResource_pools().add(r);
        manifest.getJobs().add(job);
    }

    @Test public void testManifestGeneration() throws IOException, URISyntaxException {
        String manifest = deploymentManager.generateManifest(this.manifest);
        String cmp_manifest = readFile("cmp_manifest.yml");
        assertEquals(cmp_manifest, manifest);


    }


}
