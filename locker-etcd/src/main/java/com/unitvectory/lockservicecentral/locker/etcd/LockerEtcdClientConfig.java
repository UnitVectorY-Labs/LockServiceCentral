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
package com.unitvectory.lockservicecentral.locker.etcd;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * The Configuration for the etcd Client.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@Slf4j
public class LockerEtcdClientConfig {

	@Value("${locker.etcd.endpoints:http://localhost:2379}")
	private String endpoints;

	@Value("${locker.etcd.tls.enabled:false}")
	private boolean tlsEnabled;

	@Value("${locker.etcd.tls.caCertPath:#{null}}")
	private String caCertPath;

	@Value("${locker.etcd.tls.clientCertPath:#{null}}")
	private String clientCertPath;

	@Value("${locker.etcd.tls.clientKeyPath:#{null}}")
	private String clientKeyPath;

	@Value("${locker.etcd.auth.username:#{null}}")
	private String username;

	@Value("${locker.etcd.auth.password:#{null}}")
	private String password;

	/**
	 * Creates the etcd Client bean.
	 *
	 * @return the etcd Client instance
	 */
	@Bean
	public Client etcdClient() {
		String[] endpointArray = endpoints.split(",");

		ClientBuilder builder = Client.builder()
				.endpoints(endpointArray);

		// Configure authentication if provided
		if (username != null && !username.isEmpty() && password != null) {
			builder.user(ByteSequence.from(username.getBytes()))
					.password(ByteSequence.from(password.getBytes()));
		}

		// Configure TLS if enabled
		if (tlsEnabled) {
			try {
				SslContextBuilder sslBuilder = GrpcSslContexts.forClient();

				if (caCertPath != null && !caCertPath.isEmpty()) {
					sslBuilder.trustManager(new File(caCertPath));
				}

				if (clientCertPath != null && !clientCertPath.isEmpty()
						&& clientKeyPath != null && !clientKeyPath.isEmpty()) {
					sslBuilder.keyManager(new File(clientCertPath), new File(clientKeyPath));
				}

				builder.sslContext(sslBuilder.build());
			} catch (Exception e) {
				log.error("Failed to configure TLS for etcd client", e);
				throw new RuntimeException("Failed to configure TLS for etcd client", e);
			}
		}

		return builder.build();
	}
}
