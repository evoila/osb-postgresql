/**
 * 
 */
package de.evoila.cf.cpi.existing;

/**
 * @author Christian Brinker, evoila.
 *
 */
public interface CustomExistingService {

	CustomExistingServiceConnection connection(String host, int port, String database, String username, String password) throws Exception;

	void bindRoleToInstanceWithPassword(CustomExistingServiceConnection connection, String database,
			String username, String password) throws Exception;

}
