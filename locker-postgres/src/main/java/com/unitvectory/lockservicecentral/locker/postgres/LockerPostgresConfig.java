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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

/**
 * The Configuration for the Postgres LockService.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@Profile("postgres")
public class LockerPostgresConfig {

	@Autowired
	private DataSource dataSource;

	@Value("${locker.postgres.tableName:locks}")
	private String tableName;

	/**
	 * Creates the LockService bean.
	 *
	 * @param canonicalLogContextProvider the canonical log context provider
	 * @return the LockService instance
	 */
	@Bean
	public LockService lockService(ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
		return new PostgresLockService(this.dataSource, this.tableName, canonicalLogContextProvider);
	}
}
