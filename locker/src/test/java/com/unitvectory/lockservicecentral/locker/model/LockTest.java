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
package com.unitvectory.lockservicecentral.locker.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

/**
 * The Lock test.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class LockTest {

    @Test
    public void testConstructorWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("namespace", "testNamespace");
        map.put("lockName", "testLockName");
        map.put("owner", "testOwner");
        map.put("instanceId", "testInstanceId");
        map.put("leaseDuration", 1000L);
        map.put("expiry", 2000L);

        Lock lock = new Lock(map);

        assertEquals("testNamespace", lock.getNamespace());
        assertEquals("testLockName", lock.getLockName());
        assertEquals("testOwner", lock.getOwner());
        assertEquals("testInstanceId", lock.getInstanceId());
        assertEquals(1000L, lock.getLeaseDuration());
        assertEquals(2000L, lock.getExpiry());
    }
}