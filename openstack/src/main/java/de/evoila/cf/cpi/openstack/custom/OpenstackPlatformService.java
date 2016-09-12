/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.VolumeUnit;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.openstack.OpenstackServiceFactory;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * 
 * @author Johannes Hiemer.
 *
 */
@Service
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "backend")
public class OpenstackPlatformService extends OpenstackServiceFactory {

	private static final String VOLUME_SIZE = "volume_size";
	private static final String FLAVOR = "flavor";

	private StackHandler stackHandler;

	@Autowired
	@Qualifier(value = "defaultStackHandler")
	private StackHandler defaultStackHandler;

	@Autowired(required = false)
	private PlatformRepository platformRepository;

	@Autowired
	private ServicePortAvailabilityVerifier portAvailabilityVerifier;

	@Autowired
	private IpAccessor ipAccessor;

	@Autowired(required = false)
	private void setStackHandler(CustomStackHandler customStackHandler) {
		if (customStackHandler != null) {
			stackHandler = customStackHandler;
		} else {
			stackHandler = defaultStackHandler;
		}
	}

	@Override
	@PostConstruct
	public void registerCustomPlatformServie() {
		if (platformRepository != null)
			platformRepository.addPlatform(Platform.OPENSTACK, this);

		if (stackHandler == null) {
			stackHandler = defaultStackHandler;
		}
	}

	@Override
	public boolean isSyncPossibleOnCreate(Plan plan) {
		return false;
	}

	@Override
	public boolean isSyncPossibleOnDelete(ServiceInstance instance) {
		return false;
	}

	@Override
	public boolean isSyncPossibleOnUpdate(ServiceInstance instance, Plan plan) {
		return false;
	}

	@Override
	public ServiceInstance postProvisioning(ServiceInstance serviceInstance, Plan plan) throws PlatformException {

		boolean available;
		try {
			available = portAvailabilityVerifier.verifyServiceAvailability(serviceInstance.getHosts());
		} catch (Exception e) {
			throw new PlatformException("Service instance is not reachable. Service may not be started on instance.",
					e);
		}

		if (!available) {
			throw new PlatformException("Service instance is not reachable. Service may not be started on instance.");
		}

		return serviceInstance;
	}

	@Override
	public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan,
			Map<String, String> customProperties) throws PlatformException {
		String instanceId = serviceInstance.getId();

		Map<String, String> platformParameters = new HashMap<String, String>();
		platformParameters.put(FLAVOR, plan.getFlavorId());
		platformParameters.put(VOLUME_SIZE, volumeSize(plan.getVolumeSize(), plan.getVolumeUnit()));

		platformParameters.putAll(customProperties);

		try {
			String internalId = stackHandler.create(instanceId, platformParameters);

			List<ServerAddress> tmpAddresses = ipAccessor.getIpAddresses(instanceId);
			List<ServerAddress> serverAddresses = Lists.newArrayList();
			for (Entry<String, Integer> port : this.ports.entrySet()) {
				for (ServerAddress tmpAddress : tmpAddresses) {
					ServerAddress serverAddress = new ServerAddress(tmpAddress);
					serverAddress.setName(port.getKey());
					serverAddress.setPort(port.getValue());

					serverAddresses.add(serverAddress);
				}
			}

			serviceInstance = new ServiceInstance(serviceInstance, "http://currently.not/available", internalId,
					serverAddresses);
		} catch (Exception e) {
			throw new PlatformException(e);
		}

		return serviceInstance;
	}

	private String volumeSize(int volumeSize, VolumeUnit volumeUnit) {
		if (volumeUnit.equals(VolumeUnit.M))
			throw new NotAcceptableException("Volumes in openstack may not be smaller than 1 GB");
		else if (volumeUnit.equals(VolumeUnit.G))
			return String.valueOf(volumeSize);
		else if (volumeUnit.equals(VolumeUnit.T))
			return String.valueOf(volumeSize * 1024);
		return String.valueOf(volumeSize);
	}

	@Override
	public ServiceInstance getCreateInstancePromise(ServiceInstance instance, Plan plan) {
		return new ServiceInstance(instance, null, null);
	}

	@Override
	public void preDeprovisionServiceInstance(ServiceInstance serviceInstance) {
	}

	@Override
	public void deleteServiceInstance(ServiceInstance serviceInstance) throws PlatformException {
		stackHandler.delete(serviceInstance.getInternalId());
	}

	@Override
	public ServiceInstance updateInstance(ServiceInstance instance, Plan plan) {
		throw new NotSupportedException("Updating Service Instances is currently not supported");
	}

}
