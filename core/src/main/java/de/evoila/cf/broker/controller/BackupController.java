package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @author Yannic Remmet.
 */
@RestController
@RequestMapping(value = "/v2/backup")
class BackupController extends BaseController {

    @Autowired
    BackupService backupService;

    @RequestMapping(value = "/{serviceInstanceId}/backup", method = RequestMethod.POST)
    public ResponseEntity<HashMap> backupNow (@PathVariable String serviceInstanceId, @RequestBody HashMap fileDestination) throws ServiceInstanceDoesNotExistException {
        ResponseEntity<HashMap> response = backupService.backupNow(serviceInstanceId, fileDestination);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/restore", method = RequestMethod.POST)
    public ResponseEntity<HashMap> restoreNow (@PathVariable String serviceInstanceId, @RequestBody HashMap fileDestination) throws ServiceInstanceDoesNotExistException {
        ResponseEntity<HashMap> response = backupService.restoreNow(serviceInstanceId, fileDestination);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/jobs", method = RequestMethod.GET)
    public ResponseEntity<HashMap> getJobs (@PathVariable String serviceInstanceId,
                                            @RequestParam(value = "page_size", defaultValue = "25") int pageSize,
                                            @RequestParam(value = "page", defaultValue = "0") int page) {
        ResponseEntity<HashMap> response = backupService.getJobs(serviceInstanceId, page, pageSize);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/jobs/{jobid}", method = RequestMethod.DELETE)
    public ResponseEntity<HashMap> getJobs (@PathVariable String serviceInstanceId,
                                            @PathVariable String jobid) {
        ResponseEntity<HashMap> response = backupService.deleteJob(serviceInstanceId, jobid);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/plans", method = RequestMethod.GET)
    public ResponseEntity<HashMap> getJobs (@PathVariable String serviceInstanceId) {
        ResponseEntity<HashMap> response = backupService.getPlans(serviceInstanceId);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/plans", method = RequestMethod.POST)
    public ResponseEntity<HashMap> postPlan (@PathVariable String serviceInstanceId, @RequestBody HashMap plan) throws ServiceInstanceDoesNotExistException {
        ResponseEntity<HashMap> response = backupService.postPlan(serviceInstanceId, plan);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/plans/{planid}", method = RequestMethod.PATCH)
    public ResponseEntity<HashMap> patchPlan (@PathVariable String serviceInstanceId,
                                             @PathVariable String planId,
                                             @RequestBody HashMap plan) throws ServiceInstanceDoesNotExistException {
        ResponseEntity<HashMap> response = backupService.updatePlan(serviceInstanceId, planId, plan);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }

    @RequestMapping(value = "/{serviceInstanceId}/plans/{planid}", method = RequestMethod.DELETE)
    public ResponseEntity<HashMap> deleteJob (@PathVariable String serviceInstanceId,
                                              @PathVariable String planid) {
        ResponseEntity<HashMap> response = backupService.deletePlan(serviceInstanceId, planid);
        return new ResponseEntity<HashMap>(response.getBody(), response.getStatusCode());
    }
}