/**
 * 
 */
package de.evoila.cf.cpi.openstack.fluent;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.image.Image;

import de.evoila.cf.cpi.openstack.BaseConnectionFactoryTest;

/**
 * @author Johannes Hiemer.
 *
 */
public class GlanceFluentTest extends BaseConnectionFactoryTest {
	
	GlanceFluent glanceFluent = null;
	
	@Before
	public void before() {
		glanceFluent = new GlanceFluent();
	}
	
	@Test
	public void testList() {
		List<? extends Image> images = glanceFluent.list();
		
		Assert.assertNotNull(images);
		Assert.assertTrue(images.size() > 0);
	}

}
