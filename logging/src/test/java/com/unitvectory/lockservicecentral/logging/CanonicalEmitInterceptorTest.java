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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for CanonicalEmitInterceptor.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
class CanonicalEmitInterceptorTest {

    private CanonicalEmitInterceptor interceptor;
    private ObjectProvider<CanonicalLogContext> contextProvider;
    private CanonicalLogger canonicalLogger;
    private ObjectMapper objectMapper;
    private CanonicalLogContext context;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        contextProvider = mock(ObjectProvider.class);
        canonicalLogger = mock(CanonicalLogger.class);
        objectMapper = new ObjectMapper();
        context = new CanonicalLogContext();
        
        when(contextProvider.getObject()).thenReturn(context);
        
        interceptor = new CanonicalEmitInterceptor(contextProvider, canonicalLogger, objectMapper);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void testEmitsExactlyOneLine() throws Exception {
        when(response.getStatus()).thenReturn(200);
        
        // Simulate request processing
        context.put("service", "test-service");
        context.put("kind", "http");
        
        interceptor.afterCompletion(request, response, null, null);
        
        // Verify exactly one log line was emitted
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger, times(1)).info(logCaptor.capture());
        
        // Verify it's valid JSON
        String logLine = logCaptor.getValue();
        assertDoesNotThrow(() -> objectMapper.readValue(logLine, Map.class));
    }

    @Test
    void testExactlyOnceEmission() throws Exception {
        when(response.getStatus()).thenReturn(200);
        
        // First call should emit
        interceptor.afterCompletion(request, response, null, null);
        verify(canonicalLogger, times(1)).info(anyString());
        
        // Second call should not emit
        interceptor.afterCompletion(request, response, null, null);
        verify(canonicalLogger, times(1)).info(anyString()); // Still only 1 call
    }

    @Test
    void testSuccessOutcome() throws Exception {
        when(response.getStatus()).thenReturn(200);
        
        interceptor.afterCompletion(request, response, null, null);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        assertTrue(logLine.contains("\"outcome\":\"success\""));
        assertTrue(logLine.contains("\"http_status_code\":200"));
    }

    @Test
    void testRejectedOutcome() throws Exception {
        when(response.getStatus()).thenReturn(423); // HTTP 423 Locked
        
        interceptor.afterCompletion(request, response, null, null);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        assertTrue(logLine.contains("\"outcome\":\"rejected\""));
        assertTrue(logLine.contains("\"http_status_code\":423"));
    }

    @Test
    void testFailureOutcome() throws Exception {
        when(response.getStatus()).thenReturn(500);
        
        interceptor.afterCompletion(request, response, null, null);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        assertTrue(logLine.contains("\"outcome\":\"failure\""));
    }

    @Test
    void testExceptionAddsExceptionField() throws Exception {
        when(response.getStatus()).thenReturn(500);
        RuntimeException ex = new RuntimeException("Test error");
        
        interceptor.afterCompletion(request, response, null, ex);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        assertTrue(logLine.contains("\"outcome\":\"failure\""));
        assertTrue(logLine.contains("\"exception\":"));
        assertTrue(logLine.contains("RuntimeException"));
    }

    @Test
    void testFallbackOnSerializationFailure() throws Exception {
        // Create a mock ObjectMapper that throws on serialization
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});
        
        CanonicalEmitInterceptor failingInterceptor = new CanonicalEmitInterceptor(contextProvider, canonicalLogger, failingMapper);
        when(response.getStatus()).thenReturn(200);
        
        // Create a fresh context for this test
        CanonicalLogContext freshContext = new CanonicalLogContext();
        when(contextProvider.getObject()).thenReturn(freshContext);
        
        // Should not throw, and should emit fallback
        assertDoesNotThrow(() -> failingInterceptor.afterCompletion(request, response, null, null));
        
        // Should log a warning when the fallback serialization also fails
        verify(canonicalLogger, atLeastOnce()).warn(anyString());
    }

    @Test
    void testDurationMsCalculated() throws Exception {
        when(response.getStatus()).thenReturn(200);
        
        // Add a small delay to ensure duration > 0
        Thread.sleep(5);
        
        interceptor.afterCompletion(request, response, null, null);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        assertTrue(logLine.contains("\"duration_ms\":"));
        assertTrue(logLine.contains("\"ts\":"));
    }

    @Test
    void testFlatJsonOutput() throws Exception {
        when(response.getStatus()).thenReturn(200);
        
        context.put("service", "test-service");
        context.put("http_method", "GET");
        context.put("http_target", "/test");
        
        interceptor.afterCompletion(request, response, null, null);
        
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(canonicalLogger).info(logCaptor.capture());
        
        String logLine = logCaptor.getValue();
        
        // Parse JSON and verify it's flat (no nested objects)
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(logLine, LinkedHashMap.class);
        
        for (Object value : parsed.values()) {
            assertFalse(value instanceof Map, "Should not contain nested maps");
            assertFalse(value instanceof java.util.List, "Should not contain lists");
        }
    }
}
