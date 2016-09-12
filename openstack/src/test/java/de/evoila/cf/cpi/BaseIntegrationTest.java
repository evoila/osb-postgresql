/**
 * 
 */
package de.evoila.cf.cpi;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import de.evoila.cf.cpi.configuration.IntegrationTestConfiguration;

/**
 * @author Johannes Hiemer.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
		IntegrationTestConfiguration.class
})
public abstract class BaseIntegrationTest {
	
	protected MockMvc mockMvc;
	
}
