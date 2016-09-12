package de.evoila.cf.broker.controller;

import java.util.Map;

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

import de.evoila.cf.broker.exception.ServerviceInstanceBindingDoesNotExistsException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceBindingExistsException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.ErrorMessage;
import de.evoila.cf.broker.model.ServiceInstanceBindingRequest;
import de.evoila.cf.broker.model.ServiceInstanceBindingResponse;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;

/**
 * 
 * 
 * @author Johannes Hiemer.
 */
@Controller
@RequestMapping(value = "/v2/service_instances")
public class ServiceInstanceBindingController extends BaseController {

	private final Logger log = LoggerFactory.getLogger(ServiceInstanceBindingController.class);

	public static final String SERVICE_INSTANCE_BINDING_BASE_PATH = "/v2/service_instances/{instanceId}/service_bindings";

	@Autowired
	private BindingServiceImpl bindingService;

	@RequestMapping(value = "/{instanceId}/service_bindings/{bindingId}", method = RequestMethod.PUT)
	public ResponseEntity<ServiceInstanceBindingResponse> bindServiceInstance(
			@PathVariable("instanceId") String instanceId, @PathVariable("bindingId") String bindingId,
			@Valid @RequestBody ServiceInstanceBindingRequest request)
					throws ServiceInstanceDoesNotExistException, ServiceInstanceBindingExistsException,
					ServiceBrokerException, ServiceDefinitionDoesNotExistException {

		log.debug("PUT: " + SERVICE_INSTANCE_BINDING_BASE_PATH + "/{bindingId}"
				+ ", bindServiceInstance(), instanceId = " + instanceId + ", bindingId = " + bindingId);

		Map<String, String> bindResource = request.getBindResource();
		String route = (bindResource != null) ? bindResource.get("route") : null;
		ServiceInstanceBindingResponse response = bindingService.createServiceInstanceBinding(bindingId, instanceId,
				request.getServiceDefinitionId(), request.getPlanId(), (request.getAppGuid() == null), route);

		log.debug("ServiceInstanceBinding Created: " + bindingId);

		return new ResponseEntity<ServiceInstanceBindingResponse>(response, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{instanceId}/service_bindings/{bindingId}", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteServiceInstanceBinding(@PathVariable("instanceId") String instanceId,
			@PathVariable("bindingId") String bindingId, @RequestParam("service_id") String serviceId,
			@RequestParam("plan_id") String planId) throws ServiceBrokerException {

		log.debug("DELETE: " + SERVICE_INSTANCE_BINDING_BASE_PATH + "/{bindingId}"
				+ ", deleteServiceInstanceBinding(),  serviceInstance.id = " + instanceId + ", bindingId = " + bindingId
				+ ", serviceId = " + serviceId + ", planId = " + planId);

		try {
			bindingService.deleteServiceInstanceBinding(bindingId);
		} catch (ServerviceInstanceBindingDoesNotExistsException e) {
			return new ResponseEntity<String>("{}", HttpStatus.GONE);
		}

		log.debug("ServiceInstanceBinding Deleted: " + bindingId);

		return new ResponseEntity<String>("{}", HttpStatus.OK);
	}

	@ExceptionHandler(ServiceInstanceDoesNotExistException.class)
	@ResponseBody
	public ResponseEntity<ErrorMessage> handleException(ServiceInstanceDoesNotExistException ex,
			HttpServletResponse response) {
		return getErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@ExceptionHandler(ServiceInstanceBindingExistsException.class)
	@ResponseBody
	public ResponseEntity<ErrorMessage> handleException(ServiceInstanceBindingExistsException ex,
			HttpServletResponse response) {
		return getErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
	}

}
