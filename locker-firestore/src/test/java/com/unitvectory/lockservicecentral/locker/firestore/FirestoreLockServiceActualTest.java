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
package com.unitvectory.lockservicecentral.locker.firestore;

import org.junit.jupiter.api.Disabled;

import com.google.cloud.firestore.FirestoreOptions;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.locker.tests.AbstractLockServiceTest;

/**
 * The FirestoreLockService test.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Disabled
public class FirestoreLockServiceActualTest extends AbstractLockServiceTest {

    @Override
    protected LockService createLockService() {
        // These tests are disabled because they require interaction with an actual
        // Firestore database to run. These are only intended to be used for manual
        // local testing.
        return new FirestoreLockService(FirestoreOptions.getDefaultInstance().toBuilder()
                .build().getService(), "locks");
    }

}
