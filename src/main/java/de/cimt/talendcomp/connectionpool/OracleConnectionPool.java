package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.util.Properties;

import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import routines.system.TalendDataSource;

public class OracleConnectionPool extends BasicConnectionPool {
	
	private PoolDataSource dataSourceOra = null;
	private UniversalConnectionPoolManager ucpManager = null;

	public OracleConnectionPool(String connectionUrl, String user, String password) {
		super(connectionUrl, user, password);
	}
	
	@Override
	public void initializePool() throws Exception {
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
		if (this.maxTotal > 0) {
			this.dataSourceOra.setMaxPoolSize(this.maxTotal);
		}
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
			} else if (isDebug()) {
				debug("Initial check connection from pool: number borrowed: " + dataSourceOra.getBorrowedConnectionsCount() + " number available: " + dataSourceOra.getAvailableConnectionsCount());
				testConn.close();
			}
		} catch (Exception e) {
			String message = "Test pool failed. URL=" + this.connectionUrl + " USER=" + this.user + ". Error message=" + e.getMessage();
			error(message, e);
			throw new Exception(message, e);
		}
	}
	
	/**
	 * get data source
	 * instantiate appropriate data source for mysql or oracle pool
	 * @return Interface TalendDataSource
	 */
	@Override
	public TalendDataSource getDataSource() {
		if (pooledTalendDateSource == null) {
			if (dataSourceOra == null) {
				throw new IllegalStateException("Connection pool not set up");
			}
			pooledTalendDateSource = new PooledTalendDataSource(dataSourceOra);
			pooledTalendDateSource.setDebug(isDebug());
			PooledTalendDataSource.setLogger(logger);
		}
		return pooledTalendDateSource;
	}

	/**
	 * close connection pool
	 * @throws Exception
	 */
	@Override
	public void closePool() throws Exception {
		if (ucpManager == null) {
			throw new IllegalStateException("Connection pool not set up");
		}
		this.ucpManager.destroyConnectionPool(this.poolName);
	}

}
