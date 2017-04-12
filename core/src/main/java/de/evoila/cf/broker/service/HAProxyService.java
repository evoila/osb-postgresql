/**
 * 
 */
package de.evoila.cf.broker.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.catalina.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.evoila.cf.broker.bean.HAProxyBean;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.HABackendResponse;
import de.evoila.cf.broker.model.HAProxyServerAddress;
import de.evoila.cf.broker.model.Mode;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.config.security.AcceptSelfSignedClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Brinker, evoila.
 *
 */

public abstract class HAProxyService {

	private static final String APPLICATION_JSON = "application/json";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String X_AUTH_TOKEN_HEADER = "X-Auth-Token";
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private HAProxyBean haproxyBean;
	
	private String haProxy;
	
	private String authToken;

	private RestTemplate restTemplate = new RestTemplate();

	private HttpHeaders headers = new HttpHeaders();

	@PostConstruct
	private void initHeaders() {
		haProxy = haproxyBean.getUri();
		authToken = haproxyBean.getAuthToken();
		headers.add(X_AUTH_TOKEN_HEADER, authToken);
		headers.add(CONTENT_TYPE, APPLICATION_JSON);
		
	}

	@ConditionalOnBean(AcceptSelfSignedClientHttpRequestFactory.class)
	@Autowired(required = false)
	private void selfSignedRestTemplate(AcceptSelfSignedClientHttpRequestFactory requestFactory) {
		restTemplate.setRequestFactory(requestFactory);
	}

	public List<ServerAddress> appendAgent(List<ServerAddress> internalAddresses) throws ServiceBrokerException {
		List<ServerAddress> externalAddresses = internalAddresses.stream().map(in -> new HAProxyServerAddress(in, getMode(in), getOptions(in))).map(in -> appendSingleAgent(in))
				.filter(in -> in != null).collect(Collectors.toList());

		if (externalAddresses.size() < internalAddresses.size())
			throw new ServiceBrokerException("Could not provide external IPs for all parts of the service instance.");

		return externalAddresses;
	}

	private ServerAddress appendSingleAgent(HAProxyServerAddress internalAddress) {
		log.info("Headers are Content-Type:" + headers.getContentType() + " and Token: " +  headers.getFirst(X_AUTH_TOKEN_HEADER));
		log.info("URI is: " + haProxy);
		log.info("Name of Internal Adress is: " + internalAddress.getName());
		
		HttpEntity<HAProxyServerAddress> entity = new HttpEntity<>(internalAddress, headers);
		log.info("Body Values are Internal IP is:" + entity.getBody().getIp() + " Port: " +  entity.getBody().getPort() + " Mode: " + entity.getBody().getMode() + " Options: " + entity.getBody().getOptions());
		log.info("Body is: " + entity.getBody());
		try {
			HABackendResponse response = restTemplate.exchange(haProxy, HttpMethod.PUT, entity, HABackendResponse.class)
					.getBody();
			
			
			log.info("Called: " + haProxy);
			log.info("Response is: " + response);
			
			if (response != null) {
				ServerAddress serverAddress = new ServerAddress(internalAddress.getName(), response.getIp(), response.getPort());
				return serverAddress;
			}
		} catch (RestClientException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void removeAgent(List<ServerAddress> internalAddresses) throws ServiceBrokerException {
		for (ServerAddress internalAddress : internalAddresses) {
			removeSingleAgent(internalAddress);
		}
	}

	private void removeSingleAgent(ServerAddress internalAddress) throws ServiceBrokerException {
		HttpEntity<ServerAddress> entity = new HttpEntity<>(internalAddress, headers);
		try {
			HttpStatus statusCode = restTemplate.exchange(haProxy, HttpMethod.DELETE, entity, Object.class)
					.getStatusCode();
			if (!statusCode.equals(HttpStatus.NO_CONTENT))
				throw new ServiceBrokerException("Could not remove external IP " + internalAddress.getName() + " - "
						+ internalAddress.getIp() + ":" + internalAddress.getPort());
		} catch (RestClientException e) {
			e.printStackTrace();
			throw new ServiceBrokerException("Could not remove external IP " + internalAddress.getName() + " - "
					+ internalAddress.getIp() + ":" + internalAddress.getPort());
		}
	}
	
	public abstract Mode getMode(ServerAddress serverAddress);
	
	public abstract List<String> getOptions(ServerAddress serverAddress);
}
