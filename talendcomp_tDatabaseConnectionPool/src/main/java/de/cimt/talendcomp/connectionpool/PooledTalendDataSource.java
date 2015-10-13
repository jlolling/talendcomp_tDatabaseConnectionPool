package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.jdbc.PoolDataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import routines.system.TalendDataSource;

public class PooledTalendDataSource extends TalendDataSource {

	private BasicDataSource ds = null;
	private PoolDataSource dsOra = null;
	private UniversalConnectionPoolManager ucpManager = null;
	private boolean debug = false;
	
	public PooledTalendDataSource(BasicDataSource ds) {
		super(ds);
		if (ds == null) {
			throw new IllegalArgumentException("data source cannot be null");
		}
		this.ds = ds;
	}
	
	public PooledTalendDataSource(PoolDataSource ds, UniversalConnectionPoolManager ucpManager) {
		super(ds);
		if (ds == null) {
			throw new IllegalArgumentException("data source cannot be null");
		}
		if (ucpManager == null) {
			throw new IllegalArgumentException("ucpManager cannot be null");
		}
		this.dsOra = ds;
		this.ucpManager = ucpManager;
	}
	
	/**
	 * getConnection
	 * 
	 * @return
	 * @throws SQLException
	 */
	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = null;
		if (this.ds != null) {
			conn = this.ds.getConnection();
			if (debug) {
				System.out.println("Get connection from pool: number active: " + ds.getNumActive());
				System.out.println("Get connection from pool: number idle: " + ds.getNumIdle());
			}
		} else if (this.dsOra != null) {
			conn = this.dsOra.getConnection();
			if (debug) {
				System.out.println("Get connection from pool: number borrowed: " + dsOra.getBorrowedConnectionsCount());
				System.out.println("Get connection from pool: number available: " + dsOra.getAvailableConnectionsCount());
			}		
		} else {
			throw new IllegalStateException("No data source available");
		}
		return conn;
	}
	
	@Override
	public void close() throws SQLException {
		if (ds != null) {
			this.ds.close();
		} else if (ucpManager != null && dsOra != null) {
			try {
				ucpManager.stopConnectionPool(dsOra.getDataSourceName());
			} catch (UniversalConnectionPoolException e) {
				throw new SQLException(e.getMessage(), e);
			}
		} else {
			throw new IllegalStateException("No data source avialable");
		}
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
}