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
package com.unitvectory.lockservicecentral.api.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unitvectory.lockservicecentral.datamodel.model.Lock;
import com.unitvectory.lockservicecentral.datamodel.model.LockAction;
import com.unitvectory.lockservicecentral.datamodel.repository.LockRepository;

import lombok.NonNull;

/**
 * The Lock Service
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Service
public class LockService {

    @Autowired
    private LockRepository lockRepository;

    /**
     * Get the current time.
     * 
     * @return the current time
     */
    private long getNow() {
        return Instant.now().getEpochSecond();
    }

    /**
     * Get a lock.
     * 
     * @param namespace the namespace
     * @param lockName  the lock name
     * @return the lock
     */
    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        Lock lock = new Lock();
        lock.setNamespace(namespace);
        lock.setLockName(lockName);
        lock.setAction(LockAction.GET);

        Lock activeLock = lockRepository.getLock(namespace, lockName);

        if (activeLock != null) {
            lock = activeLock;
        }

        long now = getNow();

        // Clear out the owner and instance ID
        lock.setGet(now);

        return lock;
    }

    /**
     * Acquire a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock acquireLock(@NonNull Lock lock) {
        lock.setAction(LockAction.ACQUIRE);

        // Calculate the expiry based on the current time and lease duration
        long now = getNow();
        long expiry = now + lock.getLeaseDuration();
        lock.setExpiry(expiry);

        return lockRepository.acquireLock(lock, now);
    }

    /**
     * Renew a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock renewLock(@NonNull Lock lock) {
        lock.setAction(LockAction.RENEW);

        // Calculate the expiry based on the current time and lease duration
        long now = getNow();
        long expiry = now + lock.getLeaseDuration();
        lock.setExpiry(expiry);

        return lockRepository.renewLock(lock, now);
    }

    /**
     * Release a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock releaseLock(@NonNull Lock lock) {
        lock.setAction(LockAction.RELEASE);

        // The current time is used to release the lock
        long now = getNow();

        return lockRepository.releaseLock(lock, now);
    }
}
