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
     * Acquire a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock acquireLock(Lock lock) {
        lock.setAction(LockAction.ACQUIRE);

        // Calculate the expiry based on the current time and lease duration
        Instant now = Instant.now();
        long expiry = now.getEpochSecond() + lock.getLeaseDuration();
        lock.setExpiry(expiry);

        return lockRepository.acquireLock(lock, now.getEpochSecond());
    }

    /**
     * Renew a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock renewLock(Lock lock) {
        lock.setAction(LockAction.RENEW);

        // Calculate the expiry based on the current time and lease duration
        Instant now = Instant.now();
        long expiry = now.getEpochSecond() + lock.getLeaseDuration();
        lock.setExpiry(expiry);

        return lockRepository.renewLock(lock, now.getEpochSecond());
    }

    /**
     * Release a lock.
     * 
     * @param lock the lock request
     * @return the lock response
     */
    public Lock releaseLock(Lock lock) {
        lock.setAction(LockAction.RELEASE);

        // Calculate the expiry based on the current time and lease duration
        Instant now = Instant.now();

        return lockRepository.releaseLock(lock, now.getEpochSecond());
    }
}
