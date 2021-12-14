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

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import routines.system.TalendDataSource;

public class PooledTalendDataSource extends TalendDataSource {

	private DataSource ds = null;
	private boolean debug = false;
	
	public PooledTalendDataSource(DataSource ds) {
		super(ds);
		if (ds == null) {
			throw new IllegalArgumentException("data source cannot be null");
		}
		this.ds = ds;
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
				debug(Thread.currentThread().getName() + ": DEBUG: Get connection from pool: number active: " + ((BasicDataSource) ds).getNumActive() + ", number idle: " + ((BasicDataSource) ds).getNumIdle());
			}
		} else {
			throw new IllegalStateException("No data source available");
		}
		return conn;
	}
	
	public void closePool() throws SQLException {
   		if (ds != null) {
			if (ds instanceof BasicDataSource) {
				((BasicDataSource) ds).close();
			}
		} else {
			throw new IllegalStateException("No data source avialable");
		}
	}

	@Override
	public void close() {
		// do nothing
	}
	
	public void info(String message) {
		System.out.println(Thread.currentThread().getName() + ": INFO: " + message);
	}
	
	public void debug(String message) {
		System.out.println(Thread.currentThread().getName() + ": DEBUG: " + message);
	}

	public void error(String message) {
		error(message, null);
	}
	
	public void error(String message, Throwable t) {
		if (t != null && (message == null || message.trim().isEmpty())) {
			message = t.getMessage();
		}
		System.err.println(Thread.currentThread().getName() + ": ERROR: " + message);
		if (t != null) {
			t.printStackTrace(System.err);
		}
	}

}