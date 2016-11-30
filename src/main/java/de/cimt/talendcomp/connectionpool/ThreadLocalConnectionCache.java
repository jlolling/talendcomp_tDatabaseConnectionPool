package de.cimt.talendcomp.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalConnectionCache {
	
	private static Map<String, ThreadLocal<Connection>> connectionMap = new HashMap<String, ThreadLocal<Connection>>();
	
	public static void set(String sharedConnectionName, Connection conn) {
		ThreadLocal<Connection> tl = connectionMap.get(sharedConnectionName);
		if (tl == null) {
			tl = new ThreadLocal<Connection>();
		}
		tl.set(conn);
	}
	
	public static Connection get(String sharedConnectionName) {
		ThreadLocal<Connection> tl = connectionMap.get(sharedConnectionName);
		if (tl != null) {
			return tl.get();
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
		}
	}

}
