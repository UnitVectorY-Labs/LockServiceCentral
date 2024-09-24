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

/**
 * The Static Entropy Service used for testing
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class StaticEntropyService implements EntropyService {

    /**
     * The current UUID to be returned
     */
    private String uuid = "00000000-0000-0000-0000-000000000000";

    @Override
    public String uuid() {
        // A static entropy service always returns the same UUID
        // Only used for testing
        return this.uuid;
    }

    /**
     * Sets the current UUID for testing.
     * 
     * @param uuid the current UUID
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
