package de.cimt.talendcomp.connectionpool.test;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Driver;

import org.junit.Test;

import de.cimt.talendcomp.connectionpool.ThreadLocalConnectionCache;

public class TestThreadLocalConnectionCache {

	
	public Connection createConnection() throws Exception {
		String url = "jdbc:postgresql://debiandb.local:5432/postgres";
		String user = "postgres";
		String passwd = "postgres";
		String driverClass = "org.postgresql.Driver";
		System.out.println(Thread.currentThread().getName() + ": load driver");
		Driver driver = (Driver) Class.forName(driverClass).newInstance();
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
		Connection conn = createConnection();
		ThreadLocalConnectionCache.set("test", conn);
		Connection conn2 = ThreadLocalConnectionCache.get("test");
		if (conn2.isClosed()) {
			throw new IllegalStateException("Connection was closed already!");
		}
		conn2.close();
		System.out.println(Thread.currentThread().getName() + ": end");
		assertTrue(true);
	}
	
	@Test
	public void testSetGetThreaded() {

		Runnable oneRequest = new Runnable() {
			
			@Override
			public void run() {
				try {
					System.out.println(Thread.currentThread().getName() + ": start");
					Connection conn = createConnection();
					ThreadLocalConnectionCache.set("test", conn);
					Thread.sleep(5000);
					Connection conn2 = ThreadLocalConnectionCache.get("test");
					if (conn2.isClosed()) {
						throw new IllegalStateException("Connection was closed already!");
					}
					conn2.close();
					System.out.println(Thread.currentThread().getName() + ": end");
					assertTrue(true);
				} catch (Exception e) {
					e.printStackTrace();
					assertTrue(false);
				}
			}
		};
		
		Thread t1 = new Thread(oneRequest);
		Thread t2 = new Thread(oneRequest);
		Thread t3 = new Thread(oneRequest);
		Thread t4 = new Thread(oneRequest);
		Thread t5 = new Thread(oneRequest);
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		
	}
	

}
