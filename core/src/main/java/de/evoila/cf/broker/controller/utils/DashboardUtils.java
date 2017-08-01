/**
 * 
 */
package de.evoila.cf.broker.controller.utils;

import java.net.URL;

import de.evoila.cf.broker.model.ServiceDefinition;

/**
 * @author Johannes Hiemer.
 *
 */
public class DashboardUtils {
	
	public static boolean hasDashboard(ServiceDefinition serviceDefinition) {
		if (serviceDefinition.getDashboard() != null 
				&& serviceDefinition.getDashboard().getUrl() != null
				&& DashboardUtils.isURL(serviceDefinition.getDashboard().getUrl())
				&& serviceDefinition.getDashboardClient() != null) 
			return true;
		
		return false;
	}
	
	public static String dashboard(ServiceDefinition serviceDefinition, String serviceInstanceId) {
		return DashboardUtils.appendSegmentToPath(serviceDefinition.getDashboard().getUrl(), serviceInstanceId);
	}
	
	public static boolean isURL(String url) {
	    try {
	        new URL(url);
	        return true;
	    } catch (Exception e) {
	        return false;
	    }
	}
	
	public static String appendSegmentToPath(String path, String segment) {
		if (path == null || path.isEmpty()) 
			return "/" + segment;

		if (path.charAt(path.length() - 1) == '/') 
			return path + segment;

		return path + "/" + segment;
	}

}
