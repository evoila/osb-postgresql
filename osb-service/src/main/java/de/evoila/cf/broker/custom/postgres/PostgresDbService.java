/**
 * 
 */
package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Johannes Hiemer
 *
 */
public class PostgresDbService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private Connection connection;

	private boolean createConnection(String database, List<ServerAddress> serverAddresses, Properties properties, boolean ssl) {
		String connectionUrl = ServiceInstanceUtils.connectionUrl(serverAddresses);

		try {
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://" + connectionUrl + "/" + database + "?targetServerType=primary" + (ssl?"&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory":"");
			connection = DriverManager.getConnection(url, properties);

		} catch (ClassNotFoundException | SQLException e) {
			log.info("Could not establish connection", e);
			return false;
		}

		return true;
	}

	public boolean createExtendedConnection(String username, String password, String database, boolean ssl, List<ServerAddress> serverAddresses) {
		Properties properties = new Properties();

		properties.setProperty("user",username);
		properties.setProperty("password",password);
		properties.setProperty("preferQueryMode","extended");

		return createConnection(database, serverAddresses, properties, ssl);
	}

	public boolean createSimpleConnection(String username, String password, String database, boolean ssl, List<ServerAddress> serverAddresses) {
		Properties properties = new Properties();
		properties.setProperty("user",username);
		properties.setProperty("password",password);
		properties.setProperty("preferQueryMode","simple");

		return createConnection(database, serverAddresses, properties, ssl);
	}

	public boolean isConnected() throws SQLException {
		return connection != null && !connection.isClosed();
	}
	
    public void closeIfConnected() {
        try {
            if(isConnected()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.info("Could not close connection", e);
        }
    }

	public void executeUpdate(String query) throws SQLException {
		Statement statement = connection.createStatement();

		try {
			log.debug("Executing the following query: " + query);
			statement.execute(query);
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			statement.close();
		}
	}

	public Map<String, String> executeSelect(String query, String column) throws SQLException {
		Statement statement = connection.createStatement();

		try {
            Map<String, String> resultMap = new HashMap<>();
			ResultSet result = statement.executeQuery(query);

			while(result.next()) {
                resultMap.put(result.getString(column), result.getString(column));
            }

			return resultMap;
		} catch (SQLException e) {
			log.error(e.toString());
			return null;
		}
	}

	public void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
		if (parameterMap == null) {
			throw new SQLException("parameterMap cannot be empty");
		}

		PreparedStatement preparedStatement = connection.prepareStatement(query);

		for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
			preparedStatement.setString(parameter.getKey(), parameter.getValue());
		}

		try {
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			preparedStatement.close();
		}
	}

	public Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap)
			throws SQLException {
		if (parameterMap == null) {
			throw new SQLException("parameterMap cannot be empty");
		}

		PreparedStatement preparedStatement = connection.prepareStatement(query);

		for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
			preparedStatement.setString(parameter.getKey(), parameter.getValue());
		}

		try {
			ResultSet result = preparedStatement.executeQuery();
			ResultSetMetaData resultMetaData = result.getMetaData();
			int columns = resultMetaData.getColumnCount();

			Map<String, String> resultMap = new HashMap<String, String>(columns);

			if (result.next()) {
				for (int i = 1; i <= columns; i++) {
					resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
				}
			}

			return resultMap;
		} catch (SQLException e) {
			log.error(e.toString());
			return null;
		}
	}
}
