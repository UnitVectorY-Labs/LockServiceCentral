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
package com.unitvectory.lockservicecentral.locker.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockAction;
import com.unitvectory.lockservicecentral.locker.LockService;

/**
 * The set of tests the verify the behavior of a LockerService.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public abstract class AbstractLockerTest {

    private LockService lockService;

    protected abstract LockService createLockService();

    long getNow() {
        return System.currentTimeMillis() / 1000;
    }

    @BeforeEach
    void setUp() {
        this.lockService = createLockService();
    }

    @Test
    public void getLockNullTest() {
        // If we try to get a lock that does not exist (by generating a random lock
        // name), we should get back null.
        String name = UUID.randomUUID().toString();
        Lock lock = this.lockService.getLock("junit", name);
        assertNull(lock);
    }

    @Test
    public void getLockExistsTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, 0);
        assertNotNull(acquired);

        // Now we want to get the lock we made
        Lock found = this.lockService.getLock("junit", name);
        assertNotNull(found);

        assertEquals(LockAction.ACQUIRE, found.getAction());
        assertNull(found.getSuccess());
        assertEquals("junit", found.getNamespace());
        assertEquals(name, found.getLockName());
        assertEquals("owner", found.getOwner());
        assertEquals("instance", found.getInstanceId());
        assertEquals(60L, found.getLeaseDuration());
        assertEquals(now + 60, found.getExpiry());
    }

    @Test
    public void acquireLockNewTest() {
        // If we try to acquire a lock that does not exist (by generating a random lock
        // name), we should get back a new lock.
        String name = UUID.randomUUID().toString();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 10L, this.getNow() + 10);
        Lock acquired = this.lockService.acquireLock(lock, 0);
        assertNotNull(acquired);

        assertEquals(LockAction.SUCCESS, acquired.getAction());
        assertEquals(true, acquired.getSuccess());
        assertEquals("junit", acquired.getNamespace());
        assertEquals(name, acquired.getLockName());
        assertEquals("owner", acquired.getOwner());
        assertEquals("instance", acquired.getInstanceId());
        assertEquals(10L, acquired.getLeaseDuration());
        assertEquals(this.getNow() + 10, acquired.getExpiry());
    }

}
