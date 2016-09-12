/**
 * 
 */
package de.evoila.cf.broker.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.config.security.AcceptSelfSignedClientHttpRequestFactory;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
public class HAProxyService {

	private static final String APPLICATION_JSON = "application/json";

	private static final String CONTENT_TYPE = "Content-type";

	private static final String X_AUTH_TOKEN_HEADER = "X-Auth-Token";

	@Value("${haproxy.uri}")
	private String haProxy;

	@Value("${haproxy.auth.token}")
	private String authToken;

	private RestTemplate restTemplate = new RestTemplate();

	private HttpHeaders headers = new HttpHeaders();

	@PostConstruct
	private void initHeaders() {
		headers.add(X_AUTH_TOKEN_HEADER, authToken);
		headers.add(CONTENT_TYPE, APPLICATION_JSON);
	}

	@ConditionalOnBean(AcceptSelfSignedClientHttpRequestFactory.class)
	@Autowired(required = false)
	private void selfSignedRestTemplate(AcceptSelfSignedClientHttpRequestFactory requestFactory) {
		restTemplate.setRequestFactory(requestFactory);
	}

	public List<ServerAddress> appendAgent(List<ServerAddress> internalAddresses) throws ServiceBrokerException {
		List<ServerAddress> externalAddresses = internalAddresses.stream().map(in -> appendSingleAgent(in))
				.filter(in -> in != null).collect(Collectors.toList());

		if (externalAddresses.size() < internalAddresses.size())
			throw new ServiceBrokerException("Could not provide external IPs for all parts of the service instance.");

		return externalAddresses;
	}

	private ServerAddress appendSingleAgent(ServerAddress internalAddress) {
		HttpEntity<ServerAddress> entity = new HttpEntity<>(internalAddress, headers);

		try {
			ServerAddress response = restTemplate.exchange(haProxy, HttpMethod.PUT, entity, ServerAddress.class)
					.getBody();

			if (response != null) {
				response.setName(internalAddress.getName());
				return response;
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
}
