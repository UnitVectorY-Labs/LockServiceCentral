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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 *
 * Configures JWT validation based on `jwt.issuer` and `jwt.jwks` settings.
 * If neither is set, no authentication is required.
 *
 * If either is set, JWT authentication is required.
 * JWKS is prioritized, but OpenID Connect discovery is used if only the issuer
 * is set.
 *
 * If `jwt.issue` is set, the issuer claim is validated.
 * If `jwt.audience` is set, the audience claim is validated.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class MyCustomSecurityConfiguration {

	@Value("${authentication.disabled:false}")
	private boolean authenticationDisabled;

	@Value("${jwt.issuer:#{null}}")
	private String issuer;

	@Value("${jwt.jwks:#{null}}")
	private String jwks;

	@Value("${jwt.audience:#{null}}")
	private String audience;

	/**
	 * Creates the SecurityFilterChain bean.
	 *
	 * @param http the HttpSecurity to configure
	 * @return the configured SecurityFilterChain
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		if (this.issuer != null || this.jwks != null) {
			// Either issuer or JWKS is provided, so JWT authentication is required
			final NimbusJwtDecoder jwtDecoder = gJwtDecoder();

			OAuth2TokenValidator<Jwt> validator = getValidator();
			jwtDecoder.setJwtValidator(validator);

			http
					.authorizeHttpRequests(authorize -> authorize
							// Require authentication for all API requests
							.requestMatchers("/v1/**").authenticated()
							.anyRequest().permitAll())
					.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
		} else if (this.authenticationDisabled) {
			// No issuer or JWKS provided, no authentication required
			http
					// Given this is an unauthenticated backend service, we can disable CSRF
					.csrf((csrf) -> csrf.disable()).authorizeHttpRequests(authorize -> authorize
							// Allow API requests as authentication is not required
							.anyRequest().permitAll());
		} else {
			// No issuer or JWKS provided, but authentication is required, the app is
			// unusable but this is the default unconfigured state
			http
					// Given this is an unauthenticated backend service, we can disable CSRF
					.csrf((csrf) -> csrf.disable()).authorizeHttpRequests(authorize -> authorize
							.requestMatchers("/v1/**").authenticated()
							.anyRequest().permitAll());
		}

		return http.build();
	}

	private NimbusJwtDecoder gJwtDecoder() {
		if (jwks != null && !jwks.isBlank()) {
			// Use the provided JWKS URL for validation
			return NimbusJwtDecoder.withJwkSetUri(jwks).build();
		} else if (issuer != null && !issuer.isBlank()) {
			// Use OpenID Connect discovery if issuer is set but no JWKS
			return NimbusJwtDecoder.withIssuerLocation(issuer).build();
		} else {
			throw new IllegalArgumentException("Either jwt.issuer or jwt.jwks must be set");
		}
	}

	private OAuth2TokenValidator<Jwt> getValidator() {
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(JwtValidators.createDefault());

		if (issuer != null && !issuer.isBlank()) {
			// Validate the issuer claim
			validators.add(JwtValidators.createDefaultWithIssuer(issuer));
		}

		if (audience != null && !audience.isBlank()) {
			// Validate the audience claim if set
			validators.add(new AudienceClaimValidator(audience));
		}

		return new DelegatingOAuth2TokenValidator<>(validators);
	}
}