package de.evoila.cf.broker.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.evoila.cf.broker.exception.AsyncRequiredException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceExistsException;
import de.evoila.cf.broker.model.ErrorMessage;
import de.evoila.cf.broker.model.JobProgressResponse;
import de.evoila.cf.broker.model.ServiceDefinition;
import de.evoila.cf.broker.model.ServiceInstanceRequest;
import de.evoila.cf.broker.model.ServiceInstanceResponse;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.impl.DeploymentServiceImpl;

/**
 * 
 * @author Johannes Hiemer.
 * @author Christian Brinker, evoila.
 */
@Controller
@RequestMapping(value = "/v2")
public class ServiceInstanceController extends BaseController {

	private final Logger log = LoggerFactory.getLogger(ServiceInstanceController.class);

	public static final String SERVICE_INSTANCE_BASE_PATH = "/v2/service_instances";

	@Autowired
	private DeploymentServiceImpl deploymentService;

	@Autowired
	private CatalogService catalogService;

	public ServiceInstanceController() {
		log.info("was here");
	}

	@RequestMapping(value = "/service_instances/{instanceId}", method = RequestMethod.PUT)
	public ResponseEntity<ServiceInstanceResponse> createServiceInstance(
			@PathVariable("instanceId") String serviceInstanceId,
			@RequestParam(value = "accepts_incomplete", required = false) Boolean acceptsIncomplete,
			@Valid @RequestBody ServiceInstanceRequest request) throws ServiceDefinitionDoesNotExistException,
					ServiceInstanceExistsException, ServiceBrokerException, AsyncRequiredException {

		if (acceptsIncomplete == null) {
			throw new AsyncRequiredException();
		}

		log.debug("PUT: " + SERVICE_INSTANCE_BASE_PATH + "/{instanceId}"
				+ ", createServiceInstance(), serviceInstanceId = " + serviceInstanceId);

		ServiceDefinition svc = catalogService.getServiceDefinition(request.getServiceDefinitionId());

		if (svc == null) {
			throw new ServiceDefinitionDoesNotExistException(request.getServiceDefinitionId());
		}

		ServiceInstanceResponse response = deploymentService.createServiceInstance(serviceInstanceId,
				request.getServiceDefinitionId(), request.getPlanId(), request.getOrganizationGuid(),
				request.getSpaceGuid(), request.getParameters());

		log.debug("ServiceInstance Created: " + serviceInstanceId);

		if (response.isAsync())
			return new ResponseEntity<ServiceInstanceResponse>(response, HttpStatus.ACCEPTED);
		else
			return new ResponseEntity<ServiceInstanceResponse>(response, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/service_instances/{instanceId}/last_operation", method = RequestMethod.GET)
	public ResponseEntity<JobProgressResponse> lastOperation(@PathVariable("instanceId") String serviceInstanceId)
			throws ServiceDefinitionDoesNotExistException, ServiceInstanceExistsException, ServiceBrokerException,
			ServiceInstanceDoesNotExistException {

		JobProgressResponse serviceInstanceProcessingResponse = deploymentService.getLastOperation(serviceInstanceId);

		return new ResponseEntity<JobProgressResponse>(serviceInstanceProcessingResponse, HttpStatus.OK);
	}

	@RequestMapping(value = "/service_instances/{instanceId}", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteServiceInstance(@PathVariable("instanceId") String instanceId,
			@RequestParam("service_id") String serviceId, @RequestParam("plan_id") String planId)
					throws ServiceBrokerException, AsyncRequiredException, ServiceInstanceDoesNotExistException {

		log.debug("DELETE: " + SERVICE_INSTANCE_BASE_PATH + "/{instanceId}"
				+ ", deleteServiceInstanceBinding(), serviceInstanceId = " + instanceId + ", serviceId = " + serviceId
				+ ", planId = " + planId);

		deploymentService.deleteServiceInstance(instanceId);

		log.debug("ServiceInstance Deleted: " + instanceId);

		return new ResponseEntity<String>("{}", HttpStatus.OK);
	}

	@Override
	@ExceptionHandler({ ServiceDefinitionDoesNotExistException.class, AsyncRequiredException.class })
	@ResponseBody
	public ResponseEntity<ErrorMessage> handleException(Exception ex, HttpServletResponse response) {
		return getErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@ExceptionHandler(ServiceInstanceExistsException.class)
	@ResponseBody
	public ResponseEntity<ErrorMessage> handleException(ServiceInstanceExistsException ex,
			HttpServletResponse response) {
		return getErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(ServiceInstanceDoesNotExistException.class)
	@ResponseBody
	public ResponseEntity<ErrorMessage> handleException(ServiceInstanceDoesNotExistException ex,
			HttpServletResponse response) {
		return getErrorResponse("{}", HttpStatus.GONE);
	}
}
