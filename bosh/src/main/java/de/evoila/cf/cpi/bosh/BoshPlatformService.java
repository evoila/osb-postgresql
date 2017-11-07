package de.evoila.cf.cpi.bosh;


import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.controller.utils.DashboardUtils;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.PlatformServiceAdapter;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.bosh.connection.BoshConnection;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.tasks.Task;
import io.bosh.client.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import rx.Observable;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;;
import java.util.Optional;



public abstract class BoshPlatformService extends PlatformServiceAdapter {
    public static final String QUEUED = "queued";
    public static final int SLEEP = 3000;
    public static final String ERROR = "error";
    public static final String PROCESSING = "processing";
    public static final String DONE = "done";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final BoshConnection connection;
    private final PlatformRepository platformRepository;
    private final ServicePortAvailabilityVerifier portAvailabilityVerifier;
    private final DeploymentManager deploymentManager;
    private final Optional<DashboardClient> dashboardClient;
    private final BoshProperties boshProperties;
    private final CatalogService catalogService;
    ;


    BoshPlatformService (PlatformRepository repository,
                         CatalogService catalogService,
                         ServicePortAvailabilityVerifier availabilityVerifier,
                         BoshProperties boshProperties,
                         Optional<DashboardClient> dashboardClient,
                         DeploymentManager deploymentManager){

        Assert.notNull(repository, "The platform repository can not be null");
        Assert.notNull(availabilityVerifier, "The ServicePortAvailabilityVerifier can not be null");
        Assert.notNull(boshProperties, "The BoshProperties can not be null");
        Assert.notNull(catalogService, "The CatalogService can not be null");
        Assert.notNull(deploymentManager, "The Deployment Manager can not be null");

        this.catalogService = catalogService;
        this.platformRepository = repository;
        this.portAvailabilityVerifier = availabilityVerifier;
        this.dashboardClient = dashboardClient;
        this.deploymentManager = deploymentManager;
        this.boshProperties = boshProperties;
        connection = new BoshConnection(boshProperties.getUsername(),
                                        boshProperties.getPassword(),
                                        boshProperties.getHost()).authenticate();
    }

    @Override
    @PostConstruct
    public void registerCustomPlatformService () {
            this.platformRepository.addPlatform(Platform.BOSH, this);
    }

    @Override
    public ServiceInstance postProvisioning (ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        boolean available;
        try {
            available = portAvailabilityVerifier.verifyServiceAvailability(serviceInstance.getHosts(), false);
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
    public ServiceInstance createInstance (ServiceInstance in, Plan plan, Map<String, String> customParameters) throws PlatformException {
        try {
            Deployment deployment = deploymentManager.createDeployment(in,plan,customParameters);
            Observable<Task> task = connection.connection().deployments().create(deployment);
            waitForTaskCompletion(task.toBlocking().first());
            Observable<List<ErrandSummary>> errands = connection.connection().errands().list(deployment.getName());
            runCreateErrands(in, plan, deployment, errands);
            updateHosts(in, plan, deployment);
        } catch (URISyntaxException | IOException e) {
            logger.error("Couldn't create Service Instance via Bosh Deployment");
            logger.error(e.getMessage());
            throw new PlatformException("Could not create Service Instance", e);
        }
        return new ServiceInstance(in, in.getDashboardUrl(), in.getId());
    }

    protected void runCreateErrands (ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException { }
    protected void runUpdateErrands (ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException { }
    protected void runDeleteErrands (ServiceInstance instance, Deployment deployment, Observable<List<ErrandSummary>> errands) { }

    protected void waitForTaskCompletion (Task task) throws PlatformException {
        logger.debug("Bosh Deployment started waiting for task to complete {}", task);
        if(task == null) {
            logger.error("Deployment Task is null");
            throw new PlatformException("Could not alter Service Instance. No Bosh task is present");
        }
        switch (task.getState()){
            case PROCESSING:
            case QUEUED:
                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    throw new PlatformException(e);
                }
                Observable<Task> taskObservable = connection.connection().tasks().get(task.getId());
                waitForTaskCompletion(taskObservable.toBlocking().first());
                return;
            case ERROR:
                throw new PlatformException(String.format("Could not create Service Instance. Task finished with error. [%s]  %s", task.getId(), task.getResult()));
            case DONE:
                return;
        }
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
        Observable<Deployment> obs = connection.connection().deployments().get(serviceInstance.getId());
        Deployment deployment = obs.toBlocking().first();
        Observable<List<ErrandSummary>> errands = connection.connection().errands().list(deployment.getName());
        runDeleteErrands(serviceInstance, deployment, errands);
        connection.connection().deployments().delete(deployment);
    }



    @Override
    public ServiceInstance updateInstance (ServiceInstance instance, Plan plan) throws PlatformException {
        Deployment deployment = connection.connection().deployments().get(instance.getId()).toBlocking().first();
        Observable<List<ErrandSummary>> errands = connection.connection().errands().list(deployment.getName());
        runUpdateErrands(instance, plan, deployment, errands);
        try {
            deployment = deploymentManager.updateDeployment(instance, deployment, plan);
            Observable<Task> taskObservable = connection.connection().deployments().update(deployment);
            waitForTaskCompletion(taskObservable.toBlocking().first());
            updateHosts(instance,plan, deployment);
        } catch (IOException e) {
            throw new PlatformException("Could not update Service instance", e);
        }

        return new ServiceInstance(instance.getId(), instance.getServiceDefinitionId(), plan.getId(),
                                   instance.getOrganizationGuid(), instance.getSpaceGuid(), instance.getParameters(),
                                   instance.getDashboardUrl(), instance.getInternalId());
    }

    protected abstract void updateHosts (ServiceInstance instance, Plan plan, Deployment deployment);



}
