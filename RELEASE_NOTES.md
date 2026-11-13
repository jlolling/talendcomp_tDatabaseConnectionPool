# Release 2.10 – tDatabaseConnectionPool

Talend components that provide a database connection pool (Apache Commons DBCP2) inside a Talend job.

Components included in this release:

* tPostgresqlConnectionPool
* tMysqlConnectionPool
* tOracleConnectionPool
* tDB2ConnectionPool

Documentation (PostgreSQL edition as example): [tPostgresqlConnectionPool.pdf](https://github.com/jlolling/talendcomp_tDatabaseConnectionPool/blob/master/doc/tPostgresqlConnectionPool.pdf)

## What's new in 2.10

### Build and project structure

* The component sources moved from `talend_component/` to the Maven standard location `src/main/components/`.
* The build now uses `cimt-talendcomp-maven-plugin` 3.1 (previously 3.0). The source and component directories no longer need to be configured explicitly.
* **Java 11 is now required.** The compile target was raised from Java 1.8 to Java 11.
* Library jars (`commons-dbcp2`, `commons-pool2`, `commons-logging` and the component jar) are no longer committed inside the component folders. The Maven build adds them when it packages the components.
* Maven artifact: `de.jlo.talendcomp:jlo-talendcomp-connectionpool:2.10`.

### Component configuration

* All components: the values of the `MODE` options changed from `CREATE_POOL` / `CLOSE_POOL` to `CREATE` / `CLOSE`, so they now match the option names. The code templates (`*_begin.javajet`) were updated to check for the new values.
* tPostgresqlConnectionPool:
  * The `Mode` selection is now the first row of the settings. The repository property type follows in the second row and is shown only in `CREATE` mode.
  * The repository value mappings (host, port, database, schema, user, password) were removed from the connection fields.
  * The `Connection pool` selection in `CLOSE` mode no longer uses dynamic settings.

### Documentation

* The README now lists all four components and links to the documentation.

## Version history

| Version | Date       | Changes |
|---------|------------|---------|
| 2.10    | 2026-11-13 | Project structure moved to `src/main/components`, Java 11, Maven plugin 3.1, mistake in the component XML config fixed (see above) |
| 2.9     | 2023-02-06 | Compile error fixed: `setDebug` was missing |
| 2.8     | 2021-12-14 | Log4j dependency removed; build setup for DB2 added. Also included since 2.7: tDB2ConnectionPool added, schema is set for the tPostgresqlConnection component, closing of the test connection fixed, dependency on talend-job-mock removed, documentation improved |
| 2.7     | 2018-11-30 | Mock artifact added for testing |
| 2.6     | 2017-12-13 | Max idle connections set to the same value as max total connections |
| 2.5     | 2017-05-11 | Compatible with Talend 6.3.1 and later |
| 2.4     | 2017-02-02 | Bug fixed: NullPointerException while closing the pool |
| 2.3     | 2017-01-30 | Better behavior when the pool is created in a child job instead of the parent job (not tagged) |
| 2.2     | 2017-01-17 | tOracleConnectionPool added (Oracle pool moved into its own class); debug logging uses the logger if present; If trigger enabled; new Maven plugin version |

## Upgrade notes

* Talend Studio and the job runtime must run on **Java 11 or later**.
* Install the components from the build output into your Studio user component folder (`custom_component_folder`), replacing the previous version.
* Check existing jobs that use tPostgresqlConnectionPool with repository metadata: the connection fields are no longer filled from the repository metadata automatically.
