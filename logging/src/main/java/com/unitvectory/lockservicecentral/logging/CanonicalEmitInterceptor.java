/*
 * Copyright 2026 the original author or authors.
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
package com.unitvectory.lockservicecentral.logging;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that finalizes and emits the canonical log record exactly once.
 * 
 * <p>In preHandle, captures the route template. In afterCompletion, computes final
 * baseline fields and emits the record.</p>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Component
public class CanonicalEmitInterceptor implements HandlerInterceptor {

    private final ObjectProvider<CanonicalLogContext> contextProvider;
    private final CanonicalLogger canonicalLogger;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new interceptor.
     * 
     * @param contextProvider provides the request-scoped context
     * @param canonicalLogger the canonical logger
     * @param objectMapper the Jackson object mapper
     */
    public CanonicalEmitInterceptor(
            ObjectProvider<CanonicalLogContext> contextProvider,
            CanonicalLogger canonicalLogger,
            ObjectMapper objectMapper) {
        this.contextProvider = contextProvider;
        this.canonicalLogger = canonicalLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            CanonicalLogContext context = contextProvider.getObject();
            
            // Capture route template when available
            Object routePattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (routePattern != null) {
                context.put("http_route", routePattern.toString());
            }
        } catch (Exception e) {
            // Don't break the request if logging fails
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            CanonicalLogContext context = contextProvider.getObject();
            
            // Ensure exactly-once emission
            if (!context.markEmittedIfFirst()) {
                return;
            }
            
            // Compute final baseline fields
            Instant endTime = Instant.now();
            context.put("ts", endTime);
            
            long durationMs = Duration.between(context.getStartInstant(), endTime).toMillis();
            context.put("duration_ms", durationMs);
            
            int statusCode = response.getStatus();
            context.put("http_status_code", statusCode);
            
            // Determine outcome
            String outcome = determineOutcome(statusCode, ex);
            context.put("outcome", outcome);
            
            // Add error fields if exception present
            if (ex != null) {
                context.put("exception", CanonicalLogContext.exceptionToStackTrace(ex));
            }
            
            // Emit the canonical record
            emitRecord(context);
            
        } catch (Exception e) {
            // Emit fallback record on failure
            emitFallbackRecord(e);
        }
    }

    /**
     * Determines the outcome based on status code and exception.
     * 
     * @param statusCode the HTTP status code
     * @param ex the exception, if any
     * @return the outcome string
     */
    private String determineOutcome(int statusCode, Exception ex) {
        if (ex != null) {
            return "failure";
        }
        if (statusCode >= 200 && statusCode < 300) {
            return "success";
        }
        if (statusCode == 423) { // HTTP 423 Locked - conflict
            return "rejected";
        }
        if (statusCode == 408 || statusCode == 504) { // Request Timeout or Gateway Timeout
            return "timeout";
        }
        if (statusCode >= 400) {
            return "failure";
        }
        return "success";
    }

    /**
     * Emits the canonical record as a JSON line.
     * 
     * @param context the canonical log context
     */
    private void emitRecord(CanonicalLogContext context) {
        try {
            Map<String, Object> snapshot = context.snapshot();
            String jsonLine = objectMapper.writeValueAsString(snapshot);
            canonicalLogger.info(jsonLine);
        } catch (JsonProcessingException e) {
            emitFallbackRecord(e);
        }
    }

    /**
     * Emits a minimal fallback record when normal emission fails.
     * 
     * @param cause the cause of the failure
     */
    private void emitFallbackRecord(Exception cause) {
        try {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("kind", "http");
            fallback.put("outcome", "failure");
            fallback.put("error_type", "canonical_log_emit_failure");
            fallback.put("error_message", CanonicalLogContext.truncateErrorMessage(cause.getMessage()));
            
            String jsonLine = objectMapper.writeValueAsString(fallback);
            canonicalLogger.info(jsonLine);
        } catch (Exception e) {
            // Last resort: log as plain text
            canonicalLogger.warn("Failed to emit canonical log record: " + cause.getMessage());
        }
    }
}
