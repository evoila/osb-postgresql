/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;

/**
 * @author Christian Mueller, evoila
 *
 */
@Service
@ConditionalOnProperty(prefix = "openstack", name = { "log_host", "log_port"}, havingValue = "")
public class PostgreSqlCustomStackHandler extends CustomStackHandler {
	
	private static final String PRE_IP_TEMPLATE = "/openstack/templatePreIPs.yaml";
	private static final String PRE_VOLUME_TEMPLATE = "/openstack/templatePreIPs.yaml";
	private static final String MAIN_TEMPLATE = "/openstack/templatePreIPs.yaml";
	
	private static final String NAME_TEMPLATE = "postgresql-%s-%s";
	
	private static final String PORTS_KEY = "port_ids";
	private static final String IP_ADRESS_KEY = "port_ips";
	private static final String VOLUME_KEY = "volume_ids";
	
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
			List<String> secondaryStacks = stackMapping.getSecondaryStacks();
			for (String stackId : secondaryStacks) {
				super.delete(stackId);
			}

			super.delete(stackMapping.getPrimaryStack());
			super.delete(stackMapping.getPortsStack());

			stackMappingRepo.delete(stackMapping);
		}
	}

	@Override
	public String create(String instanceId, Map<String, String> customParameters)
			throws PlatformException, InterruptedException {
		
		if (customParameters.containsKey(ParameterManager.CLUSTER)) {
			PostgreSqlStackMapping clusterStacks = createCluster(instanceId, customParameters);
			stackMappingRepo.save(clusterStacks);
			return clusterStacks.getId();
		} 
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
			stackMapping.addServerAddress(new ServerAddress("node-" + i, ip));
		}
		
		ParameterManager.updatePortParameters(customParameters, ips, ports);
		Stack preVolumeStack = preVolumeStack(instanceId, customParameters);
		stackMapping.setVolumeStack(preVolumeStack.getId());
		stackMappingRepo.save(stackMapping);
		
		responses = extractResponses(preVolumeStack, VOLUME_KEY);
		List<String> volumes = responses[0];
		
		Stack mainStack = mainStack(instanceId, customParameters);
		stackMapping.setPrimaryStack(mainStack.getId());
		
		log.debug("Stack deployment for RabbitMQ ready - Stacks:" + stackMapping.getSecondaryStacks().size() + 2);

		return stackMapping;
	}

	private Stack mainStack(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parameters = ParameterManager.copyProperties(customParameters, 
				ParameterManager.RESOURCE_NAME,
				ParameterManager.NODE_NUMBER,
				ParameterManager.NODE_NUMBER,
				ParameterManager.FLAVOUR,
				ParameterManager.SERVICE_DB,
				ParameterManager.ADMIN_USER,
				ParameterManager.ADMIN_PASSWORD,
				ParameterManager.PRIMARY_VOLUME_ID,
				ParameterManager.PRIMARY_IP,
				ParameterManager.PRIMARY_PORT,
				ParameterManager.STANDBY_VOLUME_ID,
				ParameterManager.STANDBY_IP,
				ParameterManager.STANDBY_PORT
				);
		
		parameters.putAll(this.defaultParameters());
		
		String name = String.format(NAME_TEMPLATE, instanceId, "cl");
		String templatePorts = accessTemplate(MAIN_TEMPLATE);
		heatFluent.create(name, templatePorts, parameters, false, 10l);
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
		Map<String, String> parametersPreIP = ParameterManager.copyProperties(customParameters, 
				ParameterManager.RESOURCE_NAME,
				ParameterManager.NODE_NUMBER,
				ParameterManager.VOLUME_SIZE
				);
		
		String name = String.format(NAME_TEMPLATE, instanceId, "vol");
		String templatePorts = accessTemplate(PRE_VOLUME_TEMPLATE);
		heatFluent.create(name, templatePorts, parametersPreIP, false, 10l);
		Stack preVolumeStack = stackProgressObserver.waitForStackCompletion(name);
		return preVolumeStack;
	}
	
	

	private Stack createPreIp(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parametersPreIP = ParameterManager.copyProperties(customParameters, 
				ParameterManager.RESOURCE_NAME,
				ParameterManager.NODE_NUMBER
				);

		String name = String.format(NAME_TEMPLATE, instanceId, "ip");
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
