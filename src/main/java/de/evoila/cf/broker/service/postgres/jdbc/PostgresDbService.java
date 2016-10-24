/**
 * 
 */
package de.evoila.cf.broker.service.postgres.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Hiemer
 *
 */
public class PostgresDbService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private Connection connection;

	private String host;

	private int port;

	public boolean createConnection(String instanceId, String host, int port) {
		this.host = host;
		this.port = port;
		try {
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://" + host + ":" + port + "/" + instanceId;
			connection = DriverManager.getConnection(url, instanceId, instanceId);
		} catch (ClassNotFoundException | SQLException e) {
			log.info("Could not establish connection", e);
			return false;
		}
		return true;
	}

	public boolean isConnected() throws SQLException {
		return connection != null && !connection.isClosed();
	}

	public void checkValidUUID(String instanceId) throws SQLException {
		UUID uuid = UUID.fromString(instanceId);

		if (!instanceId.equals(uuid.toString())) {
			throw new SQLException("UUID '" + instanceId + "' is not an UUID.");
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

	public Map<String, String> executeSelect(String query) throws SQLException {
		Statement statement = connection.createStatement();

		try {
			ResultSet result = statement.executeQuery(query);
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

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
