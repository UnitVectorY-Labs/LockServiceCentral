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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The validation error response
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Getter
@AllArgsConstructor
public class ValidationErrorResponse {

    /**
     * The message
     */
    private final String message;

    /**
     * The details
     */
    private final List<String> details;

    /**
     * Create a new validation error response from a handler method validation
     * @param ex the handler method validation exception
     */
    public ValidationErrorResponse(HandlerMethodValidationException ex) {
        this.message = "Validation failed";
        this.details = new ArrayList<>();

        ex.getAllValidationResults().forEach(validationResult -> {
            MethodParameter methodParameter = validationResult.getMethodParameter();
            String parameterName = methodParameter.getParameterName();
            validationResult.getResolvableErrors().forEach(error -> {
                String validationMessage = error.getDefaultMessage();
                this.details
                        .add(String.format("Parameter '%s' failed validation: %s", parameterName, validationMessage));
            });
        });
    }
}
