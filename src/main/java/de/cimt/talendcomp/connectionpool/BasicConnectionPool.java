/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import routines.system.TalendDataSource;

public class BasicConnectionPool {

	protected static Logger logger = null;
	protected String user;
	protected String pass;
	protected String connectionUrl = null;
	protected boolean testOnBorrow = true;
	//private boolean testWhileIdle = true;
	protected Integer timeIdleConnectionIsChecked = 30000;
	protected Integer timeBetweenChecks = 60000;
	protected Integer initialSize = 0;
	protected Integer maxTotal = -1;
	protected Integer maxIdle = -1;
	protected Integer maxWaitForConnection = 0;
	protected Integer numConnectionsPerCheck = 5;
	protected String driver = null;
	private Collection<String> initSQL;
	private BasicDataSource dataSource = null;
	protected PooledTalendDataSource pooledTalendDateSource = null;
	protected String validationQuery = null;
	protected String connectionPropertiesStr = null;
	private static boolean debug = false;
	private static Map<String, routines.system.TalendDataSource> dsMap = null;
	private static Map<String, BasicConnectionPool> poolMap = new ConcurrentHashMap<String, BasicConnectionPool>();
	private boolean autoCommit = false;
	private String jndiName = null;
	private boolean enableJMX = true;
	
	/**
	 * Constructor with necessary params
	 * @param connectionUrl
	 * @param user
	 * @param password
	 * @param databaseType
	 */
	public BasicConnectionPool(String connectionUrl, String user, String password) {
		if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
			throw new IllegalArgumentException("connection url can not be null or empty");
		} else {
			this.connectionUrl = connectionUrl;
		}
		if (user == null || user.trim().isEmpty()) {
			throw new IllegalArgumentException("User can not be null");
		} else if (password == null) {
			throw new IllegalArgumentException(
					"Password can not be null. At least empty String \"\" ");
		} else {
			this.user = user;
			this.pass = password;
		}
	}

	/**
	 * load given driver
	 * @param driver
	 * @throws SQLException
	 */
	public void loadDriver(String driver) throws Exception {
		if (driver == null || driver.trim().isEmpty()) {
			throw new IllegalArgumentException("driver can not be null or empty");
		}
		try {
			Class.forName(driver.trim());
			this.driver = driver;
		} catch (ClassNotFoundException e) {
			throw new Exception("Could not load driver class: " + driver, e);
		}
	}
	
	/**
	 * Creates the data sources and initialize the pool
	 * @throws Exception
	 * @throws Exception, UniversalConnectionPoolException
	 */
	public void initializePool() throws Exception {
		if (this.driver == null) {
			throw new IllegalStateException("Please call method loadDriver before setup datasource");
		}
		if (this.connectionUrl == null) {
			throw new IllegalStateException("Please use method setConnectionString before setup datasource");
		}
		// use org.apache.commons.dbcp2.BasicDataSource
		this.dataSource = new BasicDataSource();
		this.dataSource.setUsername(this.user);
		this.dataSource.setPassword(this.pass);
		this.dataSource.setUrl(this.connectionUrl);
		this.dataSource.setTestOnBorrow(this.testOnBorrow);
		this.dataSource.setTestWhileIdle(true);
		this.dataSource.setMinEvictableIdleTimeMillis(this.timeIdleConnectionIsChecked);
		this.dataSource.setTimeBetweenEvictionRunsMillis(this.timeBetweenChecks);
		this.dataSource.setInitialSize(this.initialSize);
		this.dataSource.setMaxTotal(this.maxTotal);
		this.dataSource.setMaxIdle(this.maxIdle);
		if (enableJMX) {
			this.dataSource.setJmxName(buildJmxName());
		}
		//this.dataSource.setMaxIdle(this.maxIdle);
		if (this.maxWaitForConnection == 0) { 
			this.maxWaitForConnection = -1;
		}
		this.dataSource.setMaxWaitMillis(this.maxWaitForConnection);
		this.dataSource.setNumTestsPerEvictionRun(this.numConnectionsPerCheck);
		this.dataSource.setValidationQuery(this.validationQuery);
		if (initSQL != null && initSQL.isEmpty() == false) {
			this.dataSource.setConnectionInitSqls(this.initSQL);
		}
		this.dataSource.setDefaultAutoCommit(autoCommit);
		this.dataSource.setLifo(false);
		this.dataSource.setLogAbandoned(isDebug());
		this.dataSource.setLogExpiredConnections(isDebug());
		if (connectionPropertiesStr != null) {
			this.dataSource.setConnectionProperties(connectionPropertiesStr);
		}
		// create our first connection to detect connection problems right here
		try {
			Connection testConn = dataSource.getConnection();
			if (testConn == null) {
				throw new Exception("No initial data source available");
			} else {
				testConn.close();
				if (isDebug()) {
					debug("Initial check connection pool: number active: " + dataSource.getNumActive() + "number idle: " + dataSource.getNumIdle());
				}
			}
		} catch (Exception e) {
			String message = "Test pool failed. URL=" + this.connectionUrl + " USER=" + this.user + ". Error message=" + e.getMessage();
			throw new Exception(message, e);
		}
	}
	
	private String prepareConnectionProperties(String properties) {
		if (properties != null) {
			if (properties.startsWith("?")) {
				properties = properties.substring(1);
			}
			properties = properties.replace("&", ";");
			return properties;
		} else {
			return "";
		}
	}

	/**
	 * get data source
	 * instantiate appropriate data source for mysql or oracle pool
	 * @return Interface TalendDataSource
	 */
	public TalendDataSource getDataSource() {
		if (pooledTalendDateSource == null) {
			if (dataSource == null) {
				throw new IllegalStateException("Connection pool not set up");
			}
			pooledTalendDateSource = new PooledTalendDataSource(dataSource);
			pooledTalendDateSource.setDebug(isDebug());
			PooledTalendDataSource.setLogger(logger);
		}
		return pooledTalendDateSource;
	}

	/**
	 * set additional parameter separated by semicolon
	 * @param propertiesStr
	 * @throws SQLException
	 */
	public void setAdditionalProperties(String propertiesStr) throws SQLException {
		if (propertiesStr != null && propertiesStr.trim().isEmpty() == false) {
			this.connectionPropertiesStr = prepareConnectionProperties(propertiesStr.trim());
		} else {
			this.connectionPropertiesStr = null;
		}
	}
	
	/**
	 * close connection pool
	 * @throws Exception
	 */
	public void closePool() throws Exception {
		if (dataSource == null) {
			throw new IllegalStateException("Connection pool not set up");
		}
		this.dataSource.close();
		poolMap.remove(jndiName);
	}

	public boolean getTestOnBorrow() {
		return testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	public Integer getTimeIdleConnectionIsChecked() {
		return timeIdleConnectionIsChecked;
	}

	/**
	 * time an connection can be in idle state before it is checked <br>
	 * required testWhileIdle = true<br>
	 * default = 60000 Milli Sec (MySql), Oracle 60000 (Sec)
	 * 
	 * @param timeIdleConnectionIsChecked
	 */
	public void setTimeIdleConnectionIsChecked(Integer timeIdleConnectionIsChecked) {
		if (timeIdleConnectionIsChecked == null) {
			throw new IllegalArgumentException("timeIdleConnectionIsChecked can not be null");
		} else {
			this.timeIdleConnectionIsChecked = timeIdleConnectionIsChecked;
		}

	}

	public Integer getTimeBetweenChecks() {
		return timeBetweenChecks;
	}

	/**
	 * time between checks for connections in idle state<br>
	 * required testWhileIdle = true<br>
	 * default = 60000 Milli Sec (MySql), Oracle 60000 (Sec)
	 * 
	 * @param timeBetweenChecks
	 */
	public void setTimeBetweenChecks(Integer timeBetweenChecks) {
		if (timeBetweenChecks == null) {
			throw new IllegalArgumentException("timeBetweenChecks can not be null");
		} else {
			this.timeBetweenChecks = timeBetweenChecks;
		}

	}

	public Integer getInitialSize() {
		return initialSize;
	}

	/**
	 * default = 0
	 * 
	 * @param initialSize
	 */
	public void setInitialSize(Integer initialSize) {
		if (initialSize == null) {
			throw new IllegalArgumentException("initialSize can not be null");
		} else {
			this.initialSize = initialSize;
		}

	}

	public Integer getMaxTotal() {
		return maxTotal;
	}

	public Integer getMaxIdle() {
		return maxIdle;
	}

	/**
	 * max number of connections in pool<br>
	 * <br>
	 * default = 5
	 * 
	 * @param maxTotal
	 */
	public void setMaxTotal(Integer maxTotal) {
		if (maxTotal == null) {
			throw new IllegalArgumentException("maxTotal can not be null");
		} else {
			this.maxTotal = maxTotal;
			this.maxIdle = maxTotal; // max idle should be the same value as max total
		}

	}

	public Integer getMaxWaitForConnection() {
		return maxWaitForConnection;
	}

	/**
	 * Time to wait for connections if maxTotal size is reached<br>
	 * default = 0 
	 * 
	 * @param maxWaitForConnection
	 */
	public void setMaxWaitForConnection(Integer maxWaitForConnection) {
		if (maxWaitForConnection == null) {
			throw new IllegalArgumentException("maxWaitForConnection can not be null");
		} else {
			this.maxWaitForConnection = maxWaitForConnection;
		}
	}

	public Integer getNumConnectionsPerCheck() {
		return numConnectionsPerCheck;
	}

	/**
	 * number of connections in idle state that are checked <br>
	 * default = 5
	 * 
	 * @param numConnectionsPerCheck
	 */
	public void setNumConnectionsPerCheck(Integer numConnectionsPerCheck) {
		if (numConnectionsPerCheck == null) {
			throw new IllegalArgumentException("numConnectionsPerCheck can not be null");
		} else {
			this.numConnectionsPerCheck = numConnectionsPerCheck;
		}
	}
	
	public Collection<String> getInitSQL() {
		return initSQL;
	}

	/**
	 * set SQL that is fired before a connection become available in pool
	 * separated by semicolon
	 * @param initSQL
	 */
	public void setInitSQL(String initSQL) {
		if (initSQL != null && initSQL.trim().isEmpty() == false) {
			this.initSQL = new ArrayList<String>();
			String[] splitted = initSQL.split(";");
			for (String sql : splitted) {
				if (sql != null && sql.trim().isEmpty() == false) {
					this.initSQL.add(sql.trim());
				}
			}
		}
	}
	
	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(String validationQuery) {
		if (validationQuery != null && validationQuery.trim().isEmpty() == false) {
			this.validationQuery = validationQuery;
		}
	}
	
	public int getNumActiveConnections() {
		if (dataSource != null) {
			return dataSource.getNumActive();
		} else {
			return 0;
		}
	}

	public static boolean isDebug() {
		if (logger != null) {
			return logger.getLevel().equals(Level.DEBUG);
		} else {
			return debug;
		}
	}

	public static void setDebug(boolean debug) {
		BasicConnectionPool.debug = debug;
		if (logger != null) {
			if (debug) {
				logger.setLevel(Level.DEBUG);
			} else {
				logger.setLevel(Level.INFO);
			}
		}
	}

	public static void info(String message) {
		if (logger != null) {
			logger.info(message);
		} else {
			System.out.println(Thread.currentThread().getName() + ": INFO: " + message);
		}
	}
	
	public static void warn(String message) {
		if (logger != null) {
			logger.warn(message);
		} else {
			System.out.println(Thread.currentThread().getName() + ": WARN: " + message);
		}
	}

	public static void debug(String message) {
		if (isDebug()) {
			if (logger != null) {
				logger.debug(message);
			} else {
				System.out.println(Thread.currentThread().getName() + ": DEBUG: " + message);
			}
		}
	}

	public static void error(String message) {
		error(message, null);
	}
	
	public static void error(String message, Throwable t) {
		if (t != null && (message == null || message.trim().isEmpty())) {
			message = t.getMessage();
		}
		if (logger != null) {
			if (t != null) {
				logger.error(message, t);
			} else {
				logger.error(message);
			}
		} else {
			System.err.println(Thread.currentThread().getName() + ": ERROR: " + message);
			if (t != null) {
				t.printStackTrace(System.err);
			}
		}
	}

	public static Map<String, routines.system.TalendDataSource> getDsMap() {
		return dsMap;
	}

	public static void setDsMap(Map<String, routines.system.TalendDataSource> dsMap) {
		BasicConnectionPool.dsMap = dsMap;
	}
	
	public static void putInstance(String alias, BasicConnectionPool pool) {
		poolMap.put(alias, pool);
	}
	
	public static BasicConnectionPool getInstance(String alias) {
		return poolMap.get(alias);
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}
	
	public static void setLogger(Logger logger) {
		BasicConnectionPool.logger = logger;
	}

	public static void convertToPooledDataSources(Map<String, Object> globalMap, String dataSourceKey) {
		@SuppressWarnings("unchecked")
		Map<String, routines.system.TalendDataSource> dsmap = (Map<String, routines.system.TalendDataSource>) globalMap.get(dataSourceKey);
		if (dsmap == null) {
			warn("No DataSources are available. This is not a problem if this job does not run in a service.");
		} else {
			for (Map.Entry<String, routines.system.TalendDataSource> entry : dsmap.entrySet()) {
				routines.system.TalendDataSource tds = entry.getValue();
				if (tds instanceof PooledTalendDataSource) {
					continue; // do not stack pooled data sources
				}
				if (tds.getRawDataSource() != null) {
					debug("Replace TalendDataSource for alias: " + entry.getKey() + " with a PooledTalendDataSource");
					PooledTalendDataSource pds = new PooledTalendDataSource(tds.getRawDataSource());
					dsmap.put(entry.getKey(), pds);
				}
			}
		}
	}

	public static void convertToPooledDataSource(Map<String, Object> globalMap, String dataSourceKey, String alias) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, routines.system.TalendDataSource> dsmap = (Map<String, routines.system.TalendDataSource>) globalMap.get(dataSourceKey);
		if (dsmap == null) {
			throw new Exception("No DataSources are available.");
		}
		routines.system.TalendDataSource tds = dsmap.get(alias);
		if (tds == null) {
			throw new Exception("No DataSource: " + alias + " are available.");
		}
		if ((tds instanceof PooledTalendDataSource) == false) {
			debug("Replace TalendDataSource for alias: " + alias + " with a PooledTalendDataSource");
			PooledTalendDataSource pds = new PooledTalendDataSource(tds.getRawDataSource());
			dsmap.put(alias, pds);
		}
	}

	public void setJndiName(String jndiName) {
		if (jndiName != null && jndiName.trim().isEmpty() == false) {
			this.jndiName = jndiName.trim();
		} else {
			this.jndiName = null;
		}
	}
	
	public String getJndiName() {
		return jndiName;
	}
	
	public String buildJmxName() {
		if (jndiName != null && jndiName.trim().isEmpty() == false) {
			return "de.cimt.talendcomp.connectionpool:type=BasicConnectionPool,jndiName="  + jndiName.trim().replace(':', '_');
		} else {
			return null;
		}
	}

	public boolean isEnableJMX() {
		return enableJMX;
	}

	public void setEnableJMX(Boolean enableJMX) {
		if (enableJMX != null) {
			this.enableJMX = enableJMX.booleanValue();
		}
	}
	
}
