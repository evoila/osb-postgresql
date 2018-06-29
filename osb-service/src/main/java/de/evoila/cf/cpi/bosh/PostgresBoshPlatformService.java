package de.evoila.cf.cpi.bosh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.bosh.deployment.manifest.InstanceGroup;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.vms.Vm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(BoshProperties.class)
public class PostgresBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 5432;

    PostgresBoshPlatformService(PlatformRepository repository, CatalogService catalogService,
                                ServicePortAvailabilityVerifier availabilityVerifier,
                                BoshProperties boshProperties, Optional<DashboardClient> dashboardClient,
                                Environment environment) {
        super(repository,
                catalogService, availabilityVerifier,
                boshProperties, dashboardClient,
                new PostgresDeploymentManager(boshProperties, environment));
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
    public ServiceInstance postCreateInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        boolean available;
        try {
            available = portAvailabilityVerifier.verifyServiceAvailability(serviceInstance, false);
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
    public void postDeleteInstance(ServiceInstance serviceInstance) { }

    private void executeCommands(Channel channel, List<String> commands){
        try {

            log.info("Sending commands...");
            sendCommands(channel, commands);

            readChannelOutput(channel);
            log.info("Finished sending commands!");

        } catch(Exception e) {
            log.info("An error ocurred during executeCommands: "+e);
        }
    }

    private void sendCommands(Channel channel, List<String> commands){
        try {
            PrintStream out = new PrintStream(channel.getOutputStream());

            out.println("#!/bin/bash");
            for(String command : commands) {
                out.println(command);
            }
            out.println("exit");

            out.flush();
        } catch(Exception e) {
            log.info("Error while sending commands: "+ e);
        }

    }

    private void readChannelOutput(Channel channel){
        byte[] buffer = new byte[1024];

        try {
            InputStream in = channel.getInputStream();
            String line = "";
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    log.info(line);
                }

                if(line.contains("logout")) {
                    break;
                }

                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee){}
            }
        } catch(Exception e) {
            log.info("Error while reading channel output: "+ e);
        }

    }

    public void close(Channel channel, Session session){
        channel.disconnect();
        session.disconnect();
        log.info("Disconnected channel and session");
    }

    public void createPgPoolUser(ServiceInstance instance, Plan plan, String username, String password)
            throws IOException, JSchException, InstanceGroupNotFoundException {

        Deployment deployment = super.getDeployment(instance);
        Manifest manifest = super.getDeployedManifest(deployment.getName());

        Optional<InstanceGroup> group = manifest.getInstanceGroups()
                .stream()
                .filter(i -> i.getName().equals(plan.getMetadata().getIngressInstanceGroup()))
                .findAny();
        if (group.isPresent()) {
            int instance_cnt = group.get().getInstances();
            for (int i = 0; i < instance_cnt; i++) {
                createPgPoolUser(instance, group.get(), i, username, password);
            }
        } else {
            throw new InstanceGroupNotFoundException(instance, manifest, plan.getMetadata().getIngressInstanceGroup());
        }
    }

    private void createPgPoolUser(ServiceInstance instance, InstanceGroup instanceGroup, int i, String username,
                                  String password) throws JSchException {
        Session session = getSshSession(instance, instanceGroup, i)
                .toBlocking()
                .first();

        session.connect();
        Channel channel = session.openChannel("shell");
        channel.connect();

        List<String> commands = Arrays.asList(
                String.format("sudo /var/vcap/packages/pgpool2/bin/pg_md5 --md5auth " +
                                "--config-file /var/vcap/jobs/pgpool/config/pgpool.conf --username=%s %s",
                        username, password)
        );

        executeCommands(channel, commands);

        close(channel, session);
    }


    public void createPgPoolUser(String deploymentName, String instanceName, List<ServerAddress> serverAddresses, String username, String password)
            throws JSchException {

        int instance_cnt = serverAddresses.size();

        for (int i = 0; i < instance_cnt; i++) {

            Session session = getSshSession(deploymentName, instanceName, i)
                    .toBlocking()
                    .first();

            session.connect();
            Channel channel = session.openChannel("shell");
            channel.connect();

            List<String> commands = Arrays.asList(
                    String.format("sudo /var/vcap/packages/pgpool2/bin/pg_md5 --md5auth " +
                                    "--config-file /var/vcap/jobs/pgpool/config/pgpool.conf --username=%s %s",
                            username, password)
            );

            executeCommands(channel, commands);

            close(channel, session);
        }
    }
}
