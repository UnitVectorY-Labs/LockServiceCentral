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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dedicated logger for canonical log records.
 * 
 * <p>Wraps a SLF4J logger named "canonical" to keep the intent clear and ensure
 * canonical records are routed to a dedicated appender.</p>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Component
public class CanonicalLogger {

    private static final Logger logger = LoggerFactory.getLogger("canonical");

    /**
     * Logs a canonical JSON line at INFO level.
     * 
     * @param jsonLine the JSON log line
     */
    public void info(String jsonLine) {
        logger.info(jsonLine);
    }

    /**
     * Logs a warning message.
     * 
     * @param message the warning message
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * Logs an error message.
     * 
     * @param message the error message
     * @param ex the exception
     */
    public void error(String message, Throwable ex) {
        logger.error(message, ex);
    }
}
