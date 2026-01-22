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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

/**
 * Request-scoped context for building a canonical log record.
 * 
 * <p>
 * Each HTTP request gets an isolated instance. Controllers and services can add
 * fields
 * during request processing using {@link #put(String, Object)}. The final
 * record is emitted
 * exactly once at request completion.
 * </p>
 * 
 * <p>
 * Thread-safe for limited intra-request parallelism when explicitly propagated.
 * </p>
 * 
 * <p>
 * Note: This class is configured as request-scoped with a scoped proxy in
 * {@link CanonicalLoggingConfig}.
 * </p>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class CanonicalLogContext {

    private static final Logger log = LoggerFactory.getLogger(CanonicalLogContext.class);

    /**
     * Pattern for valid snake_case keys.
     */
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$");

    /**
     * Maximum length for truncating string values (like error messages).
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    /**
     * Maximum length for truncating user agent.
     */
    private static final int MAX_USER_AGENT_LENGTH = 200;

    /**
     * The underlying map storing log fields. Using synchronized wrapper for thread
     * safety.
     */
    private final Map<String, Object> fields = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Request start timestamp, captured at construction.
     */
    @Getter
    private final Instant startInstant;

    /**
     * Guard for exactly-once emission.
     */
    private final AtomicBoolean emitted = new AtomicBoolean(false);

    /**
     * Constructs a new context with the current timestamp.
     */
    public CanonicalLogContext() {
        this.startInstant = Instant.now();
    }

    /**
     * Adds a field to the canonical record.
     * 
     * <p>
     * Silently ignores null values. Validates that the key is snake_case and the
     * value
     * is a supported type. Invalid keys or values are logged as warnings but do not
     * throw.
     * </p>
     * 
     * @param key   the field name (must be snake_case)
     * @param value the field value (must be a supported type)
     */
    public void put(String key, Object value) {
        // Silently ignore null values
        if (value == null) {
            return;
        }

        // Validate snake_case key
        if (key == null || !SNAKE_CASE_PATTERN.matcher(key).matches()) {
            log.warn("Invalid canonical log key rejected (not snake_case): {}", key);
            return;
        }

        // Validate and normalize value type
        Object normalizedValue = normalizeValue(key, value);
        if (normalizedValue != null) {
            fields.put(key, normalizedValue);
        }
    }

    /**
     * Adds a file to the canonical record with the value set being the SHA-256 hash
     * of
     * the input. This avoids logging the exact value while still allowing
     * correlation.
     * 
     * @param key   the field name (must be snake_case)
     * @param value the field value to hash
     */
    public void putSHA256(String key, String value) {
        // Silently ignore null values
        if (value == null) {
            return;
        }

        // Hash the value using SHA-256
        String hashedValue = HashUtil.sha256Hex(value);

        // Put the value
        this.put(key, hashedValue);
    }

    /**
     * Normalizes a value to a supported type for flat JSON serialization.
     * 
     * @param key   the field key (for logging)
     * @param value the value to normalize
     * @return the normalized value, or null if the type is not supported
     */
    private Object normalizeValue(String key, Object value) {
        if (value instanceof String) {
            return value;
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        } else if (value instanceof Instant) {
            return ((Instant) value).toString();
        } else if (value instanceof UUID) {
            return value.toString();
        } else {
            log.warn("Invalid canonical log value type rejected for key '{}': {}", key, value.getClass().getName());
            return null;
        }
    }

    /**
     * Returns a defensive copy of the current fields.
     * 
     * @return a snapshot of the fields
     */
    public Map<String, Object> snapshot() {
        synchronized (fields) {
            return new LinkedHashMap<>(fields);
        }
    }

    /**
     * Marks this record as emitted if it hasn't been already.
     * 
     * @return true if this was the first call (emission should proceed), false
     *         otherwise
     */
    public boolean markEmittedIfFirst() {
        return emitted.compareAndSet(false, true);
    }

    /**
     * Returns whether the record has been emitted.
     * 
     * @return true if already emitted
     */
    public boolean isEmitted() {
        return emitted.get();
    }

    /**
     * Truncates a string value to the specified maximum length.
     * 
     * @param value     the value to truncate
     * @param maxLength the maximum length
     * @return the truncated value
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * Truncates an error message to the standard maximum length.
     * 
     * @param message the message to truncate
     * @return the truncated message
     */
    public static String truncateErrorMessage(String message) {
        return truncate(message, MAX_ERROR_MESSAGE_LENGTH);
    }

    /**
     * Truncates a user agent string to the standard maximum length.
     * 
     * @param userAgent the user agent to truncate
     * @return the truncated user agent
     */
    public static String truncateUserAgent(String userAgent) {
        return truncate(userAgent, MAX_USER_AGENT_LENGTH);
    }

    /**
     * Converts an exception to a stack trace string.
     * 
     * @param ex the exception
     * @return the stack trace as a string
     */
    public static String exceptionToStackTrace(Throwable ex) {
        if (ex == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
