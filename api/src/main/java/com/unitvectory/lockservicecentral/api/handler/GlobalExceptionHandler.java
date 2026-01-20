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
package com.unitvectory.lockservicecentral.api.handler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.unitvectory.consistgen.uuid.UuidGenerator;
import com.unitvectory.jsonschema4springboot.ValidateJsonSchemaException;
import com.unitvectory.jsonschema4springboot.ValidateJsonSchemaFailedResponse;
import com.unitvectory.lockservicecentral.api.dto.InternalErrorResponse;
import com.unitvectory.lockservicecentral.api.dto.ValidationErrorResponse;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import lombok.extern.slf4j.Slf4j;

/**
 * The global exception handler
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

    @ExceptionHandler(ValidateJsonSchemaException.class)
    public ResponseEntity<ValidateJsonSchemaFailedResponse> onValidateJsonSchemaException(
            ValidateJsonSchemaException ex) {
        enrichCanonicalContextForValidationError(ex);
        return ResponseEntity.badRequest().body(new ValidateJsonSchemaFailedResponse(ex));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationErrorResponse> onHandlerMethodValidationException(
            HandlerMethodValidationException ex) {
        enrichCanonicalContextForValidationError(ex);
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(ex));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<InternalErrorResponse> onHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        enrichCanonicalContextForError(ex);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new InternalErrorResponse(this.uuidGenerator.generateUuid(), "Method not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<InternalErrorResponse> onNoResourceFoundException(NoResourceFoundException ex) {
        enrichCanonicalContextForError(ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new InternalErrorResponse(this.uuidGenerator.generateUuid(), "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalErrorResponse> onException(Exception ex) {
        // This will generate a unique error ID for each error
        InternalErrorResponse response = new InternalErrorResponse(this.uuidGenerator.generateUuid());

        // Enrich canonical context with exception details
        enrichCanonicalContextForException(ex, response.getErrorId());

        // Logging the error ID and the exception so they can be correlated
        log.error(response.getErrorId(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Enriches the canonical log context for validation errors.
     * 
     * @param ex the validation exception
     */
    private void enrichCanonicalContextForValidationError(Exception ex) {
        try {
            CanonicalLogContext context = canonicalLogContextProvider.getObject();
            context.put("error_type", "validation_error");
            context.put("exception", CanonicalLogContext.exceptionToStackTrace(ex));
        } catch (Exception e) {
            // Don't break error handling if logging fails
        }
    }

    /**
     * Enriches the canonical log context for general errors.
     * 
     * @param ex the exception
     */
    private void enrichCanonicalContextForError(Exception ex) {
        try {
            CanonicalLogContext context = canonicalLogContextProvider.getObject();
            context.put("exception", CanonicalLogContext.exceptionToStackTrace(ex));
        } catch (Exception e) {
            // Don't break error handling if logging fails
        }
    }

    /**
     * Enriches the canonical log context for unhandled exceptions.
     * 
     * @param ex the exception
     * @param errorId the error ID for correlation
     */
    private void enrichCanonicalContextForException(Exception ex, String errorId) {
        try {
            CanonicalLogContext context = canonicalLogContextProvider.getObject();
            context.put("error_id", errorId);
            context.put("exception", CanonicalLogContext.exceptionToStackTrace(ex));
        } catch (Exception e) {
            // Don't break error handling if logging fails
        }
    }
}
