package de.evoila.cf.broker.service.impl;

import de.evoila.cf.broker.bean.impl.BackupConfiguration;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.service.BackupService;
import de.evoila.cf.broker.service.InstanceCredentialService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnBean(BackupConfiguration.class)

public class BackupServiceImpl implements BackupService {
    private final RestTemplate rest;
    private final HttpHeaders headers;
    InstanceCredentialService credentialService;
    BackupConfiguration config;

    public BackupServiceImpl (BackupConfiguration config, InstanceCredentialService credentialService) {
        this.config = config;
        this.credentialService = credentialService;

        this.rest = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        headers.add("Authorization", encodeCredentials());
    }

    private String encodeCredentials () {
        String str = config.getUser() + ":" + config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(str.getBytes());
    }

    @Override
    public ResponseEntity<HashMap> backupNow (String serviceInstanceId, HashMap body) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        body.put("source", credentials);
        Object obj = body.get("destination");
        if(obj instanceof Map){
            ((Map) obj).put("type", "Swift");
        }

        RequestEntity e = new RequestEntity(body, headers,
                                            HttpMethod.POST,
                                            URI.create(config.getUri() + "/backup")
        );
        ResponseEntity response = rest.exchange(e, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> restoreNow (String serviceInstanceId, HashMap fileDestination) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        HashMap body = new HashMap();
        body.put("destination", credentials);
        body.put("source", fileDestination);

        RequestEntity e = new RequestEntity(body, headers, HttpMethod.POST,
                                            URI.create(config.getUri() + "/restore")
        );
        ResponseEntity response = rest.exchange(e, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> getPlans (String serviceInstanceId, Map<String, String> urlParams) {
        HttpEntity entity = new HttpEntity(headers);
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.getUri() + "/plans/byInstance/" + serviceInstanceId);
        urlParams.forEach((k, v) -> builder.queryParam(k, v));
        ResponseEntity<HashMap> response = rest.exchange(builder.build().toUri(),
                                                         HttpMethod.GET,
                                                         entity,
                                                         HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> deleteJob (String serviceInstanceId, String jobid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans",
                                                HttpMethod.POST, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> postPlan (String serviceInstanceId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(plan, headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans",
                                                HttpMethod.POST, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> deletePlan (String serviceInstanceId, String planid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans/" + planid,
                                                HttpMethod.DELETE, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> updatePlan (String serviceInstanceId, String planId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(plan, headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans/" + planId,
                                                HttpMethod.PUT, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> getJobs (String serviceInstanceId, Map<String, String> urlParams) {
        HttpEntity entity = new HttpEntity(headers);
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.getUri() + "/jobs/byInstance/" + serviceInstanceId);
        urlParams.forEach((k, v) -> builder.queryParam(k, v));
        ResponseEntity<HashMap> response = rest.exchange(builder.build().toUri(),
                                                         HttpMethod.GET,
                                                         entity,
                                                         HashMap.class
        );
        return response;
    }
}
