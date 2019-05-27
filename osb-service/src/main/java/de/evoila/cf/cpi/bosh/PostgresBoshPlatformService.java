package de.evoila.cf.cpi.bosh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.deployment.manifest.InstanceGroup;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.security.credentials.CredentialStore;
import de.evoila.cf.security.credentials.DefaultCredentialConstants;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.vms.Vm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Johannes Hiemer, Yannic Remmet, Marco Hennig.
 */
@Service
@ConditionalOnBean(BoshProperties.class)
public class PostgresBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 5432;

    private CredentialStore credentialStore;

    PostgresBoshPlatformService(PlatformRepository repository, CatalogService catalogService,
                                ServicePortAvailabilityVerifier availabilityVerifier,
                                BoshProperties boshProperties,
                                CredentialStore credentialStore,
                                Optional<DashboardClient> dashboardClient,
                                Environment environment) {
        super(repository,
                catalogService, availabilityVerifier,
                boshProperties, dashboardClient,
                new PostgresDeploymentManager(boshProperties, environment, credentialStore));
        this.credentialStore = credentialStore;
    }

    @Override
    protected void updateHosts(ServiceInstance serviceInstance, Plan plan, Deployment deployment) {
        List<Vm> vms = super.getVms(serviceInstance);
        serviceInstance.getHosts().clear();

        vms.forEach(vm -> serviceInstance.getHosts().add(super.toServerAddress(vm, defaultPort, plan)));
    }

    @Override
    public void postDeleteInstance(ServiceInstance serviceInstance) {
        credentialStore.deleteCredentials(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, CredentialConstants.PGPOOL_SYSTEM_PASSWORD);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
    }

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

    public void createPgPoolUser(ServiceInstance serviceInstance, Plan plan, String username, String password)
            throws IOException, JSchException, InstanceGroupNotFoundException {

        Manifest manifest = super.getDeployedManifest(serviceInstance);

        Optional<InstanceGroup> group = manifest.getInstanceGroups()
                .stream()
                .filter(i -> i.getName().equals(plan.getMetadata().getIngressInstanceGroup()))
                .findAny();
        if (group.isPresent()) {
            for (int i = 0; i < group.get().getInstances(); i++) {
                createPgPoolUser(serviceInstance, group.get(), i, username, password);
            }
        } else {
            throw new InstanceGroupNotFoundException(serviceInstance, manifest, plan.getMetadata().getIngressInstanceGroup());
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
                String.format("sudo /var/vcap/packages/pgpool/bin/pg_md5 --md5auth " +
                                "--config-file /var/vcap/jobs/pgpool/config/pgpool.conf --username=%s %s",
                        username, password)
        );

        executeCommands(channel, commands);

        close(channel, session);
    }


    public void createPgPoolUser(String deploymentName, String instanceName, List<ServerAddress> serverAddresses, String username, String password)
            throws JSchException {

        for (int i = 0; i < serverAddresses.size(); i++) {

            Session session = getSshSession(deploymentName, instanceName, i)
                    .toBlocking()
                    .first();

            session.connect();
            Channel channel = session.openChannel("shell");
            channel.connect();

            List<String> commands = Arrays.asList(
                    String.format("sudo /var/vcap/packages/pgpool/bin/pg_md5 --md5auth " +
                                    "--config-file /var/vcap/jobs/pgpool/config/pgpool.conf --username=%s %s",
                            username, password)
            );

            executeCommands(channel, commands);

            close(channel, session);
        }
    }
}
