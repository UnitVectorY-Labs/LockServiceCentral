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
package com.unitvectory.lockservicecentral.datamodel.firestore.repository;

import org.springframework.stereotype.Service;

import com.unitvectory.lockservicecentral.datamodel.model.Lock;
import com.unitvectory.lockservicecentral.datamodel.repository.LockRepository;

/**
 * The data model config for GCP Firestore
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Service
public class FirestoreLockRepository implements LockRepository {

    @Override
    public Lock acquireLock(Lock lock) {
        // TODO: Implement
        lock.setSuccess(true);
        return lock;
    }

    @Override
    public Lock renewLock(Lock lock) {
        // TODO: Implement
        lock.setSuccess(true);
        return lock;
    }

    @Override
    public Lock releaseLock(Lock lock) {
        // TODO: Implement
        lock.setSuccess(true);
        return lock;
    }

}
