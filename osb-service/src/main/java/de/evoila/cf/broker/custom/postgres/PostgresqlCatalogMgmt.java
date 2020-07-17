package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.EndpointConfiguration;
import de.evoila.cf.broker.interfaces.TransformCatalog;
import de.evoila.cf.broker.model.catalog.Catalog;
import de.evoila.cf.broker.model.catalog.plan.*;
import de.evoila.cf.broker.utils.PostgresqlMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Potrick Weber
 */
@Profile("pcf")
@Component
public class PostgresqlCatalogMgmt implements TransformCatalog {

    private final Logger log = LoggerFactory.getLogger(PostgresqlCatalogMgmt.class);


    public void transform(Catalog catalog, Environment environment, EndpointConfiguration endpointConfiguration) {
        catalog.getServices()
                .stream().forEach(serviceDefinition -> {
            log.debug("service:" + serviceDefinition.getName());
            serviceDefinition.getPlans()
                    .stream().forEach(plan -> {
                        if (plan.getMetadata().isActive()) {
                            log.debug("plan:" + plan.getName());
                            changeSslCert(plan);
                            log.debug("plan ssl change end" + plan.getName());

                            deletePgpoolInstances(plan);
                            log.debug("plan ssl delete end" + plan.getName());
                        }
            });

        });
    }


    public void clean(Catalog catalog, Environment environment, EndpointConfiguration endpointConfiguration) {
    }

    private void changeSslCert(Plan plan) {
        Constraint
        Map<String, Object> ssl = (Map<String, Object>) PostgresqlMapUtils.getMapProperty(plan.getMetadata().getProperties(), "postgres", "ssl");
        Object caPath = plan.getMetadata().getCustomParameters().get("capath");

        if (caPath instanceof String) {
            log.debug("set capath");
            final String caPathAsString = (String) caPath;
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".cert_pem))", "ca");
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".private_key_pem))", "cakey");
        }
    }

    private void deletePgpoolInstances(Plan plan) {
        log.debug("run fpgpool");
        if (!plan.getMetadata().getIngressInstanceGroup().equals("pgpool")) {
            log.debug("no pgpool");
            Optional<CustomInstanceGroupConfig> instanceGroupConfig = plan.getMetadata().getInstanceGroupConfig().stream().filter(i -> i.getName().equals("pgpool")).findAny();
            if (instanceGroupConfig.isPresent()) {
                log.debug("found pgploo");
                instanceGroupConfig.get().setNodes(0);
            }
        }
    }
}

