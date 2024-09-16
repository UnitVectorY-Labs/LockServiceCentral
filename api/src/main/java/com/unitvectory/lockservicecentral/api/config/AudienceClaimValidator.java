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

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.AllArgsConstructor;

/**
 * Custom logic to validate the aud claim in the JWT
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@AllArgsConstructor
class AudienceClaimValidator implements OAuth2TokenValidator<Jwt> {

	private final String requiredAudience;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {

		// Check if the audience claim is present and contains the required audience
		List<String> audience = jwt.getAudience();
		if (audience == null || audience.isEmpty() || !audience.contains(requiredAudience)) {
			// If the audience claim is missing or does not contain the required audience,
			// construct an error and return a failure result.
			OAuth2Error error = new OAuth2Error("invalid_audience",
					"The required audience is missing or invalid", null);
			return OAuth2TokenValidatorResult.failure(error);
		}

		// If the audience claim is present and valid, return a success result.
		return OAuth2TokenValidatorResult.success();
	}
}