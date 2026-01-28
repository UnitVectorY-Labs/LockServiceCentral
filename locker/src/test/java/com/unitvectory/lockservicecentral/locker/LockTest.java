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
package com.unitvectory.lockservicecentral.locker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * The Lock test.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
class LockTest {

    @Test
    void testConstructorWithMap() {
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

    @Test
    void copyTest() {

        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        Lock copy = lock.copy();

        // Assert that lock and copy are not the exact same object
        assertNotSame(lock, copy);

        assertEquals("testNamespace", copy.getNamespace());
        assertEquals("testLockName", copy.getLockName());
        assertEquals("testOwner", copy.getOwner());
        assertEquals("testInstanceId", copy.getInstanceId());
        assertEquals(1000L, copy.getLeaseDuration());
        assertEquals(2000L, copy.getExpiry());
    }

    @Test
    void toMapTest() {

        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        Map<String, Object> map = lock.toMap();

        assertEquals("testNamespace", map.get("namespace"));
        assertEquals("testLockName", map.get("lockName"));
        assertEquals("testOwner", map.get("owner"));
        assertEquals("testInstanceId", map.get("instanceId"));
        assertEquals(1000L, map.get("leaseDuration"));
        assertEquals(2000L, map.get("expiry"));
    }

    @Test
    void setGetTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        lock.setGet(3000L);

        assertEquals(LockAction.GET, lock.getAction());
        assertNull(lock.getSuccess());
        assertNull(lock.getInstanceId());
        assertNull(lock.getLeaseDuration());
        assertNull(lock.getExpiry());
    }

    @Test
    void setFailedTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        lock.setFailed();

        assertEquals(LockAction.FAILED, lock.getAction());
        assertFalse(lock.getSuccess());
        assertNull(lock.getOwner());
        assertNull(lock.getInstanceId());
        assertNull(lock.getLeaseDuration());
        assertNull(lock.getExpiry());
    }

    @Test
    void setSuccessTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        lock.setSuccess();

        assertEquals(LockAction.SUCCESS, lock.getAction());
        assertTrue(lock.getSuccess());
        assertEquals("testOwner", lock.getOwner());
        assertEquals("testInstanceId", lock.getInstanceId());
        assertEquals(1000L, lock.getLeaseDuration());
        assertEquals(2000L, lock.getExpiry());
    }

    @Test
    void setClearedTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        lock.setCleared();

        assertEquals(LockAction.SUCCESS, lock.getAction());
        assertTrue(lock.getSuccess());
        assertNull(lock.getOwner());
        assertNull(lock.getInstanceId());
        assertNull(lock.getLeaseDuration());
        assertNull(lock.getExpiry());
    }

    @Test
    void isActiveTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        assertTrue(lock.isActive(1999L));
        assertTrue(lock.isActive(2000L));
        assertFalse(lock.isActive(2001L));
    }

    @Test
    void isExpiredTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        assertFalse(lock.isExpired(1999L));
        assertFalse(lock.isExpired(2000L));
        assertTrue(lock.isExpired(2001L));
    }

    @Test
    void isMatchTest() {
        Lock lock = new Lock();
        lock.setNamespace("testNamespace");
        lock.setLockName("testLockName");
        lock.setOwner("testOwner");
        lock.setInstanceId("testInstanceId");
        lock.setLeaseDuration(1000L);
        lock.setExpiry(2000L);

        Lock lock2 = new Lock();
        lock2.setNamespace("testNamespace");
        lock2.setLockName("testLockName");
        lock2.setOwner("testOwner");
        lock2.setInstanceId("testInstanceId");
        lock2.setLeaseDuration(1000L);
        lock2.setExpiry(2000L);

        assertTrue(lock.isMatch(lock2));

        lock2.setNamespace("testNamespace2");
        assertFalse(lock.isMatch(lock2));
    }
}