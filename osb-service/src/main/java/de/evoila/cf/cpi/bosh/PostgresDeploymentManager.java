package de.evoila.cf.cpi.bosh;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.custom.postgres.CustomParameters;
import de.evoila.cf.broker.custom.postgres.PostgreSQLUtils;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.MapUtils;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.cpi.bosh.deployment.manifest.features.Features;
import de.evoila.cf.cpi.bosh.deployment.manifest.instanceGroup.JobV2;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.security.SecureRandom;

/**
 * @author Marco Hennig, Johannes Hiemer.
 */
@Component
public class PostgresDeploymentManager extends DeploymentManager {

    private CredentialStore credentialStore;
    private ObjectMapper objectMapper;
    

    public PostgresDeploymentManager(BoshProperties properties,
                                     Environment environment,
                                     CredentialStore credentialStore,
                                     ObjectMapper objectMapper) {
        super(properties, environment);
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String,
            Object> customParameters, boolean isUpdate) {
	SecureRandom random = new SecureRandom();
	boolean useSsl = true;
	ArrayList<String> extensions = null;
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty()) {
            properties.putAll(customParameters);
            Object ssl = getMapProperty(properties, "postgres", "ssl", "enabled");
            if (ssl != null) {
                useSsl = ((Boolean) ssl).booleanValue();
                ssl = getMapProperty(properties, "postgres", "ssl");
            }
            extensions = (ArrayList<String>) getMapProperty(properties, "postgres", "database", "extensions");
            if (extensions != null) {
                deleteMapProperty(properties, "postgres", "database", "extensions");
            }


        }

        CustomParameters planParameters = objectMapper.convertValue(plan.getMetadata().getCustomParameters(), CustomParameters.class);

        if (planParameters.getDns() != null) {
            JobV2 postgres = manifest.getInstanceGroup("postgres").orElseThrow().getJobs().stream().findFirst().filter(job -> job.getName().equals("postgres")).orElseThrow();
            JobV2 haproxy = manifest.getInstanceGroup("haproxy").orElseThrow().getJobs().stream().findFirst().filter(job -> job.getName().equals("haproxy")).orElseThrow();

            List<JobV2.Aliases> postgresAliases = postgres.getProvides().get("postgres-address").getAliases();
            List<JobV2.Aliases>  haproxyAliases = haproxy.getProvides().get("haproxy-address").getAliases();
            String urlPrefix = serviceInstance.getId().replace("-", "");
            ArrayList<String> altNames = new ArrayList<>();
            altNames.add("*.haproxy.services.sb-"+serviceInstance.getId()+".bosh"); // required for backup-agent

            String dnsEntry = planParameters.getDns();
            altNames.add(urlPrefix + "." + dnsEntry);
            altNames.add("*.postgres." + urlPrefix + "." + dnsEntry);
            altNames.add("*.haproxy." + urlPrefix + "." + dnsEntry);
            postgresAliases.add(new JobV2.Aliases("_.postgres." + urlPrefix + "." + dnsEntry, JobV2.PlaceholderType.UUID));
            haproxyAliases.add(new JobV2.Aliases("_.haproxy." + urlPrefix + "." + dnsEntry, JobV2.PlaceholderType.UUID));
            if (plan.getMetadata().getIngressInstanceGroup().equals("haproxy")){
                haproxyAliases.add(new JobV2.Aliases( urlPrefix + "." + dnsEntry));
            }else{
                postgresAliases.add(new JobV2.Aliases( urlPrefix + "." + dnsEntry));
            }

            manifest.getVariables().stream().filter(variable -> variable.getName().equals("server_cert")).findFirst().ifPresent(serverCert -> {
                    serverCert.getOptions().setAlternativeNames(altNames);
                    if (planParameters.getCert() != null) {
                        serverCert.getOptions().setCa(planParameters.getCert());
                    }
            });

            if (manifest.getFeatures() == null) manifest.setFeatures(new Features());
            manifest.getFeatures().setUseDnsAddresses(true);
            manifest.getFeatures().setUseShortDnsAddresses(planParameters.isShortDns());
        }

        if (customParameters != null && !customParameters.isEmpty()) {
            for (Map.Entry<String, Object> parameter : customParameters.entrySet()) {
                Map<String, Object> manifestProperties = manifestProperties(parameter.getKey().toString(), manifest);

                if (manifestProperties != null)
                    MapUtils.deepMerge(manifestProperties, customParameters);
            }

        }

//        if (!isUpdate) {
            log.debug("Updating Deployment Manifest, replacing parameters");
            setManifestMetadataFromPlan(manifest, plan);
            Map<String, Object> postgresManifestProperties = manifestProperties("postgres", manifest);

            HashMap<String, Object> postgres = (HashMap<String, Object>) postgresManifestProperties.get("postgres");
            HashMap<String, Object> postgresExporter = (HashMap<String, Object>) postgresManifestProperties.get("postgres_exporter");
            HashMap<String, Object> backupAgent = (HashMap<String, Object>) postgresManifestProperties.get("backup_agent");

            List<HashMap<String, Object>> adminUsers = (List<HashMap<String, Object>>) postgres.get("admin_users");
            HashMap<String, Object> userProperties = adminUsers.get(0);

            postgresExporter.put("datasource_name", "postgresql://((exporter_credentials.username)):((exporter_credentials.password))@127.0.0.1:5432/postgres?sslmode="+(useSsl?"require":"disable"));

            List<HashMap<String, Object>> users = (List<HashMap<String, Object>>) postgres.get("users");
            HashMap<String, Object> defaultUserProperties;


            List<String> databaseUsers = new ArrayList<>();
        databaseUsers.add("((" + CredentialConstants.ROOT_CREDENTIALS + ".username))");


            if (planParameters.getCert() != null) {
                Map<String, Object> sslMap = (Map<String,Object>)getMapProperty(postgres, "ssl");
                sslMap.put("ca", (String)sslMap.get("ca") + "((" + planParameters.getCert() + ".ca))");
            }

            List<Map<String, Object>> databases = (ArrayList<Map<String,Object>>)getMapProperty(postgres,"databases");
            if (databases == null) {
                databases = new ArrayList<>();
            }
            Map<String, Object> database = new HashMap<>();
            database.put("name", PostgreSQLUtils.dbName(serviceInstance.getId()));
            database.put("users", databaseUsers);
            if(extensions!=null && !extensions.isEmpty()){
                database.put("extensions",extensions);
            }
            databases.add(database);

            postgres.put("databases", databases);
  //      }


        this.updateInstanceGroupConfiguration(manifest, plan);
    }

    private Map<String, Object> manifestProperties(String instanceGroup, Manifest manifest) {
        return manifest
            .getInstanceGroups()
            .stream()
            .filter(i -> {
                if (i.getName().equals(instanceGroup))
                    return true;
                return false;
            }).findFirst().get().getProperties();
    }

    private Object getMapProperty(Map<String,Object> map,String ... keys){
        Map<String,Object> nextMap=map;
         Object objectMap=map;
        if(map==null){
            return null;
        }
        for(String key:keys){
            map=(Map< String, Object>)objectMap;
            if(!map.containsKey(key)){
                return null;
            }
            objectMap=map.get(key);
        }
        return objectMap;
    }

    private void deleteMapProperty(Map<String,Object> map,String ... keys){
        Map<String,Object> nextMap=map;
        Object objectMap = map;
        if(map==null){
            return;
        }
        for(String key:keys){
            map=(Map< String, Object>)objectMap;
            if(!map.containsKey(key)){
                return ;
            }
            objectMap=map.get(key);
        }
        map.remove(objectMap);
    }

    private void setMapProperty(Map<String,Object> map,Object value,String ... keys){
        Map<String,Object> nextMap=map;
        int i;
        for(i=0;i<keys.length-1;i++){
            if(!map.containsKey(keys[i])){
                map.put(keys[i],keys[i+1]);
            }else {
                map = (Map<String, Object>) map.get(keys[i]);
            }
        }
        map.put(keys[i],value);
    }
}
