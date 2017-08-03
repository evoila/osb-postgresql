/**
 * 
 */
package de.evoila.cf.broker.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Johannes Hiemer.
 *
 */
@Controller
@RequestMapping(value = "/v2/authentication")
public class AuthenticationController extends BaseController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String REQUIRED_SCOPES = "cloud_controller_service_permissions.read openid";

	private static final String IS_AUTHENTICATED = "dashboard_is_authenticated";
	
	private static final int SESSION_TIMEOUT = 30;
	
	@Autowired
	protected ServiceInstanceRepository serviceInstanceRepository;
	
	@Autowired
	private CatalogService catalogService;
	
	@Autowired
	private HttpSession httpSession;

	@Autowired
	private AuthenticationManager authenticationManager;

    @RequestMapping(value = "/{serviceInstanceId}")
    public void authRedirect(@PathVariable String serviceInstanceId,
    		HttpServletResponse response) throws URISyntaxException, IOException {
    	ServiceDefinition serviceDefinition = resolveServiceDefinitionByServiceInstanceId(serviceInstanceId);
    	if (serviceDefinition != null) {
    		if (serviceDefinition != null
    				&& serviceDefinition.getDashboard() != null 
    				&& serviceDefinition.getDashboard().getAuthEndpoint() != null
    				&& DashboardUtils.isURL(serviceDefinition.getDashboard().getAuthEndpoint())) {
    			
    			Dashboard dashboard = serviceDefinition.getDashboard();
    			DashboardClient dashboardClient = serviceDefinition.getDashboardClient();
    			
    			String redirectUri = DashboardUtils.appendSegmentToPath(dashboardClient.getRedirectUri(), serviceInstanceId); 
    			DashboardAuthenticationRedirectBuilder dashboardAuthenticationRedirectBuilder 
    				= new DashboardAuthenticationRedirectBuilder(dashboard,
    						dashboardClient, redirectUri, REQUIRED_SCOPES);
    				
    			response.sendRedirect(dashboardAuthenticationRedirectBuilder.getRedirectUrl());
    		}
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
    public ModelAndView confirm(@PathVariable String serviceInstanceId, @RequestParam(value = "code", required = true) String authCode,
								HttpServletRequest request) throws URISyntaxException {
		ModelAndView mav = new ModelAndView("index");
		if (authCode == null)
			mav.setViewName("unauthorized");

		ServiceDefinition serviceDefinition = resolveServiceDefinitionByServiceInstanceId(serviceInstanceId);
		if (serviceDefinition != null) {
			Dashboard dashboard = serviceDefinition.getDashboard();
			DashboardClient dashboardClient = serviceDefinition.getDashboardClient();

			String redirectUri = DashboardUtils.appendSegmentToPath(dashboardClient.getRedirectUri(), serviceInstanceId);
			CompositeAccessToken token = null;
			try {
				token = OpenIdAuthenticationUtils.getAccessAndRefreshToken(dashboard.getAuthEndpoint(), authCode,
							dashboardClient, redirectUri);
			} catch (RestClientException ex) {
				log.info("Token could not be used for Dashboard please reauthenticate...");
			}


			if (token != null) {
				mav.addObject("token", token);
				mav.addObject("serviceInstanceId", serviceInstanceId);
			} else {
				mav.setViewName("unauthorized");
				log.info("Did not receive a valid token, had to abort authentication...");
			}
		}
		return mav;
	}

}
