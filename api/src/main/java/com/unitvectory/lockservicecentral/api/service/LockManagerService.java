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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unitvectory.consistgen.epoch.EpochTimeProvider;
import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockAction;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import lombok.NonNull;

/**
 * The Lock Service
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Service
public class LockManagerService {

    @Autowired
    private LockService lockService;

    @Autowired
    private EpochTimeProvider epochTimeProvider;

    @Autowired
    private ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

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

        long startTime = System.currentTimeMillis();
        Lock activeLock = lockService.getLock(namespace, lockName);
        long endTime = System.currentTimeMillis();

        if (activeLock != null) {
            lock = activeLock;
        }

        long now = this.epochTimeProvider.epochTimeSeconds();

        // Clear out the owner and instance ID
        lock.setGet(now);

        // Enrich canonical context
        enrichCanonicalContext(endTime - startTime, activeLock != null ? "success" : "not_found", null);

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
        long now = this.epochTimeProvider.epochTimeSeconds();
        long expiry = now + lock.getLeaseDuration();
        lock.setExpiry(expiry);

        long startTime = System.currentTimeMillis();
        Lock result = lockService.acquireLock(lock, now);
        long endTime = System.currentTimeMillis();

        // Determine lock result
        String lockResult = Boolean.TRUE.equals(result.getSuccess()) ? "success" : "conflict";

        // Enrich canonical context
        enrichCanonicalContext(endTime - startTime, lockResult, result.getSuccess() ? expiry : null);

        return result;
    }

    /**
     * Renew a lock.
     *
     * @param lock the lock request
     * @return the lock response
     */
    public Lock renewLock(@NonNull Lock lock) {
        lock.setAction(LockAction.RENEW);

        // The current time is used to renew the lock
        long now = this.epochTimeProvider.epochTimeSeconds();

        long startTime = System.currentTimeMillis();
        Lock result = lockService.renewLock(lock, now);
        long endTime = System.currentTimeMillis();

        // Determine lock result
        String lockResult = Boolean.TRUE.equals(result.getSuccess()) ? "success" : "conflict";

        // Enrich canonical context with new expiry
        Long computedExpiry = Boolean.TRUE.equals(result.getSuccess()) ? result.getExpiry() : null;
        enrichCanonicalContext(endTime - startTime, lockResult, computedExpiry);

        return result;
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
        long now = this.epochTimeProvider.epochTimeSeconds();

        long startTime = System.currentTimeMillis();
        Lock result = lockService.releaseLock(lock, now);
        long endTime = System.currentTimeMillis();

        // Determine lock result
        String lockResult = Boolean.TRUE.equals(result.getSuccess()) ? "success" : "conflict";

        // Enrich canonical context
        enrichCanonicalContext(endTime - startTime, lockResult, null);

        return result;
    }

    /**
     * Enriches the canonical log context with service-level details.
     *
     * @param backendDurationMs time spent in backend call
     * @param lockResult the lock operation result
     * @param computedExpiry the computed expiry (for acquire/renew)
     */
    private void enrichCanonicalContext(long backendDurationMs, String lockResult, Long computedExpiry) {
        CanonicalLogContext context = canonicalLogContextProvider.getObject();

        context.put("lock_backend", lockService.getBackendName());
        context.put("backend_duration_ms", backendDurationMs);
        context.put("lock_result", lockResult);

        if (computedExpiry != null) {
            context.put("computed_expiry_epoch_sec", computedExpiry);
        }
    }
}
