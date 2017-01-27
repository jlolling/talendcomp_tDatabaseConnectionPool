package de.cimt.talendcomp.connectionpool.test;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Hashtable;

import javax.management.ObjectName;

import org.junit.Test;

import de.cimt.talendcomp.connectionpool.ThreadLocalConnectionCache;

public class TestThreadLocalConnectionCache {

	
	public Connection createConnection() throws Exception {
		String url = "jdbc:postgresql://debiandb.local:5432/postgres";
		String user = "postgres";
		String passwd = "postgres";
		String driverClass = "org.postgresql.Driver";
		System.out.println(Thread.currentThread().getName() + ": load driver");
		Class.forName(driverClass).newInstance();
		System.out.println(Thread.currentThread().getName() + ": connect");
		Connection conn_tPostgresqlConnection_1 = java.sql.DriverManager
				.getConnection(url,
						user,
						passwd);
		return conn_tPostgresqlConnection_1;
	}
	
	@Test
	public void testSetGet() throws Exception {
		System.out.println(Thread.currentThread().getName() + ": start");
		Connection conn1 = createConnection();
		ThreadLocalConnectionCache.set("test", conn1);
		Connection conn2 = ThreadLocalConnectionCache.get("test");
		if (conn2.isClosed()) {
			throw new IllegalStateException("Connection was closed already!");
		}
		conn2.close();
		System.out.println(Thread.currentThread().getName() + ": end");
		assertTrue(conn1 == conn2);
	}
	
	@Test
	public void testBuildObjectName() throws Exception {
		Hashtable<String, String> values = new Hashtable<String, String>();
		values.put("type", "BasicConnectionPool");
		values.put("jndiName", "jdbc/ds");
		values.put("url", "jdbc_postgresql_//debiandb.local_5432/postgres");
		ObjectName name = new ObjectName("de.cimt.talendcoomp.pool", values);
		System.out.println(name.toString());
		assertTrue(true);
	}

}
