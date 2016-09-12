/**
 * 
 */
package de.evoila.cf.cpi.openstack;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;
import de.evoila.cf.broker.service.PlatformService;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;

/**
 * @author Johannes Hiemer.
 *
 */
public abstract class OpenstackServiceFactory implements PlatformService {

	private final static String OPENSTACK_SERVICE_KEY = "openstackFactoryService";

	private Logger log = LoggerFactory.getLogger(getClass());

	@Value("${openstack.endpoint}")
	private String endpoint;

	@Value("${openstack.username}")
	private String username;

	@Value("${openstack.password}")
	private String password;

	@Value("${openstack.tenantId}")
	private String tenantId;

	protected Map<String, Integer> ports;

	public Map<String, Integer> getPorts() {
		return ports;
	}

	public void setPorts(Map<String, Integer> ports) {
		this.ports = ports;
	}

	@Autowired
	private EndpointAvailabilityService endpointAvailabilityService;

	@PostConstruct
	public void initialize() {
		log.debug("Initializing Openstack Connection Factory");
		try {
			if (endpointAvailabilityService.isAvailable(OPENSTACK_SERVICE_KEY)) {
				OpenstackConnectionFactory.getInstance().setCredential(username, password).authenticate(endpoint,
						tenantId);

				log.debug("Reading heat template definition for openstack");

				endpointAvailabilityService.add(OPENSTACK_SERVICE_KEY,
						new EndpointServiceState(OPENSTACK_SERVICE_KEY, AvailabilityState.AVAILABLE));
			}
		} catch (Exception ex) {
			endpointAvailabilityService.add(OPENSTACK_SERVICE_KEY,
					new EndpointServiceState(OPENSTACK_SERVICE_KEY, AvailabilityState.ERROR, ex.toString()));
		}
	}

}
