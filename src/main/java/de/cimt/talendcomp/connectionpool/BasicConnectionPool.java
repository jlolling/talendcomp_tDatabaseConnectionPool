package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import org.apache.commons.dbcp2.BasicDataSource;

import routines.system.TalendDataSource;

public class BasicConnectionPool {

	private String user;
	private String pass;
	private String databaseType = null;
	private String connectionUrl = null;
	private boolean testOnBorrow = true;
	//private boolean testWhileIdle = true;
	private Integer timeIdleConnectionIsChecked = 30000;
	private Integer timeBetweenChecks = 60000;
	private Integer initialSize = 0;
	private Integer maxTotal = 5;
	//private Integer maxIdle = 5;
	private Integer maxWaitForConnection = 0;
	private Integer numConnectionsPerCheck = 5;
	private String driver = null;
	private Collection<String> initSQL;
	private BasicDataSource dataSource = null;
	private PoolDataSource dataSourceOra = null;
	private UniversalConnectionPoolManager ucpManager = null;
	private PooledTalendDataSource pooledTalendDateSource = null;
	private String poolName = null;
	private String validationQuery = null;
	private String connectionPropertiesStr = null;
	private boolean debug = false;
	private static Map<String, routines.system.TalendDataSource> dsMap = null;
	private static Map<String, BasicConnectionPool> poolMap = new HashMap<String, BasicConnectionPool>();
	private boolean autoCommit = false;
	
	/**
	 * Constructor with necessary params
	 * @param connectionUrl
	 * @param user
	 * @param password
	 * @param databaseType
	 */
	public BasicConnectionPool(String connectionUrl, String user, String password, String databaseType) {
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
		} else if (databaseType == null || databaseType.trim().isEmpty()) {
			throw new IllegalArgumentException(
					"databaseType can not be null. Use 'MySQL', 'Oracle', 'DB2', 'Postgres' or 'SQLServer' \"\" ");
		} else {
			this.user = user;
			this.pass = password;
			this.databaseType = databaseType.trim().toUpperCase();
		}
	}

	/**
	 * load given driver
	 * @param driver
	 * @throws SQLException
	 */
	public void loadDriver(String driver) throws Exception {
		if (driver == null || driver.trim().isEmpty()){
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
	public void initializePool() throws Exception, UniversalConnectionPoolException {
		if (this.driver == null) {
			throw new IllegalStateException("Please use method loadDriver befor setup datasource");
		}
		if (this.connectionUrl == null) {
			throw new IllegalStateException("Please use method setConnectionString befor setup datasource");
		}
		if (this.databaseType.equals("ORACLE")) {
			// use oracle.ucp.*
			// due to casting in talend oracle components
			this.dataSourceOra = PoolDataSourceFactory.getPoolDataSource();
			this.dataSourceOra.setConnectionFactoryClassName(this.driver);
			this.dataSourceOra.setConnectionPoolName(this.poolName);
			this.dataSourceOra.setUser(this.user);
			this.dataSourceOra.setPassword(this.pass);
			this.dataSourceOra.setURL(this.connectionUrl);
			this.dataSourceOra.setValidateConnectionOnBorrow(this.testOnBorrow);
			this.dataSourceOra.setInactiveConnectionTimeout(this.timeIdleConnectionIsChecked);
			this.dataSourceOra.setTimeoutCheckInterval(this.timeBetweenChecks);
			this.dataSourceOra.setInitialPoolSize(this.initialSize);
			this.dataSourceOra.setMaxPoolSize(this.maxTotal);
			this.dataSourceOra.setConnectionWaitTimeout(this.maxWaitForConnection);
			this.dataSourceOra.setSQLForValidateConnection(this.validationQuery);
			if (connectionPropertiesStr != null) {
				String[] entries = connectionPropertiesStr.split(";");
		        Properties connnProperties = new Properties();
		        for (String entry : entries) {
		            if (entry.length() > 0) {
		            	int index = entry.indexOf('=');
		            	if (index > 0) {
		            		String name = entry.substring(0, index);
		                    String value = entry.substring(index + 1);
		                    connnProperties.setProperty(name, value);
		                }
		            }
		        }
				this.dataSourceOra.setConnectionProperties(connnProperties);
			}
			this.ucpManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
			this.ucpManager.createConnectionPool((UniversalConnectionPoolAdapter) this.dataSourceOra);
			this.ucpManager.startConnectionPool(this.poolName);
			// create our first connection to detect connection problems right here
			try {
				Connection testConn = dataSourceOra.getConnection();
				if (testConn == null) {
					throw new Exception("No initial data source available");
				} else if (debug) {
					System.out.println("Initial check connection from pool: number borrowed: " + dataSourceOra.getBorrowedConnectionsCount());
					System.out.println("Initial check connection from pool: number available: " + dataSourceOra.getAvailableConnectionsCount());
					testConn.close();
				}
			} catch (Exception e) {
				String message = "Test pool failed. URL=" + this.connectionUrl + " USER=" + this.user + ". Error message=" + e.getMessage();
				throw new Exception(message, e);
			}
		} else {
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
			this.dataSource.setLogAbandoned(debug);
			this.dataSource.setLogExpiredConnections(debug);
			if (connectionPropertiesStr != null) {
				this.dataSource.setConnectionProperties(connectionPropertiesStr.trim());
			}
			// create our first connection to detect connection problems right here
			try {
				Connection testConn = dataSource.getConnection();
				if (testConn == null) {
					throw new Exception("No initial data source available");
				} else if (debug) {
					System.out.println("Initial check connection pool: number active: " + dataSource.getNumActive());
					System.out.println("initial check connection pool: number idle: " + dataSource.getNumIdle());
					testConn.close();
				}
			} catch (Exception e) {
				String message = "Test pool failed. URL=" + this.connectionUrl + " USER=" + this.user + ". Error message=" + e.getMessage();
				throw new Exception(message, e);
			}
		}
	}

	/**
	 * get data source
	 * instantiate appropriate data source for mysql or oracle pool
	 * @return Interface TalendDataSource
	 */
	public TalendDataSource getDataSource() {
		if (pooledTalendDateSource == null) {
			if (this.databaseType.equals("ORACLE")) {
				if (ucpManager == null) {
					throw new IllegalStateException("Connection pool not set up");
				}
				pooledTalendDateSource = new PooledTalendDataSource(dataSourceOra, ucpManager);
			} else {
				if (dataSource == null) {
					throw new IllegalStateException("Connection pool not set up");
				}
				pooledTalendDateSource = new PooledTalendDataSource(dataSource);
			}
			pooledTalendDateSource.setDebug(debug);
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
			this.connectionPropertiesStr = propertiesStr;
		}
	}
	
	/**
	 * close connection pool
	 * @throws SQLException
	 * @throws UniversalConnectionPoolException
	 */
	public void close() throws SQLException, UniversalConnectionPoolException {
		if (this.databaseType.equals("ORACLE")) {
			if (ucpManager == null) {
				throw new IllegalStateException("Connection pool not set up");
			}
			this.ucpManager.destroyConnectionPool(this.poolName);
		} else {
			if (dataSource == null) {
				throw new IllegalStateException("Connection pool not set up");
			}
			this.dataSource.close();
		}
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
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
		} else if (dataSourceOra != null) {
			try {
				return dataSourceOra.getBorrowedConnectionsCount();
			} catch (SQLException e) {
				e.printStackTrace();
				return 0;
			}
		} else {
			return 0;
		}
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
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

}
