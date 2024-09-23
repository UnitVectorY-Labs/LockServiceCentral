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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(acquired.getSuccess());
        assertEquals("junit", acquired.getNamespace());
        assertEquals(name, acquired.getLockName());
        assertEquals("owner", acquired.getOwner());
        assertEquals("instance", acquired.getInstanceId());
        assertEquals(10L, acquired.getLeaseDuration());
        assertEquals(this.getNow() + 10, acquired.getExpiry());
    }

    @Test
    public void acquireLockExistingTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, 0);
        assertNotNull(acquired);

        // Now we want to acquire the lock we made
        Lock lock2 = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner2", "instance2", 60L, now + 60);
        Lock acquired2 = this.lockService.acquireLock(lock2, 0);
        assertNotNull(acquired2);

        assertEquals(LockAction.FAILED, acquired2.getAction());
        assertFalse(acquired2.getSuccess());
        assertEquals("junit", acquired2.getNamespace());
        assertEquals(name, acquired2.getLockName());
        assertNull(acquired2.getOwner());
        assertNull(acquired2.getInstanceId());
        assertNull(acquired2.getLeaseDuration());
        assertNull(acquired2.getExpiry());
    }

    @Test
    public void renewLockNotExistsTest() {
        // If we try to renew a lock that does not exist (by generating a random lock
        // name), we should get back null.
        String name = UUID.randomUUID().toString();
        Lock lock = new Lock(LockAction.RENEW, null, "junit", name, "owner", "instance", 10L, this.getNow() + 10);
        Lock renewed = this.lockService.renewLock(lock, 0);
        assertNotNull(renewed);

        assertEquals(LockAction.FAILED, renewed.getAction());
        assertFalse(renewed.getSuccess());
        assertEquals("junit", renewed.getNamespace());
        assertEquals(name, renewed.getLockName());
        assertNull(renewed.getOwner());
        assertNull(renewed.getInstanceId());
        assertNull(renewed.getLeaseDuration());
        assertNull(renewed.getExpiry());
    }

    @Test
    public void releaseLockNotExistsTest() {
        // If we try to release a lock that does not exist (by generating a random lock
        // name), we should get back null.
        String name = UUID.randomUUID().toString();
        Lock lock = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", 10L, this.getNow() + 10);
        Lock released = this.lockService.releaseLock(lock, 0);
        assertNotNull(released);

        assertEquals(LockAction.SUCCESS, released.getAction());
        assertTrue(released.getSuccess());
        assertEquals("junit", released.getNamespace());
        assertEquals(name, released.getLockName());
        assertNull(released.getOwner());
        assertNull(released.getInstanceId());
        assertNull(released.getLeaseDuration());
        assertNull(released.getExpiry());
    }

}
