package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalConnectionCache {
	
	private static Map<String, ThreadLocal<Connection>> connectionMap = new HashMap<String, ThreadLocal<Connection>>();
	
	private static boolean isClosed(Connection conn) {
		try {
			return conn.isClosed();
		} catch (SQLException e) {
			return true;
		}
	}
	
	public static void set(String sharedConnectionName, Connection conn) {
		if (sharedConnectionName == null || sharedConnectionName.trim().isEmpty()) {
			throw new IllegalArgumentException("sharedConnectionName cannot be null or empty!");
		}
		if (conn == null) {
			throw new IllegalArgumentException("connection cannot be null!");
		}
		if (isClosed(conn)) {
			throw new IllegalArgumentException("connection cannot be closed!");
		}
		ThreadLocal<Connection> tl = connectionMap.get(sharedConnectionName);
		if (tl == null) {
			tl = new ThreadLocal<Connection>();
			connectionMap.put(sharedConnectionName, tl);
		}
		tl.set(conn);
	}
	
	public static Connection get(String sharedConnectionName) {
		ThreadLocal<Connection> tl = connectionMap.get(sharedConnectionName);
		if (tl != null) {
			Connection conn = tl.get();
			if (conn != null && isClosed(conn) == false) {
				return conn;
			} else {
				tl.remove();
				return null;
			}
		}
		return null;
	}

	public static void remove(String sharedConnectionName) {
		ThreadLocal<Connection> tl = connectionMap.get(sharedConnectionName);
		if (tl != null) {
			Connection conn = tl.get();
			try {
				if (conn != null && conn.isClosed() == false) {
					conn.close();
				}
			} catch (SQLException sqle) {
				// ignore
			}
			tl.remove();
		}
	}

}
