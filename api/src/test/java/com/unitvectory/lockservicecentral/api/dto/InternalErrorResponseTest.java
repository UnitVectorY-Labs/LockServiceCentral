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
package com.unitvectory.lockservicecentral.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * The InternalErrorResponse tests
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class InternalErrorResponseTest {

    @Test
    public void testDefaultConstructor() {
        InternalErrorResponse response = new InternalErrorResponse();
        assertEquals("Internal server error", response.getMessage());
        assertNotNull(response.getErrorId());
    }

    @Test
    public void testParameterizedConstructor() {
        String customMessage = "Custom error message";
        InternalErrorResponse response = new InternalErrorResponse(customMessage);
        assertEquals(customMessage, response.getMessage());
        assertNotNull(response.getErrorId());
    }
}
