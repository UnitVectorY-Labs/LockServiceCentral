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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring configuration for canonical logging components.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@EnableConfigurationProperties(AppRuntimeProperties.class)
public class CanonicalLoggingConfig {

    /**
     * Creates a request-scoped canonical log context with a scoped proxy.
     *
     * <p>
     * The scoped proxy ensures that singleton beans (like filters and interceptors)
     * can safely inject this request-scoped bean without causing scope issues.
     * </p>
     *
     * @return a new context for each request
     */
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public CanonicalLogContext canonicalLogContext() {
        return new CanonicalLogContext();
    }
}
