/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.util.List;

import de.evoila.cf.broker.model.BaseEntity;
import de.evoila.cf.broker.model.ServerAddress;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * @author Christian Mueller, evoila
 *
 */
public class PostgreSqlStackMapping implements BaseEntity<String> {
	private String id;
	private String portsStack;
	private String volumeStack;
	private String primaryStack;
	private List<String> secondaryStacks = Lists.newArrayList();
	private List<ServerAddress> serverAddresses = Lists.newArrayList();

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServiceInstance() {
		return id;
	}

	public void setServiceInstance(String serviceInstance) {
		this.id = serviceInstance;
	}

	public String getPortsStack() {
		return portsStack;
	}

	public void setPortsStack(String portsStack) {
		this.portsStack = portsStack;
	}

	public String getPrimaryStack() {
		return primaryStack;
	}

	public void setPrimaryStack(String primaryStack) {
		this.primaryStack = primaryStack;
	}

	public List<String> getSecondaryStacks() {
		return secondaryStacks;
	}

	public void setSecondaryStacks(List<String> secondaryStacks) {
		this.secondaryStacks = secondaryStacks;
	}

	public void addSecondaryStack(String stack) {
		this.secondaryStacks.add(stack);
	}

	public void removeSecondaryStack(String stack) {
		this.secondaryStacks.remove(stack);
	}

	public List<ServerAddress> getServerAddresses() {
		return this.serverAddresses;
	}

	public void setServerAddresses(List<ServerAddress> serverAddresses) {
		this.serverAddresses = serverAddresses;
	}

	public void addServerAddress(ServerAddress serverAddress) {
		this.serverAddresses.add(serverAddress);
	}

	public void removeServerAddress(ServerAddress serverAddress) {
		this.serverAddresses.remove(serverAddress);
	}

	public void setVolumeStack(String id2) {
		this.volumeStack = volumeStack;
	}
}