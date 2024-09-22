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

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;

/**
 * The MemoryLockService test.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class MemoryLockServiceTest {

    private LockService lockService;

    @BeforeEach
    public void setUp() {
        lockService = new MemoryLockService();
    }

    @Test
    public void getLockTest() {
        Lock lock = this.lockService.getLock("foo", "bar");
        assertNull(lock);
    }
}
