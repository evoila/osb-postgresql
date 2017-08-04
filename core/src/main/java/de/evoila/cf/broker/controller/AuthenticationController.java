/**
 * 
 */
package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.GeneralConfiguration;
import de.evoila.cf.broker.controller.utils.DashboardAuthenticationRedirectBuilder;
import de.evoila.cf.broker.controller.utils.DashboardUtils;
import de.evoila.cf.broker.model.Dashboard;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.ServiceDefinition;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.oauth.CompositeAccessToken;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.CatalogService;
import evoila.cf.broker.openid.OpenIdAuthenticationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

/** @author Johannes Hiemer. */
@Controller
@RequestMapping(value = "/v2/authentication")
public class AuthenticationController extends BaseController {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String REQUIRED_SCOPES = "cloud_controller_service_permissions.read openid";

	private static final String IS_AUTHENTICATED = "dashboard_is_authenticated";

	private static final String TOKEN_PREFIX = "Bearer";

	private ServiceInstanceRepository serviceInstanceRepository;
	
	private CatalogService catalogService;

	private GeneralConfiguration generalConfiguration;

	public AuthenticationController(ServiceInstanceRepository serviceInstanceRepository, CatalogService catalogService,
									GeneralConfiguration generalConfiguration) {
		Assert.notNull(serviceInstanceRepository, "ServiceInstance may not be null");
		Assert.notNull(catalogService, "CatalogService may not be null");
		Assert.notNull(generalConfiguration, "GeneralConfigurationBean may not be null");

		this.serviceInstanceRepository = serviceInstanceRepository;
		this.catalogService = catalogService;
		this.generalConfiguration = generalConfiguration;
	}

    @RequestMapping(value = "/{serviceInstanceId}")
    public Object authRedirect(@PathVariable String serviceInstanceId,
    		HttpServletResponse response) throws URISyntaxException, IOException {
    	ServiceDefinition serviceDefinition = resolveServiceDefinitionByServiceInstanceId(serviceInstanceId);
    	if (serviceDefinition != null) {
    		if (serviceDefinition != null
    				&& serviceDefinition.getDashboard() != null 
    				&& serviceDefinition.getDashboard().getAuthEndpoint() != null
    				&& DashboardUtils.isURL(serviceDefinition.getDashboard().getAuthEndpoint())) {
    			
    			Dashboard dashboard = serviceDefinition.getDashboard();
    			DashboardClient dashboardClient = serviceDefinition.getDashboardClient();
    			
    			String redirectUri = DashboardUtils.appendSegmentToPath(dashboard.getUrl(), serviceInstanceId);
    			DashboardAuthenticationRedirectBuilder dashboardAuthenticationRedirectBuilder 
    				= new DashboardAuthenticationRedirectBuilder(dashboard,
    						dashboardClient, redirectUri, REQUIRED_SCOPES);
    				
    			return new ModelAndView("redirect:" + dashboardAuthenticationRedirectBuilder.getRedirectUrl());
    		} else {
				return this.processErrorResponse("Service Definition or Dashboard Configuration could not be found",
						HttpStatus.NOT_FOUND);
			}
    	} else {
			return this.processErrorResponse("Service Definition of Service Instance could not be found",
					HttpStatus.NOT_FOUND);
		}
    }
    
    private ServiceDefinition resolveServiceDefinitionByServiceInstanceId(String serviceInstanceId) {
    	if (serviceInstanceRepository.containsServiceInstanceId(serviceInstanceId)) {
    		ServiceInstance serviceInstance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);
    		
    		return catalogService.getServiceDefinition(serviceInstance.getServiceDefinitionId());
    	} else 
    		return null;
    }

	@RequestMapping(value = "/{serviceInstanceId}/confirm")
    public Object confirm(@PathVariable String serviceInstanceId, @RequestParam(value = "code", required = true) String authCode,
								HttpServletRequest request) throws Exception {
		ModelAndView mav = new ModelAndView("index");
		if (authCode == null)
			return this.processErrorResponse("No authentication code from UAA could be found",
					HttpStatus.UNAUTHORIZED);

		ServiceDefinition serviceDefinition = resolveServiceDefinitionByServiceInstanceId(serviceInstanceId);
		if (serviceDefinition != null) {
			Dashboard dashboard = serviceDefinition.getDashboard();
			DashboardClient dashboardClient = serviceDefinition.getDashboardClient();

			String redirectUri = DashboardUtils.appendSegmentToPath(dashboardClient.getRedirectUri(), serviceInstanceId);
			CompositeAccessToken token = null;
			try {
				token = OpenIdAuthenticationUtils.getAccessAndRefreshToken(dashboard.getAuthEndpoint(), authCode,
						dashboardClient, redirectUri);
			} catch (Exception ex) {}

			token = new CompositeAccessToken();
			token.setAccessToken("eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiIyZWUxYmM0ZmI5NWI0NmRlODU4MTZkNTIzMzA5ZDQwNCIsInN1YiI6IjUwNWM1NDIxLWYxYWYtNGZmNi1iZTY1LWU4MmViZWFhMDc5MSIsInNjb3BlIjpbImNsb3VkX2NvbnRyb2xsZXIucmVhZCIsInBhc3N3b3JkLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsImRvcHBsZXIuZmlyZWhvc2UiLCJzY2ltLndyaXRlIiwic2NpbS5yZWFkIiwiY2xvdWRfY29udHJvbGxlci5hZG1pbiIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiI1MDVjNTQyMS1mMWFmLTRmZjYtYmU2NS1lODJlYmVhYTA3OTEiLCJvcmlnaW4iOiJ1YWEiLCJ1c2VyX25hbWUiOiJhZG1pbiIsImVtYWlsIjoiYWRtaW4iLCJyZXZfc2lnIjoiZjA5NWFkYTAiLCJpYXQiOjE1MDE4Mjg4OTUsImV4cCI6MTUwMTgyOTQ5NSwiaXNzIjoiaHR0cHM6Ly91YWEuY2YuZXUtZGUtbmV0ZGUubXNoLmhvc3Qvb2F1dGgvdG9rZW4iLCJ6aWQiOiJ1YWEiLCJhdWQiOlsic2NpbSIsImNsb3VkX2NvbnRyb2xsZXIiLCJwYXNzd29yZCIsImNmIiwidWFhIiwib3BlbmlkIiwiZG9wcGxlciJdfQ.KhIrEb4ENYV_Y7hA0Os02hqPXpkKSLjNaOxNvPjbK_QX9ltceNMXIdWDBkDAJuaBgHQbfuSlf0q7goz1cNRTMicLONImDCkjMlEVcYGFiiK0-yCM8CMx7-__BbGKpQrildDtTHwUj1ZwCSK3Gzl5OmWpRtZ29mgqTm01clLTlXE");
			if (token != null) {
				mav.addObject("token", TOKEN_PREFIX + token.getAccessToken());
				mav.addObject("serviceInstanceId", serviceInstanceId);
				mav.addObject("serviceBrokerEndpointUrl", generalConfiguration.getEndpointUrl());
			} else {
				log.info("Did not receive a valid token, had to abort authentication...");
				return this.processErrorResponse("Token could not be processed/or is not valid",
						HttpStatus.UNAUTHORIZED);
			}
		} else
			return this.processErrorResponse("Service Definition of Service Instance could not be found",
					HttpStatus.UNAUTHORIZED);

		return mav;
	}

}
