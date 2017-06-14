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
import org.springframework.security.crypto.codec.Base64;
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
	
	private final static String AUTH_CODE = "code";

	private static final String IS_AUTHENTICATED = "dashboard_is_authenticated";
	
	private static final int SESSION_TIMEOUT = 30;
	
	@Autowired
	protected ServiceInstanceRepository serviceInstanceRepository;
	
	@Autowired
	private CatalogService catalogService;
	
	@Autowired
	private HttpSession httpSession;

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
    
    @RequestMapping(value = "/manage/{serviceInstanceId}")
    public Object manage(@PathVariable String serviceInstanceId,
    		@RequestParam(value = "code", required = true) String authCode,
    		HttpServletRequest request) throws URISyntaxException {
    	if (authCode == null)
    		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    	else {
    		ServiceDefinition serviceDefinition = resolveServiceDefinitionByServiceInstanceId(serviceInstanceId);
        	if (serviceDefinition != null) {
        		Dashboard dashboard = serviceDefinition.getDashboard();
        		DashboardClient dashboardClient = serviceDefinition.getDashboardClient();

        		if (!isAuthenticatedSession()) {
        			String redirectUri = DashboardUtils.appendSegmentToPath(dashboardClient.getRedirectUri(), serviceInstanceId);
        			CompositeAccessToken token = null;
        			try {
        				token = this.getAccessAndRefreshToken(dashboard.getAuthEndpoint(), authCode,
        						dashboardClient, redirectUri);
        			} catch (RestClientException ex) {
        				log.info("Token could not be used for Dashboard please reauthenticate...");
        				setAuthenticatedSession(false);
        			}
            		
            		if (token != null) {
            			log.info("Creating User Session for Dashboard after successful authentication...");
            			setAuthenticatedSession(true);
            		} else {
            			log.info("Did not receive a valid token, had to abort authentication...");
            			setAuthenticatedSession(false);
            		}
        		} else if (isAuthenticatedSession()) {
        			setAuthenticatedSession(true);
        		}
        		
        		if (isAuthenticatedSession())
        			return "manage";
        		else
        			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        		
        	} else
        		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}	    	    	
    }
    
    private void setAuthenticatedSession(boolean isAuthenticated) {
    	httpSession.setAttribute(IS_AUTHENTICATED, isAuthenticated);
    	httpSession.setMaxInactiveInterval(SESSION_TIMEOUT);
    }
    
    private boolean isAuthenticatedSession() {
    	boolean authenticated = false;
    	if (httpSession.getAttribute(IS_AUTHENTICATED) != null) { 
    		if (httpSession.getAttribute(IS_AUTHENTICATED).equals(false))
    			authenticated = false;
    		if (httpSession.getAttribute(IS_AUTHENTICATED).equals(true))
    			authenticated = true;
    	}
		return authenticated;
    }
    
    @SuppressWarnings("unused")
	private String getAuthCode(String location) throws URISyntaxException {
    	URIBuilder uriBuilder = new URIBuilder(location);
    	String authCode = null;
    	for (NameValuePair queryParam : uriBuilder.getQueryParams())
    		if (queryParam.getName().equals(AUTH_CODE))
    			authCode = queryParam.getValue();
    	
    	return authCode;
    }
    
    private CompositeAccessToken getAccessAndRefreshToken(String oauthEndpoint, String code, DashboardClient dashboardClient,
    		String redirectUri) throws RestClientException {
    	String clientBasicAuth = getClientBasicAuthHeader(dashboardClient.getId(),  dashboardClient.getSecret());
    	RestTemplate template = new RestTemplate();

    	HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, clientBasicAuth);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("response_type", "token");
        form.add("grant_type", "authorization_code");
        form.add("client_id", dashboardClient.getId());
        form.add("client_secret", dashboardClient.getSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        ResponseEntity<CompositeAccessToken> token = template.exchange(oauthEndpoint + "/token", 
        		HttpMethod.POST, new HttpEntity<>(form, headers), CompositeAccessToken.class);
        
        if (token != null)
        	return token.getBody();
        else
        	return null;
    }
    
    protected String getClientBasicAuthHeader(String clientId, String clientSecret) {
        try {
            byte[] autbytes = Base64.encode(String.format("%s:%s", clientId, clientSecret).getBytes("UTF-8"));
            String base64 = new String(autbytes);
            return String.format("Basic %s", base64);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
