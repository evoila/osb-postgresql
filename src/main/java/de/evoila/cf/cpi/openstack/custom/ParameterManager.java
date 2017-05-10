/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterManager {
	private PostgreSqlCustomStackHandler stackHandler;

	public static final String RESOURCE_NAME = "resource_name";
	public static final String NODE_NUMBER = "node_number";
	
	public static final String VOLUME_SIZE = "volume_size";
	
	public static final String FLAVOUR = "flavor";
	public static final String SERVICE_DB = "service_db";
	public static final String ADMIN_USER = "admin_user";
	public static final String ADMIN_PASSWORD = "admin_password";
	
	public static final String PRIMARY_HOSTNAME = "primary_hostname";
	public static final String PRIMARY_VOLUME_ID  = "primary_volume_id";
	public static final String PRIMARY_IP = "primary_ip";
	public static final String PRIMARY_PORT = "primary_port";
	public static final String STANDBY_HOSTNAME = "standby_hostname";
	public static final String STANDBY_VOLUME_ID = "standby_volume_id";
	public static final String STANDBY_IP = "standby_ip";
	public static final String STANDBY_PORT = "standby_port";	

	
	public static final String CLUSTER = "cluster"; // ??

	
	
	public final String logHost;
	public final String logPort;
	/**
	 * @param logPort 
	 * @param logHost 
	 * @param rabbitMQCustomStackHandler
	 */
	public ParameterManager(String logHost, String logPort) {
		this.logHost = logHost;
		this.logPort = logPort;
	}

	public void configureGeneralParameters(Map<String, String> customParameters) {
		customParameters.putAll(stackHandler.defaultParameters());

		customParameters.put(PostgreSqlCustomStackHandler.LOG_PORT, logPort);
		customParameters.put(PostgreSqlCustomStackHandler.LOG_HOST, logHost);
	}

	static void updatePortParameters(Map<String, String> customParameters, List<String> ips, List<String> ports) {
		String primIp = ips.get(0);
		ips.remove(0);
		String primPort = ports.get(0);
		ports.remove(0);
		
		customParameters.put(ParameterManager.PRIMARY_PORT, primPort);
		customParameters.put(ParameterManager.PRIMARY_IP, primIp);

		customParameters.put(ParameterManager.STANDBY_PORT, primPort);
		customParameters.put(ParameterManager.STANDBY_IP, primIp);
	}
	
	static void updateVolumeParameters(Map<String, String> customParameters, List<String> volumes) {
		String primaryVolume = volumes.get(0);
		volumes.remove(0);
		
		customParameters.put(ParameterManager.PRIMARY_VOLUME_ID, primaryVolume);
		customParameters.put(ParameterManager.STANDBY_VOLUME_ID, volumes);
	}

	static Map<String, String> copyProperties(Map<String, String> completeList, String... keys) {
		Map<String, String> copiedProps = new HashMap<>();

		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			copiedProps.put(key, completeList.get(key));
		}
		return copiedProps;
	}
}