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

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that initializes baseline fields in the canonical log context.
 * 
 * <p>This filter runs early in the request lifecycle to capture request-start fields
 * such as timestamps, request ID, HTTP method, and target path.</p>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CanonicalInitFilter extends OncePerRequestFilter {

    private static final String X_REQUEST_ID_HEADER = "X-Request-Id";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String REQUEST_ID_PREFIX = "req_";

    private final ObjectProvider<CanonicalLogContext> contextProvider;
    private final AppRuntimeProperties appRuntimeProperties;
    private final String serviceName;

    /**
     * Constructs a new filter.
     * 
     * @param contextProvider provides the request-scoped context
     * @param appRuntimeProperties application runtime properties
     * @param serviceName the service name from spring.application.name
     */
    public CanonicalInitFilter(
            ObjectProvider<CanonicalLogContext> contextProvider,
            AppRuntimeProperties appRuntimeProperties,
            @Value("${spring.application.name:unknown_service}") String serviceName) {
        this.contextProvider = contextProvider;
        this.appRuntimeProperties = appRuntimeProperties;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        CanonicalLogContext context = contextProvider.getObject();
        
        // Set baseline fields known at request start
        context.put("ts_start", context.getStartInstant());
        context.put("service", serviceName);
        context.put("env", appRuntimeProperties.getEnv());
        context.put("region", appRuntimeProperties.getRegion());
        context.put("version", appRuntimeProperties.getVersion());
        context.put("kind", "http");
        
        // Generate or adopt request ID
        String requestId = request.getHeader(X_REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = REQUEST_ID_PREFIX + UUID.randomUUID().toString();
        }
        context.put("request_id", requestId);
        
        // HTTP request details
        context.put("http_method", request.getMethod());
        context.put("http_target", request.getRequestURI());
        
        // Optional baseline fields
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (userAgent != null && !userAgent.isBlank()) {
            context.put("http_user_agent", CanonicalLogContext.truncateUserAgent(userAgent));
        }
        
        // Client IP resolution
        String clientIp = resolveClientIp(request);
        if (clientIp != null && !clientIp.isBlank()) {
            context.put("client_ip", clientIp);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the client IP address from X-Forwarded-For header or remote address.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take the first IP in the list
            String[] ips = forwardedFor.split(",");
            if (ips.length > 0) {
                return ips[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
