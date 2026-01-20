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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;

/**
 * The EtcdLockService test with mocks.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class EtcdLockServiceTest {

    @Test
    public void getLockNotFoundTest() {
        Client mockClient = mock(Client.class);
        KV mockKvClient = mock(KV.class);
        GetResponse mockGetResponse = mock(GetResponse.class);

        when(mockClient.getKVClient()).thenReturn(mockKvClient);
        when(mockKvClient.get(any())).thenReturn(CompletableFuture.completedFuture(mockGetResponse));
        when(mockGetResponse.getKvs()).thenReturn(Collections.emptyList());

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
        EtcdLockService service = new EtcdLockService(mockClient, "locks/", 3, 5000, noOpProvider);

        // Call the method under test
        Lock lock = service.getLock("foo", "bar");

        // Verify the result
        assertNull(lock);
    }
}
