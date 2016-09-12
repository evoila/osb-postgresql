/**
 * 
 */
package de.evoila.cf.cpi.openstack.fluent;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.storage.block.Volume;

import de.evoila.cf.cpi.openstack.BaseConnectionFactoryTest;

/**
 * @author Johannes Hiemer.
 *
 */
public class NovaFluentTest extends BaseConnectionFactoryTest {
	
	NovaFluent novaFluent = null;
	CinderFluent cinderFluent = null;
	
	@Before
	public void before() {
		novaFluent = new NovaFluent();
		cinderFluent = new CinderFluent();
	}
	
	@Test
	public void testFlavorList() {
		List<? extends Flavor> flavors = novaFluent.getFlavors();
		
		Assert.assertNotNull(flavors);
		Assert.assertTrue(flavors.size() > 0);
	}

	@Test
	public void testCreateAndDeleteServer() throws InterruptedException {
		Server server = novaFluent.create("test123", 
				"cf-microbosh",
				"af9e47bc-e3fc-4844-8a0f-c09bf063d66d",
				"3", 
				Arrays.asList("acdece87-6afc-43c6-9e73-bed2b3bc0ea3"),
				true);	
		
		Volume volume = cinderFluent.create("test", "-", 20);
		
		Thread.sleep(20000);
		
		novaFluent.attach(server.getId(), volume.getId(), "/dev/vdb");
		
		//Assert.assertNotNull(server);
		
		//novaFluent.deleteInstance(server.getId());
		
		//cinderFluent.delete(volume.getId());
	}

}
