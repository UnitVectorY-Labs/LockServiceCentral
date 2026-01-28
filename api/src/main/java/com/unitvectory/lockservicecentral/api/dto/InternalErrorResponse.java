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

import lombok.Getter;

/**
 * The internal error response
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Getter
public class InternalErrorResponse {

    /**
     * The message
     */
    private final String message;

    /**
     * The error ID
     */
    private final String errorId;

    /**
     * Create a new internal error response
     *
     * @param errorId the error ID
     */
    public InternalErrorResponse(String errorId) {
        this.message = "Internal server error";
        this.errorId = errorId;
    }

    /**
     * Create a new internal error response
     *
     * @param errorId the error ID
     * @param message the message
     */
    public InternalErrorResponse(String errorId, String message) {
        this.message = message;
        this.errorId = errorId;
    }
}
