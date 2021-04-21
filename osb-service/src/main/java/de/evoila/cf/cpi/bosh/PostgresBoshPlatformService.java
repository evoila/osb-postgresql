package de.evoila.cf.cpi.bosh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.custom.postgres.CustomParameters;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
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
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Hiemer, Yannic Remmet, Marco Hennig.
 */
@Service
@ConditionalOnBean(BoshProperties.class)
public class PostgresBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 5432;
    private Logger log = LoggerFactory.getLogger(getClass());

    private CredentialStore credentialStore;

    private ObjectMapper objectMapper;

    PostgresBoshPlatformService(PlatformRepository repository, CatalogService catalogService,
                                ServicePortAvailabilityVerifier availabilityVerifier,
                                BoshProperties boshProperties,
                                CredentialStore credentialStore,
                                Optional<DashboardClient> dashboardClient,
                                Environment environment,
                                DeploymentManager deploymentManager,
                                ObjectMapper objectMapper) {
        super(repository,
                catalogService, availabilityVerifier,
                boshProperties, dashboardClient,
                deploymentManager);
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void updateHosts(ServiceInstance serviceInstance, Plan plan, Deployment deployment) {

        List<Vm> vms = super.getVms(serviceInstance);
        serviceInstance.getHosts().clear();
        CustomParameters planParameters = objectMapper.convertValue(plan.getMetadata().getCustomParameters(), CustomParameters.class);

        if(planParameters.getDns() == null) {
            vms.forEach(vm -> serviceInstance.getHosts().add(super.toServerAddress(vm, defaultPort, plan)));
        }else{
            String dns = serviceInstance.getId().replace("-","") + "." + planParameters.getDns();
            final String backup = ( plan.getMetadata().getBackup() != null )?plan.getMetadata().getBackup().getInstanceGroup():"none ";

            vms.forEach(vm -> {
                serviceInstance.getHosts().add(new ServerAddress(
                        vm.getJobName() + vm.getIndex(),
                        vm.getId() + "." + vm.getJobName() + "." + dns,
                        defaultPort,
                        vm.getJobName().contains(backup)
                        )
                    );
            });
        }
    }

    @Override
    public void postDeleteInstance(ServiceInstance serviceInstance) {
        credentialStore.deleteCredentials(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
    }

    private void executeCommands(Channel channel, List<String> commands, boolean silent){
        try {

            log.info("Sending commands...");
            sendCommands(channel, commands);

            readChannelOutput(channel, silent);
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

    private void readChannelOutput(Channel channel, boolean silent){
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
                    if (!silent) {
                        log.info(line);
                    }
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

}
