/**
 * 
 */
package de.evoila.cf.cpi.openstack;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.openstack4j.api.OSClient;

import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;

/**
 * @author Johannes Hiemer.
 *
 */
public class BaseConnectionFactoryTest {
	
	private static String username = "jhiemer";
	
	private static String password = "Vwg7pCRaWuiAeq7bsk74";
	
	private static String authUrlV2 = "http://85.10.239.86:5000/v2.0";
	
	private static String tenant = "Cloudfoundry";
	
	protected static OSClient connection;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		OpenstackConnectionFactory.getInstance()
		.setCredential(username, password)
		.authenticate(authUrlV2, tenant);
		
		assertNotNull(OpenstackConnectionFactory.connection());
		assertNotNull(OpenstackConnectionFactory.connection().getToken());
	}

}
