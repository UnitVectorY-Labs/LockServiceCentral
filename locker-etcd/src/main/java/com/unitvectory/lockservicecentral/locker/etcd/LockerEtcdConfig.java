/*
 * Copyright 2024 the original author or authors.
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
package com.unitvectory.lockservicecentral.locker.etcd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.unitvectory.lockservicecentral.locker.LockService;

import io.etcd.jetcd.Client;

/**
 * The Configuration for the etcd LockService.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
public class LockerEtcdConfig {

	@Autowired
	private Client etcdClient;

	@Value("${locker.etcd.keyPrefix:locks/}")
	private String keyPrefix;

	@Value("${locker.etcd.maxRetries:3}")
	private int maxRetries;

	@Value("${locker.etcd.requestTimeoutMs:5000}")
	private long requestTimeoutMs;

	@Bean
	public LockService lockService() {
		return new EtcdLockService(this.etcdClient, this.keyPrefix, this.maxRetries, this.requestTimeoutMs);
	}
}
