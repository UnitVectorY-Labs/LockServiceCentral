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
package com.unitvectory.lockservicecentral.locker.dynamodb;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * The Configuration for the DynamoDB Client.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
public class LockerAwsDynamoDbClientConfig {

	@Value("${locker.dynamodb.region:us-east-1}")
	private String region;

	@Value("${locker.dynamodb.endpoint:#{null}}")
	private String endpoint;

	@Value("${locker.dynamodb.accessKeyId:#{null}}")
	private String accessKeyId;

	@Value("${locker.dynamodb.secretAccessKey:#{null}}")
	private String secretAccessKey;

	/**
	 * Creates the DynamoDbClient bean.
	 *
	 * @return the DynamoDbClient instance
	 */
	@Bean
	public DynamoDbClient dynamoDbClient() {
		DynamoDbClientBuilder builder = DynamoDbClient.builder()
				.region(Region.of(region));

		// Configure credentials
		AwsCredentialsProvider credentialsProvider;
		if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
			// Use static credentials if provided
			AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
			credentialsProvider = StaticCredentialsProvider.create(credentials);
		} else {
			// Use default credentials provider chain (environment variables, instance profile, etc.)
			credentialsProvider = DefaultCredentialsProvider.create();
		}
		builder.credentialsProvider(credentialsProvider);

		// Configure endpoint if provided (for local development or custom endpoints)
		if (endpoint != null && !endpoint.isEmpty()) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}
}
