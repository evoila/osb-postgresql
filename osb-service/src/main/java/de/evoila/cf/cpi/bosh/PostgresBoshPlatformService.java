package de.evoila.cf.cpi.bosh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.bosh.deployment.manifest.InstanceGroup;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.vms.Vm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(BoshProperties.class)
public class PostgresBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 5432;
    private static final String INSTANCE_GROUP = "postgres";

    PostgresBoshPlatformService(PlatformRepository repository, CatalogService catalogService, ServicePortAvailabilityVerifier availabilityVerifier,
                                BoshProperties boshProperties, Optional<DashboardClient> dashboardClient) {
        super(repository, catalogService, availabilityVerifier, boshProperties, dashboardClient, new PostgresDeploymentManager(boshProperties));
    }

    @Override
    protected void runCreateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                    Observable<List<ErrandSummary>> errands) throws PlatformException {}

    @Override
    protected void runUpdateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                    Observable<List<ErrandSummary>> errands) throws PlatformException { }

    @Override
    protected void updateHosts(ServiceInstance serviceInstance, Plan plan, Deployment deployment) {
        List<Vm> vms = super.getVms(serviceInstance);
        serviceInstance.getHosts().clear();

        vms.forEach(vm -> serviceInstance.getHosts().add(super.toServerAddress(vm, defaultPort)));
    }

    @Override
    public void postDeleteInstance(ServiceInstance serviceInstance) { }

    public void createPgPoolUser(ServiceInstance instance) throws IOException, JSchException, NoSuchAlgorithmException, InstanceGroupNotFoundException {
        Deployment deployment = super.getDeployment(instance);
        Manifest manifest = super.getManifest(deployment);

        Optional<InstanceGroup> group = manifest.getInstanceGroups()
              .stream()
              .filter(i -> i.getName().equals(INSTANCE_GROUP))
              .findAny();
        if(group.isPresent()){
            int instances = group.get().getInstances();
            for(int i = 0; i< instances; i++){
                createPgPoolUser(instance, group.get(), i);
            }
        } else {
            throw new InstanceGroupNotFoundException(instance, manifest, INSTANCE_GROUP);
        }

    }

    private void createPgPoolUser(ServiceInstance instance,InstanceGroup instanceGroup,int i) throws NoSuchAlgorithmException, JSchException, IOException {
        Channel shell = getSshSession(instance,instanceGroup,i).toBlocking().first().openChannel("shell");
        OutputStreamWriter out = new OutputStreamWriter(shell.getOutputStream());
        out.write("sudo su");
        out.flush();
        out.write(String.format("/var/vcap/packages/pgpool2/bin/pg_md5 --md5auth --config-file /var/vcap/jobs/pgpool/config/pgpool.conf --username=%s %s", instance.getUsername(), instance.getPassword()));
        out.flush();;
        out.close();
    }
}
