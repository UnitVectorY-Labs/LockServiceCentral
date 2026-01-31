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

import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.ObjectProvider;

import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.locker.tests.AbstractLockServiceTest;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * The DynamoDbLockService test with actual DynamoDB server.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Disabled
public class DynamoDbLockServiceActualTest extends AbstractLockServiceTest {

    @Override
    protected LockService createLockService() {
        // These tests are disabled because they require interaction with an actual
        // DynamoDB server (or DynamoDB Local) to run. These are only intended to be used for manual
        // local testing.

        // For DynamoDB Local running on localhost:8000
        DynamoDbClient client = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:8000"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build();

        // Use a no-op ObjectProvider for testing
        ObjectProvider<CanonicalLogContext> noOpProvider = new ObjectProvider<>() {
            @Override
            public CanonicalLogContext getObject() {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getObject(Object... args) {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getIfAvailable() {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getIfUnique() {
                return new CanonicalLogContext();
            }
        };

        return new DynamoDbLockService(client, "locks", noOpProvider);
    }

    @Override
    protected String getExpectedBackendName() {
        return "dynamodb";
    }

}
