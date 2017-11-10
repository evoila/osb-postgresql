package de.evoila.cf.cpi.bosh.custom;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.bosh.BoshPlatformService;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.vms.Vm;
import io.bosh.client.vms.Vms;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(BoshProperties.class)

public class PostgesBoshPlatformService extends BoshPlatformService {
    private static final int defaultPort = 5678;

    PostgesBoshPlatformService (PlatformRepository repository, CatalogService catalogService, ServicePortAvailabilityVerifier availabilityVerifier, BoshProperties boshProperties, Optional<DashboardClient> dashboardClient) {
        super(repository, catalogService, availabilityVerifier, boshProperties, dashboardClient, new PostgresDeploymentManager());
    }

    @Override
    protected void runCreateErrands (ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException {
    }

    @Override
    protected void runUpdateErrands (ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException {
    }


    @Override
    protected void updateHosts (ServiceInstance instance, Plan plan, Deployment deployment) {
        int port = defaultPort;
        List<Vm> vms= super.getVms(instance);
        instance.getHosts().clear();
        // TODO: Filter differenticate between HA Proxy and Postgres Server
        vms.forEach(vm -> instance.getHosts().add(super.toServerAddress(vm, defaultPort)));
    }
}
