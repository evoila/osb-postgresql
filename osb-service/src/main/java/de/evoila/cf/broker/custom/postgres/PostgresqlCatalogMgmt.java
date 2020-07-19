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
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Potrick Weber
 */
@Profile("pcf")
@Component
@Order(25)
public class PostgresqlCatalogMgmt implements TransformCatalog {

    private final Logger log = LoggerFactory.getLogger(PostgresqlCatalogMgmt.class);


    public void transform(Catalog catalog, Environment environment, EndpointConfiguration endpointConfiguration) {
        catalog.getServices().forEach(s -> s.getPlans().forEach(this::convert));
    }

    public void clean(Catalog catalog, Environment environment, EndpointConfiguration endpointConfiguration) {
        catalog.getServices().forEach(s -> s.getPlans().forEach(this::cleanChangeSslCert));
    }

    private void convert(Plan plan){
        changeSslCert(plan);
        deletePgpoolInstances(plan);
    }
    
    private void cleanChangeSslCert(Plan plan) {
        PostgresqlMapUtils.deleteMapProperty(plan.getMetadata().getCustomParameters(), "capath");
    }

    private void changeSslCert(Plan plan) {
        Map<String,Object> properties= plan.getMetadata().getProperties();
        Object caPath = plan.getMetadata().getCustomParameters().get("capath");
        if (caPath instanceof String) {
            if (properties == null){
                properties = new HashMap<String, Object>();
                plan.getMetadata().setProperties(properties);
            }
            Map<String, Object> ssl = (Map<String, Object>) PostgresqlMapUtils.getMapProperty(properties, "postgres", "ssl");
            if (ssl == null){
                ssl = new HashMap<String, Object>();
                PostgresqlMapUtils.setMapProperty(properties, ssl , "postgres", "ssl");
            }
            final String caPathAsString = (String) caPath;
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".cert_pem))", "ca");
            PostgresqlMapUtils.setMapProperty(ssl, "((" + caPathAsString + ".private_key_pem))", "cakey");
            ssl = (Map<String, Object>) PostgresqlMapUtils.getMapProperty(properties, "pgpool", "ssl");
            if (ssl == null){
                ssl = new HashMap<String, Object>();
                PostgresqlMapUtils.setMapProperty(properties, ssl , "pgpool", "ssl");
            }
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

