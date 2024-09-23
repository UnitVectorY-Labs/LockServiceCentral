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
package com.unitvectory.lockservicecentral.locker.memory;

import java.util.concurrent.ConcurrentHashMap;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The Memory based LockService.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Slf4j
public class MemoryLockService implements LockService {

    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    private void save(Lock lock) {
        String key = generateKey(lock.getNamespace(), lock.getLockName());
        Lock copy = lock.copy();
        copy.setAction(null);
        copy.setSuccess(null);
        locks.put(key, copy);
    }

    private Lock get(String key) {
        Lock lock = locks.get(key);
        if (lock != null) {
            return lock.copy();
        }

        return null;
    }

    /**
     * Get a lock by namespace and lock name.
     * 
     * @param namespace The lock namespace
     * @param lockName  The lock name
     * @return The lock instance or null if it does not exist
     */
    @Override
    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        String key = generateKey(namespace, lockName);
        Lock lock = locks.get(key);
        if (lock == null) {
            return null;
        } else {
            return lock.copy();
        }
    }

    /**
     * Acquire the lock if it's available.
     * 
     * @param lock The lock request
     * @param now  The current timestamp
     * @return The lock response indicating success or failure
     */
    @Override
    public Lock acquireLock(@NonNull Lock lock, long now) {
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        // If a lock exists, check if it's still valid
        Lock existingLock = get(key);
        if (existingLock != null) {
            // If the lock is active and belongs to the same instance, replace it
            if (existingLock.isMatch(lock) || existingLock.isExpired(now)) {
                this.save(lock);
                lock.setSuccess();
                log.info("Lock replaced: {}", lock);
            } else {
                // If there's a conflict, return failure
                log.warn("Lock conflict: {}", lock);
                lock.setFailed();
            }
        } else {
            // If no lock exists, acquire it
            this.save(lock);
            lock.setSuccess();
            log.info("Lock acquired: {}", lock);
        }

        return lock.copy();
    }

    /**
     * Renew the lock by extending the lease duration.
     * 
     * @param lock The lock request
     * @param now  The current timestamp
     * @return The renewed lock or failure if it cannot be renewed
     */
    @Override
    public Lock renewLock(@NonNull Lock lock, long now) {
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        // Check if the lock exists and is still valid
        Lock existingLock = get(key);
        if (existingLock == null || !existingLock.isActive(now) || !lock.isMatch(existingLock)) {
            log.warn("Cannot renew lock: {}", lock);
            lock.setFailed();
            return lock;
        }

        // Successfully renew the lock
        long newLeaseDuration = existingLock.getLeaseDuration() + lock.getLeaseDuration();
        long newExpiry = existingLock.getExpiry() + lock.getLeaseDuration();

        existingLock.setLeaseDuration(newLeaseDuration);
        existingLock.setExpiry(newExpiry);

        lock = existingLock.copy();
        lock.setSuccess();

        this.save(lock);

        log.info("Lock renewed: {}", lock);
        return lock;
    }

    /**
     * Release the lock, making it available for others.
     * 
     * @param lock The lock request
     * @param now  The current timestamp
     * @return The released lock or failure if it cannot be released
     */
    @Override
    public Lock releaseLock(@NonNull Lock lock, long now) {
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        // Check if the lock exists and matches the request
        Lock existingLock = get(key);
        if (existingLock == null || existingLock.isExpired(now)) {
            // Lock is expired, so it is already released
            lock.setCleared();
            log.info("Lock released: {}", lock);
        } else if (!lock.isMatch(existingLock)) {
            log.warn("Cannot release lock: {}", lock);
            lock.setFailed();
            return lock;
        }

        // Release the lock
        locks.remove(key);
        lock.setCleared();
        log.info("Lock released: {}", lock);
        return lock.copy();
    }

    /**
     * Generate a unique key for the lock based on namespace and lock name.
     * 
     * @param namespace The lock namespace
     * @param lockName  The lock name
     * @return A unique key for the lock
     */
    private String generateKey(String namespace, String lockName) {
        return namespace + ":" + lockName;
    }
}