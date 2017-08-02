package de.evoila.cf.broker.service.impl;

import de.evoila.cf.broker.bean.impl.BackupConfiguration;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.service.BackupService;
import de.evoila.cf.broker.service.InstanceCredentialService;
import org.codehaus.groovy.util.ListHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

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
        String str = config.getUser()+":"+config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(str.getBytes());
    }

    @Override
    public ResponseEntity<HashMap> backupNow (String serviceInstanceId, HashMap fileDestination) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        HashMap body = new HashMap();
        body.put("source", credentials);
        body.put("destination", fileDestination);

        RequestEntity e  = new RequestEntity(body, headers, HttpMethod.PUT,
                                             URI.create(config.getUri()+"/backup"));
        ResponseEntity response = rest.exchange(e,HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> restoreNow (String serviceInstanceId, HashMap fileDestination) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        HashMap body = new HashMap();
        body.put("destination", credentials);
        body.put("source", fileDestination);

        RequestEntity e  = new RequestEntity(body, headers, HttpMethod.PUT,
                                             URI.create(config.getUri()+"/restore"));
        ResponseEntity response = rest.exchange(e,HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> getJobs (String serviceInstanceId, int page, int pageSize) {
        HashMap queryParams = new HashMap();
        queryParams.put("page", page);
        queryParams.put("page_size", pageSize);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<HashMap> response = rest.exchange(config.getUri() + "/jobs/byInstance/" + serviceInstanceId,
                HttpMethod.GET, entity, new ParameterizedTypeReference<HashMap>() {}, queryParams);

        return response;
    }

    @Override
    public ResponseEntity<HashMap> getPlans (String serviceInstanceId) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<HashMap> response = rest.exchange(config.getUri()+"/plans/byInstance/"+serviceInstanceId,
                                                HttpMethod.GET, entity, new ParameterizedTypeReference<HashMap>() {});
        return response;
    }

    @Override
    public ResponseEntity<HashMap> deleteJob (String serviceInstanceId, String jobid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri()+"/plans",
                                                HttpMethod.POST, entity, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> postPlan (String serviceInstanceId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(credentials, headers);
        ResponseEntity response = rest.exchange(config.getUri()+"/plans",
                                                HttpMethod.POST, entity, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> deletePlan (String serviceInstanceId, String planid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri()+"/plan"+planid,
                                                HttpMethod.DELETE, entity, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> updatePlan (String serviceInstanceId, String planId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(credentials, headers);
        ResponseEntity response = rest.exchange(config.getUri()+"/plan"+planId,
                                                HttpMethod.PATCH, entity, HashMap.class);
        return null;
    }
}
