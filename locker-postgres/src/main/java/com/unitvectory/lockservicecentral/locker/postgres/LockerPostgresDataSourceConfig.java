/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.unitvectory.lockservicecentral.locker.postgres;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * The Configuration for the Postgres DataSource.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@Profile("postgres")
public class LockerPostgresDataSourceConfig {

	@Value("${locker.postgres.host:localhost}")
	private String host;

	@Value("${locker.postgres.port:5432}")
	private int port;

	@Value("${locker.postgres.database:lockservice}")
	private String database;

	@Value("${locker.postgres.schema:#{null}}")
	private String schema;

	@Value("${locker.postgres.username:postgres}")
	private String username;

	@Value("${locker.postgres.password:#{null}}")
	private String password;

	@Value("${locker.postgres.ssl:false}")
	private boolean ssl;

	@Value("${locker.postgres.connectionPoolSize:10}")
	private int connectionPoolSize;

	/**
	 * Creates the DataSource bean for Postgres connections.
	 *
	 * @return the DataSource instance
	 */
	@Bean
	public DataSource dataSource() {
		HikariConfig config = new HikariConfig();

		// Build JDBC URL
		StringBuilder jdbcUrl = new StringBuilder();
		jdbcUrl.append("jdbc:postgresql://");
		jdbcUrl.append(host);
		jdbcUrl.append(":");
		jdbcUrl.append(port);
		jdbcUrl.append("/");
		jdbcUrl.append(database);

		if (ssl) {
			jdbcUrl.append("?sslmode=require");
		}

		config.setJdbcUrl(jdbcUrl.toString());
		config.setUsername(username);

		if (password != null && !password.isEmpty()) {
			config.setPassword(password);
		}

		// Set schema if provided
		if (schema != null && !schema.isEmpty()) {
			config.setSchema(schema);
		}

		// Connection pool settings
		config.setMaximumPoolSize(connectionPoolSize);
		config.setMinimumIdle(1);
		config.setIdleTimeout(60000);
		config.setConnectionTimeout(30000);
		config.setPoolName("lockservice-postgres-pool");

		// PostgreSQL specific settings
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "25");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		return new HikariDataSource(config);
	}
}
