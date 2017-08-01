package de.evoila.cf.broker.service;

import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;

public interface BackupService {

    ResponseEntity<HashMap> backupNow(String serviceInstanceId, HashMap fileDestination) throws ServiceInstanceDoesNotExistException;

    ResponseEntity<HashMap> restoreNow (String serviceInstanceId, HashMap fileDestination) throws ServiceInstanceDoesNotExistException;

    ResponseEntity<HashMap> getJobs (String serviceInstanceId, int page, int pageSize);

    ResponseEntity<HashMap> getPlans (String serviceInstanceId);

    ResponseEntity<HashMap> deleteJob (String serviceInstanceId, String jobid);

    ResponseEntity<HashMap> postPlan (String serviceInstanceId, HashMap plan) throws ServiceInstanceDoesNotExistException;

    ResponseEntity<HashMap> deletePlan (String serviceInstanceId, String planid);

    ResponseEntity<HashMap> updatePlan (String serviceInstanceId, String planId, HashMap plan) throws ServiceInstanceDoesNotExistException;
}
