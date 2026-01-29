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

/**
 * The LockService interface.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public interface LockService {

    /**
     * Get a lock.
     *
     * @param namespace the namespace
     * @param lockName  the lock name
     * @return the lock
     */
    Lock getLock(String namespace, String lockName);

    /**
     * Acquire a lock.
     *
     * @param lock the lock request
     * @param now  the current time
     * @return the lock response
     */
    Lock acquireLock(Lock lock, long now);

    /**
     * Renew a lock.
     *
     * @param lock the lock request
     * @param now  the current time
     * @return the lock response
     */
    Lock renewLock(Lock lock, long now);

    /**
     * Release a lock.
     *
     * @param lock the lock request
     * @param now  the current time
     * @return the lock response
     */
    Lock releaseLock(Lock lock, long now);
}
