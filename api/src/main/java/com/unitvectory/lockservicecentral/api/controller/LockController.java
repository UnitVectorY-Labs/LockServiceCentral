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
package com.unitvectory.lockservicecentral.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unitvectory.jsonschema4springboot.ValidateJsonSchema;
import com.unitvectory.jsonschema4springboot.ValidateJsonSchemaVersion;
import com.unitvectory.lockservicecentral.api.service.LockService;
import com.unitvectory.lockservicecentral.datamodel.model.Lock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The Lock Controller
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@RestController
public class LockController {

    @Autowired
    private LockService lockService;

    // Make a post request to acquire a lock
    @PostMapping("/v1/{namespace}/lock/{lockName}/acquire")
    public ResponseEntity<Lock> acquireLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 4, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:acquireLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        // Pass the parameters as part of the object
        lock.setNamespace(namespace);
        lock.setLockName(lockName);
        if (jwt == null) {
            lock.setOwner("anonymous");
        } else {
            lock.setOwner(jwt.getSubject());
        }

        Lock acquiredLock = lockService.acquireLock(lock);

        return acquiredLock.getSuccess() ? ResponseEntity.ok(lock)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(lock);
    }

    @PostMapping("/v1/{namespace}/lock/{lockName}/renew")
    public ResponseEntity<Lock> renewLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 4, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:renewLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        // Pass the parameters as part of the object
        lock.setNamespace(namespace);
        lock.setLockName(lockName);
        if (jwt == null) {
            lock.setOwner("anonymous");
        } else {
            lock.setOwner(jwt.getSubject());
        }

        Lock renewedLock = lockService.renewLock(lock);

        return renewedLock.getSuccess() ? ResponseEntity.ok(lock)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(lock);
    }

    @PostMapping("/v1/{namespace}/lock/{lockName}/release")
    public ResponseEntity<Lock> releaseLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 4, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:releaseLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        // Pass the parameters as part of the object
        lock.setNamespace(namespace);
        lock.setLockName(lockName);
        if (jwt == null) {
            lock.setOwner("anonymous");
        } else {
            lock.setOwner(jwt.getSubject());
        }

        Lock releasedLock = lockService.releaseLock(lock);

        return releasedLock.getSuccess() ? ResponseEntity.ok(lock)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(lock);
    }
}
