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

import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.ObjectProvider;

import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.locker.tests.AbstractLockServiceTest;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import io.etcd.jetcd.Client;

/**
 * The EtcdLockService test with actual etcd server.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Disabled
public class EtcdLockServiceActualTest extends AbstractLockServiceTest {

    @Override
    protected LockService createLockService() {
        // These tests are disabled because they require interaction with an actual
        // etcd server to run. These are only intended to be used for manual
        // local testing.
        Client client = Client.builder()
                .endpoints("http://localhost:2379")
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
        return new EtcdLockService(client, "locks/", 3, 5000, noOpProvider);
    }

}
