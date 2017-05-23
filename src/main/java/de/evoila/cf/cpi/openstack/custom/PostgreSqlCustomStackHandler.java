/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jcajce.provider.digest.GOST3411.HashMac;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.util.PrimitiveArrayBuilder;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.cpi.openstack.OpenstackServiceFactory;

/**
 * @author Yannic Remmet, evoila
 *
 */
@Service
@ConditionalOnProperty(prefix = "openstack", name = { "keypair" }, havingValue = "")
public class PostgreSqlCustomStackHandler extends CustomStackHandler {
	
	private static final String PRE_IP_TEMPLATE = "/openstack/pre-ips.yaml";
	private static final String PRE_VOLUME_TEMPLATE = "/openstack/pre-volume.yaml";
	
	private static final String MAIN_TEMPLATE = "/openstack/main.yaml";
	
	private static final String PRIMARY_TEMPLATE = "/openstack/primary.yaml";
	private static final String SECONDARY_TEMPLATE = "/openstack/secondaries.yaml";
	
	private static final String NAME_TEMPLATE = "postgresql-%s-%s";
	
	private static final String PORTS_KEY = "port_ids";
	private static final String IP_ADRESS_KEY = "port_ips";
	private static final String VOLUME_KEY = "volume_ids";
	
	@Value("${openstack.keypair}")
	private String keyPair;
	
	private final Logger log = LoggerFactory.getLogger(PostgreSqlCustomStackHandler.class);
	
	@Value("${openstack.log_port}")
	private String logPort;

	@Value("${openstack.log_host}")
	private String logHost;

	@Autowired
	private StackMappingRepository stackMappingRepo;

	private ParameterManager paramManager;
	
	public PostgreSqlCustomStackHandler() {
		super();
		paramManager = new ParameterManager(logHost, logPort);
	}
	@Override
	public void delete(String internalId) {
		PostgreSqlStackMapping stackMapping;
		stackMapping = stackMappingRepo.findOne(internalId);

		if (stackMapping == null) {
			super.delete(internalId);
		} else {
			
			try {
				super.deleteAndWait(stackMapping.getPrimaryStack());
				Thread.sleep(20000);
			} catch (PlatformException | InterruptedException e) {
				log.error("Could not delete Stack " + stackMapping.getPrimaryStack() + " Instance " + internalId);
				log.error(e.getMessage());
			}
			super.delete(stackMapping.getPortsStack());
			super.delete(stackMapping.getVolumeStack());

			stackMappingRepo.delete(stackMapping);
		}
	}

	@Override
	public String create(String instanceId, Map<String, String> customParameters)
			throws PlatformException, InterruptedException {
		log.debug(customParameters.toString());
		if (customParameters.containsKey(ParameterManager.CLUSTER)) {
			log.debug("Start creating cluster " + instanceId);
			PostgreSqlStackMapping clusterStacks = createCluster(instanceId, customParameters);
			log.debug("End creating cluster " + instanceId);
			stackMappingRepo.save(clusterStacks);
			return clusterStacks.getId();
		} 
		log.debug("Not Creating a cluster" + instanceId);
		return super.create(instanceId, customParameters);
	}

	/**
	 * @param instanceId
	 * @param customParameters
	 * @param plan
	 * @return
	 * @throws PlatformException
	 * @throws InterruptedException
	 */
	private PostgreSqlStackMapping createCluster(String instanceId, Map<String, String> customParameters)
			throws PlatformException, InterruptedException {

		log.debug("Start create a Postgres cluster");
		customParameters.putAll(this.defaultParameters());
		customParameters.putAll(this.generateValues(instanceId));
		
		PostgreSqlStackMapping stackMapping = new PostgreSqlStackMapping();
		stackMapping.setId(instanceId);
		
		
	
		Stack ipStack = createPreIp(instanceId, customParameters);
		stackMapping.setPortsStack(ipStack.getId());
		stackMappingRepo.save(stackMapping);

		
		List<String>[] responses = extractResponses(ipStack, PORTS_KEY, IP_ADRESS_KEY);
		List<String> ips = responses[1];
		List<String> ports = responses[0];
		
		for (int i = 0; i < ips.size(); i++) {
			String ip = ips.get(i);
			stackMapping.addServerAddress(new ServerAddress("node-" + i, ip, 5432));
		}
		
		ParameterManager.updatePortParameters(customParameters, ips, ports);
		Stack preVolumeStack = preVolumeStack(instanceId, customParameters);
		stackMapping.setVolumeStack(preVolumeStack.getId());
		stackMappingRepo.save(stackMapping);

		responses = extractResponses(preVolumeStack, VOLUME_KEY);
		List<String> volumes = responses[0];
		ParameterManager.updateVolumeParameters(customParameters, volumes);
		
		Stack mainStack = mainStack(instanceId, customParameters);
		
		
		stackMapping.setPrimaryStack(mainStack.getId());
		stackMappingRepo.save(stackMapping);

		return stackMapping;
	}

	private Map<? extends String, ? extends String> generateValues(String instanceId) {
		HashMap<String, String> valueMap = new HashMap();
		valueMap.put(ParameterManager.ADMIN_PASSWORD, instanceId);
		valueMap.put(ParameterManager.ADMIN_USER, instanceId);
		valueMap.put(ParameterManager.SERVICE_DB, instanceId);
		valueMap.put(ParameterManager.KEY_NAME, keyPair);
		valueMap.put(ParameterManager.STANDBY_HOSTNAME, instanceId);
		return valueMap;
	}
	
	private Stack mainStack(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parameters = ParameterManager.copyProperties(
				customParameters,
				ParameterManager.RESOURCE_NAME,
				ParameterManager.IMAGE_ID,
				ParameterManager.KEY_NAME,
				ParameterManager.FLAVOUR,
				ParameterManager.AVAILABILITY_ZONE,

				ParameterManager.PRIMARY_VOLUME_ID,
				ParameterManager.PRIMARY_IP,
				ParameterManager.PRIMARY_PORT,
				
				ParameterManager.STANDBY_VOLUME_ID,
				ParameterManager.STANDBY_IP,
				ParameterManager.STANDBY_PORT,

				ParameterManager.SERVICE_DB,
				ParameterManager.ADMIN_USER,
				ParameterManager.ADMIN_PASSWORD
				
				//ParameterManager.RESOURCE_NAME,
				//ParameterManager.NODE_NUMBER


				);
	
		String name = String.format(NAME_TEMPLATE, instanceId, "cl");
		parameters.put(ParameterManager.RESOURCE_NAME, name);
		
		String template = accessTemplate(MAIN_TEMPLATE);
		String primary = accessTemplate(PRIMARY_TEMPLATE);
		String secondaries = accessTemplate(SECONDARY_TEMPLATE);
		
		Map<String, String> files = new HashMap();
		files.put("primary.yaml", primary);
		files.put("secondaries.yaml", secondaries);
		
		heatFluent.create(name, template, parameters, false, 10l, files);
		Stack stack = stackProgressObserver.waitForStackCompletion(name);
		return stack;
	}
	
	
	private List<String>[] extractResponses(Stack stack, String... keys) {
		List<String>[] response = new List[keys.length];
		
		for (Map<String, Object> output : stack.getOutputs()) {
			Object outputKey = output.get("output_key");
			if (outputKey != null && outputKey instanceof String) {
				String key = (String) outputKey;
				for(int i=0; i< keys.length; i++) {
					if (key.equals(keys[i])) {
						response[i] = (List<String>) output.get("output_value");
					}
				}	
			}
		}
		return response;
	}
	
	private Stack preVolumeStack(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parametersPreIP = ParameterManager.copyProperties(
				customParameters, 
				ParameterManager.RESOURCE_NAME,
				ParameterManager.NODE_NUMBER,
				ParameterManager.VOLUME_SIZE
				);
		
		String name = String.format(NAME_TEMPLATE, instanceId, "vol");
		parametersPreIP.put(ParameterManager.RESOURCE_NAME, name);

		String templatePorts = accessTemplate(PRE_VOLUME_TEMPLATE);
		heatFluent.create(name, templatePorts, parametersPreIP, false, 10l);
		Stack preVolumeStack = stackProgressObserver.waitForStackCompletion(name);
		return preVolumeStack;
	}
	
	

	private Stack createPreIp(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parametersPreIP = ParameterManager.copyProperties(customParameters, 
				ParameterManager.RESOURCE_NAME,
				ParameterManager.NODE_NUMBER,
				ParameterManager.NETWORK_ID,
				ParameterManager.SECURITY_GROUPS
				);
		String name = String.format(NAME_TEMPLATE, instanceId, "ip");
		parametersPreIP.put(ParameterManager.RESOURCE_NAME, name);
		//parametersPreIP.put(ParameterManager.SECURITY_GROUPS, "cf-private");
		
		String templatePorts = accessTemplate(PRE_IP_TEMPLATE);
		
		heatFluent.create(name, templatePorts, parametersPreIP, false, 10l);

		Stack preIpStack = stackProgressObserver.waitForStackCompletion(name);
		return preIpStack;
	}

	public String getLogPort() {
		return logPort;
	}

	public void setLogPort(String logPort) {
		this.logPort = logPort;
	}

	public String getLogHost() {
		return logHost;
	}

	public void setLogHost(String logHost) {
		this.logHost = logHost;
	}

	@Override
	protected Map<String, String> defaultParameters() {
		Map<String, String> defaultParameters = super.defaultParameters();
		defaultParameters.put(LOG_HOST, logHost);
		defaultParameters.put(LOG_PORT, logPort);
		return defaultParameters;
	}
}
