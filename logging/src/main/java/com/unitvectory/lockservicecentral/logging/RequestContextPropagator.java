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

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Utility for propagating request context to background threads.
 * 
 * <p>When background work needs to contribute fields to the canonical log record,
 * use this helper to capture and propagate the request context.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * RequestAttributes requestAttributes = RequestContextPropagator.capture();
 * executor.submit(() -> {
 *     try (RequestContextPropagator.Scope scope = RequestContextPropagator.propagate(requestAttributes)) {
 *         // Access request-scoped beans here
 *         context.put("background_field", value);
 *     }
 * });
 * }</pre>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public final class RequestContextPropagator {

    private RequestContextPropagator() {
        // Utility class
    }

    /**
     * Captures the current request attributes.
     * 
     * @return the request attributes, or null if not in a request context
     */
    public static RequestAttributes capture() {
        return RequestContextHolder.getRequestAttributes();
    }

    /**
     * Propagates request attributes to the current thread, returning a scope
     * that will clean up on close.
     * 
     * @param requestAttributes the request attributes to propagate
     * @return a scope that must be closed (preferably in a try-with-resources)
     */
    public static Scope propagate(RequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return Scope.NOOP;
        }
        RequestContextHolder.setRequestAttributes(requestAttributes);
        return new Scope() {
            @Override
            public void close() {
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }

    /**
     * A scope that cleans up request context propagation when closed.
     */
    public interface Scope extends AutoCloseable {
        /**
         * A no-op scope for when there's no request context to propagate.
         */
        Scope NOOP = () -> { };

        @Override
        void close();
    }
}
