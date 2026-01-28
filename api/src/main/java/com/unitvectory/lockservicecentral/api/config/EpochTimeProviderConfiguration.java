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
package com.unitvectory.lockservicecentral.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.unitvectory.consistgen.epoch.EpochTimeProvider;
import com.unitvectory.consistgen.epoch.SystemEpochTimeProvider;

/**
 * The Time Configuration
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@Profile("!time-disabled")
public class EpochTimeProviderConfiguration {

    /**
     * Creates the EpochTimeProvider bean.
     *
     * @return the EpochTimeProvider instance
     */
    @Bean
    public EpochTimeProvider epochTimeProvider() {
        return SystemEpochTimeProvider.getInstance();
    }
}
