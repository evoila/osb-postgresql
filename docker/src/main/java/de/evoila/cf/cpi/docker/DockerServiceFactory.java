package de.evoila.cf.cpi.docker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.LogConfig.LoggingType;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.SSLConfig;

import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;
import de.evoila.cf.broker.service.PlatformService;

/**
 * 
 * @author Dennis Mueller.
 *
 */
public abstract class DockerServiceFactory implements PlatformService {

	private final static String DOCKER_SERVICE_KEY = "dockerFactoryService";

	private final static String DEFAULT_ENCODING = "UTF-8";

	private static final long MEMORY_LIMIT = 536870912;

	Logger log = LoggerFactory.getLogger(getClass());

	Map<String, Map<String, Object>> containerCredentialMap = new HashMap<String, Map<String, Object>>();

	@Value("${docker.offset}")
	private int offset;

	@Value("${docker.imageName}")
	private String imageName;

	@Value("${docker.ssl.enabled:false}")
	private boolean dockerSSLEnabled;

	@Value("${docker.host}")
	private String dockerHost;

	@Value("${docker.port}")
	private String dockerPort;

	@Value("${docker.portRange.start}")
	private int portRangeStart;

	@Value("${docker.portRange.end}")
	private int portRangeEnd;

	@Value("${docker.syslogAddress}")
	private String syslogAddress;

	private Map<String, Integer> ports;

	@Autowired
	private DockerVolumeServiceBroker dockerVolumeServiceBroker;

	@Autowired
	private EndpointAvailabilityService endpointAvailabilityService;

	private List<Integer> availablePorts = new ArrayList<Integer>();

	private List<Integer> usedPorts = new ArrayList<Integer>();

	private String containerCmd = null;

	private Map<Integer, String> reservePorts;

	@Autowired
	private ApplicationContext appContext;

	@PostConstruct
	public void initialize() throws PlatformException {
		try {
			if (endpointAvailabilityService.isAvailable(DOCKER_SERVICE_KEY)) {
				endpointAvailabilityService.add(DOCKER_SERVICE_KEY,
						new EndpointServiceState(DOCKER_SERVICE_KEY, AvailabilityState.AVAILABLE));
			}
		} catch (Exception ex) {
			endpointAvailabilityService.add(DOCKER_SERVICE_KEY,
					new EndpointServiceState(DOCKER_SERVICE_KEY, AvailabilityState.ERROR, ex.toString()));
		}

		log.debug("Reading command definition for docker");

		String templatePath = "classpath:docker/container.cmd";

		try {
			InputStream inputStream = appContext.getResource(templatePath).getInputStream();

			this.containerCmd = this.readTemplateFile(inputStream);
		} catch (IOException | URISyntaxException e) {
			log.info("Failed to load docker template", e);
		}

		reservePorts = new HashMap<>();
		for (String key : ports.keySet()) {
			reservePorts.put(ports.get(key), key);
		}
	}

	private String readTemplateFile(InputStream inputStream) throws IOException, URISyntaxException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
			result.write(buffer, 0, length);
		}
		return result.toString(DEFAULT_ENCODING);
	}

	private void updateAvailablePorts() throws PlatformException {
		this.listUsedPort();
		this.intersect();
	}

	private void listUsedPort() throws PlatformException {
		usedPorts = new ArrayList<Integer>();

		DockerClient dockerClient = this.createDockerClientInstance();
		List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

		for (Container container : containers) {
			InspectContainerResponse i = dockerClient.inspectContainerCmd(container.getId()).exec();
			Ports portBindings = i.getHostConfig().getPortBindings();

			if (portBindings == null)
				continue;

			Set<Entry<ExposedPort, Binding[]>> bindings = portBindings.getBindings().entrySet();
			for (Entry<ExposedPort, Binding[]> binding : bindings) {
				Binding[] bs = binding.getValue();
				if (bs == null)
					continue;
				for (Binding b : bs) {
					usedPorts.add(b.getHostPort());
				}
			}
		}

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at listing used ports!");
		}
	}

	private void intersect() throws PlatformException {
		availablePorts.removeAll(usedPorts);
	}

	private Integer resolveNextAvailablePort() throws PlatformException {
		initAvailablePorts();
		updateAvailablePorts();
		return availablePorts.get(0);
	}

	private void initAvailablePorts() {
		availablePorts = new ArrayList<Integer>();
		for (int i = this.portRangeStart; i <= this.portRangeEnd; i++) {
			availablePorts.add(i);
		}
	}

	private DockerClient createDockerClientInstance() throws PlatformException {
		DockerClient dockerClient = null;
		String certsPath = this.getClass().getResource("/docker/").getPath();

		SSLConfig sslConfig = new LocalDirectorySSLConfig(certsPath);

		DockerClientConfigBuilder dockerClientConfigBuilder = new DockerClientConfigBuilder();
		String protocol = "http";

		if (dockerSSLEnabled) {
			dockerClientConfigBuilder = dockerClientConfigBuilder.withSSLConfig(sslConfig);
			protocol = "https";
		}

		DockerClientConfig dockerClientConfig = dockerClientConfigBuilder
				.withUri(protocol + "://" + dockerHost + ":" + dockerPort).build();
		dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();

		return dockerClient;
	}

	private CreateContainerResponse createDockerContainer(Map<String, String> customProperties)
			throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();

		LogConfig logConfig = new LogConfig();
		logConfig.setType(LoggingType.SYSLOG);
		Map<String, String> config = new HashMap<String, String>();
		config.put("syslog-address", syslogAddress);
		logConfig.setConfig(config);

		Volume volume = new Volume("/data");

		int i = 0;
		PortBinding[] portBindings = new PortBinding[ports.size()];
		ExposedPort[] exposedPorts = new ExposedPort[ports.size()];
		for (String key : ports.keySet()) {
			this.resolveNextAvailablePort();

			exposedPorts[i] = new ExposedPort(ports.get(key));
			portBindings[i] = new PortBinding(new Binding(this.availablePorts.get(i)), exposedPorts[i]);

			i++;
		}

		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName)
				.withCmd("-c", parseContainerCmdWithCustomProperties(customProperties)).withEntrypoint("sh")
				.withExposedPorts(exposedPorts).withMemoryLimit(MEMORY_LIMIT).withPortBindings(portBindings)
				.withTty(true).withRestartPolicy(RestartPolicy.alwaysRestart()).withVolumes(volume);
		CreateContainerResponse container = containerCmd.exec();

		log.info("Docker container '" + container.getId());
		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at creating docker container!");
		}
		return container;
	}

	private String parseContainerCmdWithCustomProperties(Map<String, String> customProperties) {
		String[] dollarSplits = containerCmd.split("\\$");
		String parsedContainerCmd = dollarSplits[0];

		for (int i = 1; i < dollarSplits.length; i++) {
			String dollarSplit = dollarSplits[i];

			int whiteSpaceIndex = dollarSplit.indexOf(' ');
			String envVar = dollarSplit.substring(0, whiteSpaceIndex == -1 ? dollarSplit.length() : whiteSpaceIndex);

			parsedContainerCmd += dollarSplit.replace(envVar, customProperties.get(envVar));
		}
		return parsedContainerCmd;
	}

	private String getContainerVolumeHostPath(InspectContainerResponse containerDetails) throws PlatformException {
		return containerDetails.getMounts().get(0).getSource();
	}

	private InspectContainerResponse getContainerDetails(String containerId) throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();

		InspectContainerCmd inspectContainerCmd = dockerClient.inspectContainerCmd(containerId);
		InspectContainerResponse inspectContainerResponse = inspectContainerCmd.exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at getting container's node name!");
		}
		return inspectContainerResponse;
	}

	protected Map<String, Integer> getContainerBindings(String containerId) throws PlatformException {
		InspectContainerResponse containerDetails = this.getContainerDetails(containerId);
		Map<String, Integer> bindingsMap = new HashMap<>();
		Map<ExposedPort, Binding[]> containerBindings = containerDetails.getHostConfig().getPortBindings()
				.getBindings();
		for (ExposedPort exposedPort : containerBindings.keySet()) {
			for (Binding binding : containerBindings.get(exposedPort)) {
				bindingsMap.put(reservePorts.get(exposedPort.getPort()), binding.getHostPort());
			}
		}

		return bindingsMap;
	}

	public CreateContainerResponse createDockerContainer(String instanceId, int volumeSize,
			Map<String, String> customProperties) throws PlatformException {
		log.info("Creating container for {} with volume size {}", instanceId, volumeSize);

		CreateContainerResponse container;
		try {
			container = this.createDockerContainer(customProperties);
		} catch (Exception e) {
			throw new PlatformException(e);
		}

		Map<String, Integer> bindingsMap = this.getContainerBindings(container.getId());
		InspectContainerResponse containerDetails = this.getContainerDetails(container.getId());

		String nodeName = containerDetails.getNode().getName();
		String mountPoint = this.getContainerVolumeHostPath(containerDetails);

		try {
			this.dockerVolumeServiceBroker.createVolume(nodeName, mountPoint, offset + volumeSize);
		} catch (Exception e) {
			throw new PlatformException(e);
		}

		startContainer(container);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("host", containerDetails.getNode().getIp());
		credentials.put("port", bindingsMap.get("default"));
		credentials.put("name", container.getId());

		containerCredentialMap.put(container.getId(), credentials);

		return container;
	}

	private void startContainer(CreateContainerResponse container) throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();
		dockerClient.startContainerCmd(container.getId()).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at starting docker container!");
		}
	}

	public void removeDockerContainer(String containerId) throws PlatformException {
		this.killContainer(containerId);

		InspectContainerResponse containerResponse = this.getContainerDetails(containerId);
		String nodeName = containerResponse.getNode().getName();

		this.removeContainer(containerId);

		String mountPoint = this.getContainerVolumeHostPath(containerResponse);
		try {
			dockerVolumeServiceBroker.deleteVolume(nodeName, mountPoint);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void killContainer(String containerId) throws PlatformException {
		log.info("Killing container {}", containerId);

		DockerClient dockerClient = createDockerClientInstance();

		InspectContainerResponse container = this.getContainerDetails(containerId);
		if (container.getState().isRunning())
			dockerClient.killContainerCmd(containerId).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at killing docker container!");
		}
	}

	private void removeContainer(String containerId) throws PlatformException {
		log.info("Removing container {}", containerId);

		DockerClient dockerClient = createDockerClientInstance();
		dockerClient.removeContainerCmd(containerId).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			log.warn("Cannot close docker client at killing docker container!");
		}
	}

	public Map<String, Integer> getPorts() {
		return ports;
	}

	public void setPorts(Map<String, Integer> ports) {
		this.ports = ports;
	}

}
