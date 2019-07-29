package routines.system;

import java.sql.SQLException;

public class TalendDataSource {

	private final javax.sql.DataSource ds;

	public TalendDataSource(javax.sql.DataSource ds) {
		this.ds = ds;
	}

	public java.sql.Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	public javax.sql.DataSource getRawDataSource() {
		return ds;
	}

	public void close() throws SQLException {
		// do nothing
	}
}