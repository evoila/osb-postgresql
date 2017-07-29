/**
 * 
 */
package de.evoila.cf.broker.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import evoila.cf.broker.openid.OpenIdAuthenticationUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.evoila.cf.broker.controller.utils.DashboardAuthenticationRedirectBuilder;
import de.evoila.cf.broker.controller.utils.DashboardUtils;
import de.evoila.cf.broker.model.Dashboard;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.ServiceDefinition;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.oauth.CompositeAccessToken;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.CatalogService;

/**
 * @author Johannes Hiemer.
 *
 */
@Controller
@RequestMapping(value = "/v2/dashboard")
public class DashboardController extends BaseController {
	
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
    public Object confirm(@PathVariable String serviceInstanceId, @RequestParam(value = "code", required = true) String authCode,
						  HttpServletRequest request) throws URISyntaxException {
		if (authCode == null)
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

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
				this.createTokenAuthenticationContext(token.getAccessToken(), request);
				log.info("Creating User Session for Dashboard after successful authentication...");
			} else {
				log.info("Did not receive a valid token, had to abort authentication...");
			}
		}
		return "redirect:/v2/dashboard/manage/" + serviceInstanceId;
	}

	@RequestMapping(value = "/manage/{serviceInstanceId}")
    public Object manage(@PathVariable String serviceInstanceId,
    		HttpServletRequest request) throws URISyntaxException {

		return "manage";
    }

	private void createTokenAuthenticationContext(String token, HttpServletRequest request) {
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(token, token);
		Authentication authentication = authenticationManager.authenticate(authRequest);
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(authentication);

		this.setAuthenticatedSession(securityContext, true);
	}
    
    private void setAuthenticatedSession(SecurityContext securityContext, boolean isAuthenticated) {
    	httpSession.setAttribute(IS_AUTHENTICATED, isAuthenticated);
    	httpSession.setMaxInactiveInterval(SESSION_TIMEOUT);
		httpSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
    }

}
