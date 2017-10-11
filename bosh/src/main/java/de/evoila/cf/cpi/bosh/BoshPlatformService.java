package de.evoila.cf.cpi.bosh;


import de.evoila.cf.broker.controller.utils.DashboardUtils;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.bosh.connection.BoshConnection;
import io.bosh.client.deployments.Deployment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import rx.Observable;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@Service
public class BoshPlatformService extends BoshServiceFactory {

    private PlatformRepository platformRepository;
    private ServicePortAvailabilityVerifier portAvailabilityVerifier;
    private Optional<DashboardClient> dashboardClient;
    private DeploymentManager deploymentManager;

    BoshPlatformService(PlatformRepository repository, ServicePortAvailabilityVerifier availabilityVerifier, DeploymentManager deploymentManager){
        Assert.notNull(repository, "The platform repository can not be null");
        Assert.notNull(connection, "The ServicePortAvailabilityVerifier can not be null");
        Assert.notNull(connection, "The DeploymentManager can not be null");
        this.platformRepository = repository;
        this.portAvailabilityVerifier = availabilityVerifier;
        this.dashboardClient = dashboardClient;
        this.deploymentManager = deploymentManager;
    }

    @Override
    @PostConstruct
    public void registerCustomPlatformServie () {
            this.platformRepository.addPlatform(Platform.BOSH, this);
    }



    @Override
    public ServiceInstance postProvisioning (ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        boolean available;
        try {
            available = portAvailabilityVerifier.verifyServiceAvailability(serviceInstance.getHosts(), true);
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
    public void preDeprovisionServiceInstance (ServiceInstance serviceInstance) {

    }

    @Override
    public ServiceInstance createInstance (ServiceInstance instance, Plan plan, Map<String, String> customParameters) throws PlatformException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/yaml");

        try {
            Deployment deployment = deploymentManager.newDeployment(instance, plan);
            connection.connection().deployments().create(deployment, headers);
        } catch (URISyntaxException | IOException e) {
            throw new PlatformException("Could not create Service instance", e);
        }

        return instance;
    }

    @Override
    public ServiceInstance getCreateInstancePromise (ServiceInstance instance, Plan plan) {
        return new ServiceInstance(instance,
                                   DashboardUtils.dashboard(catalogService.getServiceDefinition(instance.getServiceDefinitionId()),
                                                                      instance.getId()),
                                   null);
    }

    @Override
    public void deleteServiceInstance (ServiceInstance serviceInstance) throws PlatformException {
       Deployment deployment = connection.connection().deployments().get(serviceInstance.getId()).toBlocking().first();

       connection.connection().deployments().delete(deployment);
    }

    @Override
    public ServiceInstance updateInstance (ServiceInstance instance, Plan plan) throws PlatformException {
        Deployment deployment = connection.connection().deployments().get(instance.getId()).toBlocking().first();
        try {
            deployment = deploymentManager.updateDeployment(deployment, plan);
        } catch (IOException e) {
            throw new PlatformException("Could not update Service instance", e);
        }

        return new ServiceInstance(instance.getId(), instance.getServiceDefinitionId(), plan.getId(),
                                   instance.getOrganizationGuid(), instance.getSpaceGuid(), instance.getParameters(),
                                   instance.getDashboardUrl(), instance.getInternalId());
    }
}
