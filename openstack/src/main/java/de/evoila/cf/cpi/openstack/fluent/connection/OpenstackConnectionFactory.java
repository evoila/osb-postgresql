/**
 * 
 */
package de.evoila.cf.cpi.openstack.fluent.connection;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Johannes Hiemer, evoila.
 *
 */
public class OpenstackConnectionFactory {

	private final Logger log = LoggerFactory.getLogger(OpenstackConnectionFactory.class);

	protected static OSClient osClient;

	private static String username;

	private static String password;

	private static String authUrl;

	private static String tenantId;

	private static String PROVIDER = "openstack-fluent--provider";

	private static OpenstackConnectionFactory instance = null;

	public static OpenstackConnectionFactory getInstance() {
		if (instance == null) {
			instance = new OpenstackConnectionFactory();
		}
		return instance;
	}

	public OpenstackConnectionFactory setCredential(String username, String password) {
		OpenstackConnectionFactory.username = username;
		OpenstackConnectionFactory.password = password;
		return instance;
	}

	public OpenstackConnectionFactory authenticate(String authUrl, String tenantId) {
		OpenstackConnectionFactory.authUrl = authUrl;
		OpenstackConnectionFactory.tenantId = tenantId;
		Assert.notNull(username, "Username may not be empty, when initializing");
		Assert.notNull(password, "Password may not be empty, when initializing");

		log.info("Initializing Provider:" + PROVIDER);

		osClient = OSFactory.builder().endpoint(authUrl).credentials(username, password).tenantId(tenantId)
				.authenticate();
		return instance;
	}

	private static void authenticate() {
		osClient = OSFactory.builder().endpoint(authUrl).credentials(username, password).tenantId(tenantId)
				.authenticate();
	}

	public OpenstackConnectionFactory authenticateV3(String authUrl, String tenantId) {
		Assert.notNull(username, "Username may not be empty, when initializing");
		Assert.notNull(password, "Password may not be empty, when initializing");

		log.info("Initializing Provider:" + PROVIDER);

		osClient = OSFactory.builderV3().endpoint(authUrl).credentials(username, password, Identifier.byId(tenantId))
				.authenticate();
		return instance;
	}

	public static OSClient connection() {
		authenticate();
		Assert.notNull(osClient, "Connection must be initialized before called any methods on it");
		Assert.notNull(osClient.getToken(), "No token defined, connection details are invalid");
		return osClient;
	}

}
