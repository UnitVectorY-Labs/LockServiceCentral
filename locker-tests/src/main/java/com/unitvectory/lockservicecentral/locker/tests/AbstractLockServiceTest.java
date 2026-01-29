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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockAction;
import com.unitvectory.lockservicecentral.locker.LockService;

/**
 * The set of tests the verify the behavior of a LockService.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public abstract class AbstractLockServiceTest {

    private LockService lockService;

    /**
     * Creates and returns a LockService instance for testing.
     *
     * @return a new LockService instance
     */
    protected abstract LockService createLockService();

    long getNow() {
        return System.currentTimeMillis() / 1000;
    }

    @BeforeEach
    void setUp() {
        this.lockService = createLockService();
    }

    private void assertGetLockMatches(Lock actual, String namespace, String name, String owner, String instanceId,
            Long leaseDuration, Long expiry) {
        assertNotNull(actual, "The getLock must return a value when expected");
        assertNull(actual.getAction(), "The getLock must return action as null");
        assertNull(actual.getSuccess(), "The getLock must return success as null");
        assertEquals(namespace, actual.getNamespace(), "The getLock namespace does not match expected");
        assertEquals(name, actual.getLockName(), "The getLock name does not match expected");
        assertEquals(owner, actual.getOwner(), "The getLock owner does not match expected");
        assertEquals(instanceId, actual.getInstanceId(), "The getLock instanceId does not match expected");
        assertEquals(leaseDuration, actual.getLeaseDuration(), "The leaseDuration does not match expected");
        assertEquals(expiry, actual.getExpiry(), "The getLock expiry does not match expected");
    }

    /**
     * Asserts that an acquire lock operation has failed.
     *
     * @param requested the requested lock
     * @param actual    the actual lock returned
     * @param now       the current time in epoch seconds
     */
    public void assertAcquireLockFailed(Lock requested, Lock actual, long now) {
        // This is just testing to make sure the test is valid
        assertNotNull(requested,
                "Invalid test, assertAcquireLockFailed must be provided with non-null requested lock");
        assertEquals(LockAction.ACQUIRE, requested.getAction(),
                "Invalid test, assertAcquireLockFailed, not passed LockAction.ACQUIRE");
        assertNull(requested.getSuccess(), "Invalid test, assertAcquireLockFailed, not passed null success");
        assertNotNull(requested.getNamespace(), "Invalid test, assertAcquireLockFailed, not passed namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertAcquireLockFailed, not passed lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertAcquireLockFailed, not passed owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertAcquireLockFailed, not passed instanceId");
        assertNotNull(requested.getLeaseDuration(), "Invalid test, assertAcquireLockFailed, not passed leaseDuration");
        assertNotNull(requested.getExpiry(), "Invalid test, assertAcquireLockFailed, not passed expiry");
        assertEquals(requested.getLeaseDuration() + now, requested.getExpiry(),
                "Invalid test, assertAcquireLockFailed, leaseDuration != expiry + now");

        // Now actually test the response
        assertNotNull(actual, "acquireLock must return a non-null lock");

        assertEquals(LockAction.FAILED, actual.getAction(),
                "Failed acquireLock must return a lock with action FAILED");
        assertFalse(actual.getSuccess(), "Failed acquireLock must return a lock with success false");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertNull(actual.getOwner(), "Owner must be null");
        assertNull(actual.getInstanceId(), "InstanceId must be null");
        assertNull(actual.getLeaseDuration(), "LeaseDuration must be null");
        assertNull(actual.getExpiry(), "Expiry must be null");
    }

    private void assertAcquireLockSuccess(Lock requested, Lock actual, long now) {
        // This is just testing to make sure the test is valid
        assertNotNull(requested,
                "Invalid test, assertAcquireLockSuccess must be provided with non-null requested lock");
        assertEquals(LockAction.ACQUIRE, requested.getAction(),
                "Invalid test, assertAcquireLockSuccess, not passed LockAction.ACQUIRE");
        assertNull(requested.getSuccess(), "Invalid test, assertAcquireLockSuccess, not passed null success");
        assertNotNull(requested.getNamespace(), "Invalid test, assertAcquireLockSuccess, not passed namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertAcquireLockSuccess, not passed lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertAcquireLockSuccess, not passed owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertAcquireLockSuccess, not passed instanceId");
        assertNotNull(requested.getLeaseDuration(), "Invalid test, assertAcquireLockSuccess, not passed leaseDuration");
        assertNotNull(requested.getExpiry(), "Invalid test, assertAcquireLockSuccess, not passed expiry");
        assertEquals(requested.getLeaseDuration() + now, requested.getExpiry(),
                "Invalid test, assertAcquireLockSuccess, leaseDuration != expiry + now");

        // Now actually test the response
        assertNotNull(actual, "acquireLock must return a non-null lock");

        assertEquals(LockAction.SUCCESS, actual.getAction(),
                "Successful acquireLock must return a lock with action SUCCESS");
        assertTrue(actual.getSuccess(), "Successful acquireLock must return a lock with success true");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertEquals(requested.getOwner(), actual.getOwner(), "Owner must match");
        assertEquals(requested.getInstanceId(), actual.getInstanceId(), "InstanceId must match");
        assertEquals(requested.getLeaseDuration(), actual.getLeaseDuration(), "LeaseDuration must match");
        assertEquals(requested.getExpiry(), actual.getExpiry(), "Expiry must match");
    }

    private void assertRenewLockFailed(Lock requested, Lock actual, long now) {
        // Validate the requested Lock object
        assertNotNull(requested,
                "Invalid test, assertRenewLockFailed must be provided with a non-null requested lock");
        assertEquals(LockAction.RENEW, requested.getAction(),
                "Invalid test, assertRenewLockFailed must be passed LockAction.RENEW");
        assertNull(requested.getSuccess(),
                "Invalid test, assertRenewLockFailed must be passed a null success field");
        assertNotNull(requested.getNamespace(), "Invalid test, assertRenewLockFailed must be passed a namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertRenewLockFailed must be passed a lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertRenewLockFailed must be passed an owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertRenewLockFailed must be passed an instanceId");
        assertNotNull(requested.getLeaseDuration(),
                "Invalid test, assertRenewLockFailed must be passed a leaseDuration");
        assertNull(requested.getExpiry(),
                "Invalid test, assertRenewLockFailed must not pass an expiry, it is calculated based on the existing lock.");

        // Validate the actual Lock object returned by renewLock
        assertNotNull(actual, "renewLock must return a non-null lock");

        assertEquals(LockAction.FAILED, actual.getAction(),
                "Failed renewLock must return a lock with action FAILED");
        assertFalse(actual.getSuccess(),
                "Failed renewLock must return a lock with success set to false");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertNull(actual.getOwner(), "Owner must be null");
        assertNull(actual.getInstanceId(), "InstanceId must be null");
        assertNull(actual.getLeaseDuration(), "LeaseDuration must be null");
        assertNull(actual.getExpiry(), "Expiry must be null");
    }

    private void assertRenewLockSuccess(Lock requested, Lock actual, long now, Long previousLockLeaseDuration,
            Long previousLockExpiration) {
        // Validate the requested Lock object
        assertNotNull(requested,
                "Invalid test, assertRenewLockSuccess must be provided with a non-null requested lock");
        assertEquals(LockAction.RENEW, requested.getAction(),
                "Invalid test, assertRenewLockSuccess must be passed LockAction.RENEW");
        assertNull(requested.getSuccess(),
                "Invalid test, assertRenewLockSuccess must be passed a null success field");
        assertNotNull(requested.getNamespace(), "Invalid test, assertRenewLockSuccess must be passed a namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertRenewLockSuccess must be passed a lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertRenewLockSuccess must be passed an owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertRenewLockSuccess must be passed an instanceId");
        assertNotNull(requested.getLeaseDuration(),
                "Invalid test, assertRenewLockSuccess must be passed a leaseDuration");
        assertNull(requested.getExpiry(),
                "Invalid test, assertRenewLockSuccess must not pass an expiry, it is calculated based on the existing lock.");

        // Validate the actual Lock object returned by renewLock
        assertNotNull(actual, "renewLock must return a non-null lock");

        assertEquals(LockAction.SUCCESS, actual.getAction(),
                "Successful renewLock must return a lock with action SUCCESS");
        assertTrue(actual.getSuccess(),
                "Successful renewLock must return a lock with success set to true");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertEquals(requested.getOwner(), actual.getOwner(), "Owner must match");
        assertEquals(requested.getInstanceId(), actual.getInstanceId(), "InstanceId must match");
        if (previousLockLeaseDuration != null) {
            assertEquals(previousLockLeaseDuration + requested.getLeaseDuration(), actual.getLeaseDuration(),
                    "LeaseDuration must match the requested leaseDuration");
        }
        if (previousLockExpiration != null) {
            assertEquals(previousLockExpiration + requested.getLeaseDuration(), actual.getExpiry(),
                    "Expiry must match the requested expiry");
        }
    }

    private void assertReleaseLockFailed(Lock requested, Lock actual) {
        // Validate the requested Lock object
        assertNotNull(requested,
                "Invalid test, assertReleaseLockFailed must be provided with a non-null requested lock");
        assertEquals(LockAction.RELEASE, requested.getAction(),
                "Invalid test, assertReleaseLockFailed must be passed LockAction.RELEASE");
        assertNull(requested.getSuccess(),
                "Invalid test, assertReleaseLockFailed must be passed a null success field");
        assertNotNull(requested.getNamespace(), "Invalid test, assertReleaseLockFailed must be passed a namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertReleaseLockFailed must be passed a lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertReleaseLockFailed must be passed an owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertReleaseLockFailed must be passed an instanceId");
        // For release actions, leaseDuration and expiry are null
        assertNull(requested.getLeaseDuration(),
                "Invalid test, assertReleaseLockFailed must have leaseDuration as null");
        assertNull(requested.getExpiry(),
                "Invalid test, assertReleaseLockFailed must have expiry as null");

        // Validate the actual Lock object returned by releaseLock
        assertNotNull(actual, "releaseLock must return a non-null lock");

        assertEquals(LockAction.FAILED, actual.getAction(),
                "Failed releaseLock must return a lock with action FAILED");
        assertFalse(actual.getSuccess(),
                "Failed releaseLock must return a lock with success set to false");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertNull(actual.getOwner(), "After release, owner should be null");
        assertNull(actual.getInstanceId(), "After release, instanceId should be null");
        assertNull(actual.getLeaseDuration(), "After release, leaseDuration should be null");
        assertNull(actual.getExpiry(), "After release, expiry should be null");
    }

    private void assertReleaseLockSuccess(Lock requested, Lock actual) {
        // Validate the requested Lock object
        assertNotNull(requested,
                "Invalid test, assertReleaseLockSuccess must be provided with a non-null requested lock");
        assertEquals(LockAction.RELEASE, requested.getAction(),
                "Invalid test, assertReleaseLockSuccess must be passed LockAction.RELEASE");
        assertNull(requested.getSuccess(),
                "Invalid test, assertReleaseLockSuccess must be passed a null success field");
        assertNotNull(requested.getNamespace(), "Invalid test, assertReleaseLockSuccess must be passed a namespace");
        assertNotNull(requested.getLockName(), "Invalid test, assertReleaseLockSuccess must be passed a lockName");
        assertNotNull(requested.getOwner(), "Invalid test, assertReleaseLockSuccess must be passed an owner");
        assertNotNull(requested.getInstanceId(), "Invalid test, assertReleaseLockSuccess must be passed an instanceId");
        // For release actions, leaseDuration and expiry are null
        assertNull(requested.getLeaseDuration(),
                "Invalid test, assertReleaseLockSuccess must have leaseDuration as null");
        assertNull(requested.getExpiry(),
                "Invalid test, assertReleaseLockSuccess must have expiry as null");

        // Validate the actual Lock object returned by releaseLock
        assertNotNull(actual, "releaseLock must return a non-null lock");

        assertEquals(LockAction.SUCCESS, actual.getAction(),
                "Successful releaseLock must return a lock with action SUCCESS");
        assertTrue(actual.getSuccess(),
                "Successful releaseLock must return a lock with success set to true");
        assertEquals(requested.getNamespace(), actual.getNamespace(), "Namespace must match");
        assertEquals(requested.getLockName(), actual.getLockName(), "LockName must match");
        assertNull(actual.getOwner(), "After release, owner should be null");
        assertNull(actual.getInstanceId(), "After release, instanceId should be null");
        assertNull(actual.getLeaseDuration(), "After release, leaseDuration should be null");
        assertNull(actual.getExpiry(), "After release, expiry should be null");
    }

    /**
     * Tests that getLock throws NullPointerException for null namespace.
     */
    @Test
    public void getLockNullNamespaceTest() {
        assertThrows(NullPointerException.class, () -> {
            this.lockService.getLock(null, "lockName");
        });
    }

    /**
     * Tests that getLock throws NullPointerException for null lockName.
     */
    @Test
    public void getLockNullLockNameTest() {
        assertThrows(NullPointerException.class, () -> {
            this.lockService.getLock("namespace", null);
        });
    }

    /**
     * Tests that acquireLock throws NullPointerException for null lock.
     */
    @Test
    public void acquireLockNullLockTest() {
        assertThrows(NullPointerException.class, () -> {
            this.lockService.acquireLock(null, this.getNow());
        });
    }

    /**
     * Tests that renewLock throws NullPointerException for null lock.
     */
    @Test
    public void renewLockNullLockTest() {
        assertThrows(NullPointerException.class, () -> {
            this.lockService.renewLock(null, this.getNow());
        });
    }

    /**
     * Tests that releaseLock throws NullPointerException for null lock.
     */
    @Test
    public void releaseLockNullLockTest() {
        assertThrows(NullPointerException.class, () -> {
            this.lockService.releaseLock(null, this.getNow());
        });
    }

    /**
     * Tests that getLock returns null for a non-existent lock.
     */
    @Test
    public void getLockNotFoundTest() {
        String name = UUID.randomUUID().toString();
        Lock lock = this.lockService.getLock("junit", name);
        assertNull(lock, "Non-existent lock should return null");
    }

    /**
     * Tests that getLock returns the lock when it exists.
     */
    @Test
    public void getLockExistsTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to get the lock we made
        Lock found = this.lockService.getLock("junit", name);
        assertGetLockMatches(found, "junit", name, "owner", "instance", 60L, now + 60L);
    }

    /**
     * Tests sequential lock operations: acquire, renew, and release.
     */
    @Test
    public void sequentialOperationsTest() {
        String name = UUID.randomUUID().toString();
        long now = getNow();

        // Acquire Lock
        Lock acquireLock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = lockService.acquireLock(acquireLock, now);
        assertAcquireLockSuccess(acquireLock, acquired, now);

        // Renew Lock
        Lock renewLock = new Lock(LockAction.RENEW, null, "junit", name, "owner", "instance", 60L, null);
        Lock renewed = lockService.renewLock(renewLock, now + 30);
        assertRenewLockSuccess(renewLock, renewed, now + 30, 60L, now + 60);

        // Release Lock
        Lock releaseLock = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", null, null);
        Lock released = lockService.releaseLock(releaseLock, now + 60);
        assertReleaseLockSuccess(releaseLock, released);

        // Verify Lock is Released
        Lock finalLock = lockService.getLock("junit", name);
        assertNull(finalLock, "Released lock should be deleted and return null");
    }

    /**
     * Tests acquiring a lock with a past timestamp when another lock exists.
     */
    @Test
    public void acquireLockPastEdgeCaseTest() {
        String name = UUID.randomUUID().toString();
        long now = getNow();

        // Acquire Lock
        Lock acquireLock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = lockService.acquireLock(acquireLock, now);
        assertAcquireLockSuccess(acquireLock, acquired, now);

        // Acquire Lock in the past
        long past = now - 1;
        Lock acquireLockPast = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner2", "instance2", 60L, past + 60);
        Lock acquirePast = lockService.acquireLock(acquireLockPast, past);
        assertAcquireLockFailed(acquireLockPast, acquirePast, past);
    }

    /**
     * Tests acquiring a lock at the same timestamp as an existing lock.
     */
    @Test
    public void acquireLockSameTimestampTest() {
        String name = UUID.randomUUID().toString();
        long now = getNow();

        // Acquire Lock
        Lock acquireLock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = lockService.acquireLock(acquireLock, now);
        assertAcquireLockSuccess(acquireLock, acquired, now);

        // Acquire Lock at the same time
        Lock acquireLockSameTime = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner2", "instance2", 60L,
                now + 60);
        Lock acquiredSameTime = lockService.acquireLock(acquireLockSameTime, now);
        assertAcquireLockFailed(acquireLockSameTime, acquiredSameTime, now);
    }

    /**
     * Tests acquiring multiple independent locks.
     */
    @Test
    public void multipleLocksTest() {
        String name1 = UUID.randomUUID().toString();
        String name2 = UUID.randomUUID().toString();
        long now = getNow();

        Lock lock1 = new Lock(LockAction.ACQUIRE, null, "junit", name1, "owner1", "instance1", 60L, now + 60);
        Lock lock2 = new Lock(LockAction.ACQUIRE, null, "junit", name2, "owner2", "instance2", 60L, now + 60);

        Lock acquired1 = lockService.acquireLock(lock1, now);
        Lock acquired2 = lockService.acquireLock(lock2, now);

        assertAcquireLockSuccess(lock1, acquired1, now);
        assertAcquireLockSuccess(lock2, acquired2, now);

        // Now get both of these locks and assert they belong to the correct owner and
        // instance
        Lock found1 = lockService.getLock("junit", name1);
        Lock found2 = lockService.getLock("junit", name2);

        assertGetLockMatches(found1, "junit", name1, "owner1", "instance1", 60L, now + 60);
        assertGetLockMatches(found2, "junit", name2, "owner2", "instance2", 60L, now + 60);
    }

    /**
     * Tests acquiring a new lock.
     */
    @Test
    public void acquireLockNewTest() {
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 10L, now + 10);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);
    }

    /**
     * Tests that acquiring an existing active lock fails.
     */
    @Test
    public void acquireLockExistingFailedTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to acquire the lock we made
        Lock lock2 = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner2", "instance2", 60L, now + 60);
        Lock acquired2 = this.lockService.acquireLock(lock2, now);
        assertAcquireLockFailed(lock2, acquired2, now);
    }

    /**
     * Tests acquiring an expired lock succeeds.
     */
    @Test
    public void acquireLockExistingExpiredTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow() - 100;
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "other", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to acquire the lock we made
        now = this.getNow();
        Lock lock2 = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired2 = this.lockService.acquireLock(lock2, now);
        assertAcquireLockSuccess(lock2, acquired2, now);
    }

    /**
     * Tests reacquiring an existing lock by the same owner and instance succeeds.
     */
    @Test
    public void acquireLockExistingSuccessTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to acquire the lock we made
        now += 10;
        Lock lock2 = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired2 = this.lockService.acquireLock(lock2, now);
        assertAcquireLockSuccess(lock2, acquired2, now);
    }

    /**
     * Tests that renewing a non-existent lock fails.
     */
    @Test
    public void renewLockNotExistsTest() {
        // If we try to renew a lock that does not exist (by generating a random lock
        // name), we should get back null.
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.RENEW, null, "junit", name, "owner", "instance", 10L, null);
        Lock renewed = this.lockService.renewLock(lock, now);
        assertRenewLockFailed(lock, renewed, now);
    }

    /**
     * Tests that renewing a lock with mismatched owner fails.
     */
    @Test
    public void renewLockNotMatchFailedTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to renew the lock we made
        now += 1;
        Lock lock2 = new Lock(LockAction.RENEW, null, "junit", name, "owner2", "instance", 60L, null);
        Lock renewed = this.lockService.renewLock(lock2, now);
        assertRenewLockFailed(lock2, renewed, now);
    }

    /**
     * Tests that renewing an existing lock succeeds.
     */
    @Test
    public void renewLockSuccessTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to renew the lock we made
        now += 10;
        Lock lock2 = new Lock(LockAction.RENEW, null, "junit", name, "owner", "instance", 60L, null);
        Lock renewed = this.lockService.renewLock(lock2, now);
        assertRenewLockSuccess(lock2, renewed, now, 60L, now - 10 + 60);
    }

    /**
     * Tests that renewing an expired lock fails.
     */
    @Test
    public void renewLockExpiredFailed() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow() - 100;
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to renew the lock we made
        now = this.getNow();
        Lock lock2 = new Lock(LockAction.RENEW, null, "junit", name, "owner", "instance", 60L, null);
        Lock renewed = this.lockService.renewLock(lock2, now);
        assertRenewLockFailed(lock2, renewed, now);
    }

    /**
     * Tests that releasing a non-existent lock succeeds.
     */
    @Test
    public void releaseLockNotExistsTest() {
        // If we try to release a lock that does not exist (by generating a random lock
        // name), we should get back null.
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", null, null);
        Lock released = this.lockService.releaseLock(lock, now);
        assertReleaseLockSuccess(lock, released);
    }

    /**
     * Tests that releasing a lock with mismatched owner fails.
     */
    @Test
    public void releaseLockNotMatchFailedTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to release the lock we made
        now += 1;
        Lock lock2 = new Lock(LockAction.RELEASE, null, "junit", name, "owner2", "instance", null, null);
        Lock released = this.lockService.releaseLock(lock2, now);
        assertReleaseLockFailed(lock2, released);
    }

    /**
     * Tests that releasing an expired lock succeeds.
     */
    @Test
    public void releaseLockExpiredTest() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "other", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to release the lock we made
        now += 100;
        Lock lock2 = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", null, null);
        Lock released = this.lockService.releaseLock(lock2, now);
        assertReleaseLockSuccess(lock2, released);
    }

    /**
     * Tests that releasing an already released lock succeeds.
     */
    @Test
    public void releaseAlreadyReleasedLockTest() {
        String name = UUID.randomUUID().toString();
        long now = getNow();

        // Acquire Lock
        Lock acquireLock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = lockService.acquireLock(acquireLock, now);
        assertAcquireLockSuccess(acquireLock, acquired, now);

        // Release Lock
        Lock releaseLock = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", null, null);
        Lock released = lockService.releaseLock(releaseLock, now + 30);
        assertReleaseLockSuccess(releaseLock, released);

        // Attempt to Release Again
        Lock releaseAgain = lockService.releaseLock(releaseLock, now + 40);
        assertReleaseLockSuccess(releaseLock, releaseAgain);
    }

    /**
     * Tests that releasing an existing lock succeeds.
     */
    @Test
    public void releaseLockSuccess() {
        // First we need to create a lock
        String name = UUID.randomUUID().toString();
        long now = this.getNow();
        Lock lock = new Lock(LockAction.ACQUIRE, null, "junit", name, "owner", "instance", 60L, now + 60);
        Lock acquired = this.lockService.acquireLock(lock, now);
        assertAcquireLockSuccess(lock, acquired, now);

        // Now we want to release the lock we made
        now += 1;
        Lock lock2 = new Lock(LockAction.RELEASE, null, "junit", name, "owner", "instance", null, null);
        Lock released = this.lockService.releaseLock(lock2, now);
        assertReleaseLockSuccess(lock2, released);
    }
}
