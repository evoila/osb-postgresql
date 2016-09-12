package de.evoila.cf.cpi.docker;

import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;

/**
 * @author Dennis Mueller Johannes Hiemer
 */
@Service
public class DockerVolumeServiceBroker {

	Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private DirectExchange exchange;

	@Autowired
	private EndpointAvailabilityService endpointAvailabilityService;

	private final static String DOCKER_VOLUME_SERVICE_KEY = "dockerVolumeService";

	public final static String DOCKER_TOPIC = "docker";

	public final static String SIP_TOPIC = "sip";

	public final static String VOLUMES_TOPIC = "volumes";

	public final static String JOBS_TOPIC = "jobs";

	public final static String CREATE_TOPIC = "create";

	public final static String DELETE_TOPIC = "delete";

	private String senderId;

	@PostConstruct
	public void initialize() throws SocketException {
		try {
			if (endpointAvailabilityService.isAvailable(DOCKER_VOLUME_SERVICE_KEY)) {
				endpointAvailabilityService.add(DOCKER_VOLUME_SERVICE_KEY,
						new EndpointServiceState(DOCKER_VOLUME_SERVICE_KEY, AvailabilityState.AVAILABLE));
			}
		} catch (Exception ex) {
			endpointAvailabilityService.add(DOCKER_VOLUME_SERVICE_KEY,
					new EndpointServiceState(DOCKER_VOLUME_SERVICE_KEY, AvailabilityState.ERROR, ex.toString()));
		}
	}

	public void createVolume(String nodeName, String mountPoint, int volumeSize) throws TimeoutException {
		log.info("Creating volume {} from node {} and volume size {}", mountPoint, nodeName, (volumeSize));

		String payload = "{\"action\" : \"" + "create" + "\", \"mountPoint\" : \"" + mountPoint
				+ "\", \"volumeSize\" : \"" + volumeSize + "\"}";
		publishPayloadToNode(nodeName, payload);
	}

	private void publishPayloadToNode(String nodeName, String payload) throws TimeoutException {

		rabbitTemplate.setReplyTimeout(120000);
		rabbitTemplate.setCorrelationKey(UUID.randomUUID().toString());
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentEncoding(Charset.defaultCharset().displayName());
		messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		messageProperties.setCorrelationId(new Date().toString().getBytes());
		Message message = new Message(payload.getBytes(), messageProperties);
		Message response = this.rabbitTemplate.sendAndReceive(exchange.getName(), nodeName, message);
		if (response == null) {
			throw new TimeoutException("Job is taking too long!");
		}
		String code = new String(response.getBody());
		if (!Objects.equals(code, "OK")) {
			// TODO Correct throwing excpection
			throw new TimeoutException("An error occured during volume creationn or deletion for container!");
		}
		System.out.print(response);
	}

	public void deleteVolume(String nodeName, String mountPoint) throws TimeoutException {
		log.info("Deleting volume {} from node {}", mountPoint, nodeName);

		String payload = "{\"action\" : \"" + "create" + "\", \"mountPoint\" : \"" + mountPoint + "\"}";

	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

}
