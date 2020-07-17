package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.bean.EndpointConfiguration;
import de.evoila.cf.broker.interfaces.TransformCatalog;
import de.evoila.cf.broker.model.catalog.Catalog;
import de.evoila.cf.broker.model.catalog.plan.CustomInstanceGroupConfig;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.utils.PostgresqlMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

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
            serviceDefinition.getPlans()
                    .stream().forEach(plan -> {
                if (plan.getMetadata().isActive()) {
                    changeSslCert(plan);
                    deletePgpoolInstances(plan);
                }
            });
        });
    }


    public void clean(Catalog catalog, Environment environment, EndpointConfiguration endpointConfiguration) {
        catalog.getServices()
                .stream().forEach(serviceDefinition -> {
            serviceDefinition.getPlans()
                    .stream().forEach(plan -> {
                if (plan.getMetadata().isActive()) {
                    cleanChangeSslCert(plan);
                }
            });
        });
    }

    private void cleanChangeSslCert(Plan plan) {
        PostgresqlMapUtils.deleteMapProperty(plan.getMetadata().getProperties(), "postgres", "ssl");
    }

    private void changeSslCert(Plan plan) {
        Map<String, Object> ssl = (Map<String, Object>) PostgresqlMapUtils.getMapProperty(plan.getMetadata().getProperties(), "postgres", "ssl");
        Object caPath = plan.getMetadata().getCustomParameters().get("capath");

        if (caPath instanceof String) {
            final String caPathAsString = (String) caPath;
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".cert_pem))", "ca");
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".private_key_pem))", "cakey");
        }
    }

    private void deletePgpoolInstances(Plan plan) {
        if (!plan.getMetadata().getIngressInstanceGroup().equals("pgpool")) {
            Optional<CustomInstanceGroupConfig> instanceGroupConfig = plan.getMetadata().getInstanceGroupConfig().stream().filter(i -> i.getName().equals("pgpool")).findAny();
            if (instanceGroupConfig.isPresent()) {
                instanceGroupConfig.get().setNodes(0);
            }
        }
    }
}

