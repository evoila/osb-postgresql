/**
 * 
 */
package de.evoila.cf.cpi.openstack.fluent;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.storage.block.Volume;

import de.evoila.cf.cpi.openstack.BaseConnectionFactoryTest;

/**
 * @author Johannes Hiemer.
 *
 */
public class CinderFluentTest extends BaseConnectionFactoryTest {
	
	CinderFluent cinderFluent = null;
	
	@Before
	public void before() {
		cinderFluent = new CinderFluent();
	}
	
	@Test
	public void testList() {
		List<? extends Volume> volumes = cinderFluent.list();
		
		Assert.assertNotNull(volumes);
		Assert.assertTrue(volumes.size() > 0);
	}
	
	@Test
	public void createGetDelete() throws InterruptedException {
		Volume volume = cinderFluent.create("sample", "small", 20);
		
		Volume loadVolume = cinderFluent.get(volume.getId());
		
		Assert.assertTrue(volume.getId().equals(loadVolume.getId()));
		
		Thread.sleep(10000);
		
		cinderFluent.delete(volume.getId());
	}

}
