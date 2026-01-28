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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CanonicalLogContext.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
class CanonicalLogContextTest {

    private CanonicalLogContext context;

    @BeforeEach
    void setUp() {
        context = new CanonicalLogContext();
    }

    @Test
    void testStartInstantCaptured() {
        assertNotNull(context.getStartInstant());
        assertTrue(context.getStartInstant().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void testPutValidSnakeCaseKeys() {
        context.put("simple_key", "value");
        context.put("another_key_123", "value2");
        context.put("a", "single");
        context.put("key123", "with_numbers");

        Map<String, Object> snapshot = context.snapshot();
        assertEquals("value", snapshot.get("simple_key"));
        assertEquals("value2", snapshot.get("another_key_123"));
        assertEquals("single", snapshot.get("a"));
        assertEquals("with_numbers", snapshot.get("key123"));
    }

    @Test
    void testPutRejectsInvalidKeys() {
        // These should be silently rejected
        context.put("CamelCase", "value");
        context.put("kebab-case", "value");
        context.put("dot.notation", "value");
        context.put("UPPERCASE", "value");
        context.put("_leading_underscore", "value");
        context.put("trailing_underscore_", "value");
        context.put("123_starts_with_number", "value");
        context.put(null, "value");
        context.put("", "value");

        Map<String, Object> snapshot = context.snapshot();
        assertTrue(snapshot.isEmpty(), "Invalid keys should be rejected");
    }

    @Test
    void testPutIgnoresNullValues() {
        context.put("valid_key", null);

        Map<String, Object> snapshot = context.snapshot();
        assertFalse(snapshot.containsKey("valid_key"));
    }

    @Test
    void testPutAcceptsValidValueTypes() {
        context.put("string_val", "hello");
        context.put("int_val", 42);
        context.put("long_val", 123456789L);
        context.put("double_val", 3.14);
        context.put("boolean_val", true);
        context.put("instant_val", Instant.parse("2026-01-20T12:00:00Z"));
        context.put("uuid_val", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        context.put("enum_val", TestEnum.SUCCESS);

        Map<String, Object> snapshot = context.snapshot();
        assertEquals("hello", snapshot.get("string_val"));
        assertEquals(42, snapshot.get("int_val"));
        assertEquals(123456789L, snapshot.get("long_val"));
        assertEquals(3.14, snapshot.get("double_val"));
        assertEquals(true, snapshot.get("boolean_val"));
        assertEquals("2026-01-20T12:00:00Z", snapshot.get("instant_val"));
        assertEquals("550e8400-e29b-41d4-a716-446655440000", snapshot.get("uuid_val"));
        assertEquals("SUCCESS", snapshot.get("enum_val"));
    }

    @Test
    void testPutRejectsInvalidValueTypes() {
        context.put("map_val", Map.of("key", "value"));
        context.put("object_val", new Object());
        context.put("list_val", java.util.List.of("a", "b"));

        Map<String, Object> snapshot = context.snapshot();
        assertFalse(snapshot.containsKey("map_val"));
        assertFalse(snapshot.containsKey("object_val"));
        assertFalse(snapshot.containsKey("list_val"));
    }

    @Test
    void testSnapshotReturnsDefensiveCopy() {
        context.put("key1", "value1");

        Map<String, Object> snapshot1 = context.snapshot();
        context.put("key2", "value2");

        Map<String, Object> snapshot2 = context.snapshot();

        assertEquals(1, snapshot1.size());
        assertEquals(2, snapshot2.size());
        assertNotSame(snapshot1, snapshot2);
    }

    @Test
    void testMarkEmittedIfFirstExactlyOnce() {
        assertFalse(context.isEmitted());

        assertTrue(context.markEmittedIfFirst());
        assertTrue(context.isEmitted());

        assertFalse(context.markEmittedIfFirst());
        assertTrue(context.isEmitted());
    }

    @Test
    void testTruncateString() {
        String longString = "a".repeat(600);
        String truncated = CanonicalLogContext.truncateErrorMessage(longString);

        assertEquals(503, truncated.length()); // 500 + "..."
        assertTrue(truncated.endsWith("..."));
    }

    @Test
    void testTruncateUserAgent() {
        String longAgent = "Mozilla/5.0 " + "x".repeat(250);
        String truncated = CanonicalLogContext.truncateUserAgent(longAgent);

        assertEquals(203, truncated.length()); // 200 + "..."
        assertTrue(truncated.endsWith("..."));
    }

    @Test
    void testTruncateShortString() {
        String shortString = "short";
        assertEquals("short", CanonicalLogContext.truncateErrorMessage(shortString));
    }

    @Test
    void testTruncateNull() {
        assertNull(CanonicalLogContext.truncate(null, 100));
    }

    @Test
    void testExceptionToStackTrace() {
        Exception ex = new RuntimeException("Test error");
        String stackTrace = CanonicalLogContext.exceptionToStackTrace(ex);

        assertNotNull(stackTrace);
        assertTrue(stackTrace.contains("RuntimeException"));
        assertTrue(stackTrace.contains("Test error"));
    }

    @Test
    void testExceptionToStackTraceNull() {
        assertNull(CanonicalLogContext.exceptionToStackTrace(null));
    }

    enum TestEnum {
        SUCCESS, FAILURE
    }
}
