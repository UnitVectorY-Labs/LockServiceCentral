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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unitvectory.jsonschema4springboot.ValidateJsonSchema;
import com.unitvectory.jsonschema4springboot.ValidateJsonSchemaVersion;
import com.unitvectory.lockservicecentral.api.service.LockService;
import com.unitvectory.lockservicecentral.locker.model.Lock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

/**
 * The Lock Controller
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@RestController
public class LockController {

    @Autowired
    private LockService lockService;

    /**
     * Gets the status of a lock.
     * 
     * The status of the lock is always returned, even if the lock does not exist as
     * that is just an available lock. The locks that are unavailable are the ones
     * that include the `owner` and `expiry` fields.
     * 
     * @param namespace the lock namespace
     * @param lockName  the lock name
     * @return the lock status
     */
    @GetMapping("/v1/{namespace}/lock/{lockName}")
    public ResponseEntity<Lock> getLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName) {

        Lock lock = lockService.getLock(namespace, lockName);

        return ResponseEntity.ok(lock);
    }

    /**
     * Acquire a lock.
     * 
     * @param namespace the lock namespace
     * @param lockName  the lock name
     * @param lock      the lock request
     * @param jwt       the JWT
     * @return the lock response
     */
    @PostMapping("/v1/{namespace}/lock/{lockName}/acquire")
    public ResponseEntity<Lock> acquireLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:acquireLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        this.setLockAttributes(lock, namespace, lockName, jwt);

        Lock acquiredLock = lockService.acquireLock(lock);

        return acquiredLock.getSuccess() ? ResponseEntity.ok(acquiredLock)
                : ResponseEntity.status(HttpStatus.LOCKED).body(acquiredLock);
    }

    /**
     * Renew a lock.
     * 
     * @param namespace the lock namespace
     * @param lockName  the lock name
     * @param lock      the lock request
     * @param jwt       the JWT
     * @return the lock response
     */
    @PostMapping("/v1/{namespace}/lock/{lockName}/renew")
    public ResponseEntity<Lock> renewLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:renewLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        this.setLockAttributes(lock, namespace, lockName, jwt);

        Lock renewedLock = lockService.renewLock(lock);

        return renewedLock.getSuccess() ? ResponseEntity.ok(renewedLock)
                : ResponseEntity.status(HttpStatus.LOCKED).body(renewedLock);
    }

    /**
     * Release a lock.
     * 
     * @param namespace the lock namespace
     * @param lockName  the lock name
     * @param lock      the lock request
     * @param jwt       the JWT
     * @return the lock response
     */
    @PostMapping("/v1/{namespace}/lock/{lockName}/release")
    public ResponseEntity<Lock> releaseLock(
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Namespace must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Namespace must be between 3 and 64 characters long") String namespace,
            @Valid @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Lock name must be alphanumeric with dashes and underscores only") @Size(min = 3, max = 64, message = "Lock name must be between 3 and 64 characters long") String lockName,
            @ValidateJsonSchema(version = ValidateJsonSchemaVersion.V7, schemaPath = "classpath:releaseLockSchema.json") Lock lock,
            @AuthenticationPrincipal Jwt jwt) {

        this.setLockAttributes(lock, namespace, lockName, jwt);

        Lock releasedLock = lockService.releaseLock(lock);

        return releasedLock.getSuccess() ? ResponseEntity.ok(releasedLock)
                : ResponseEntity.status(HttpStatus.LOCKED).body(releasedLock);
    }

    /**
     * Sets the lock attributes.
     * 
     * @param lock      the lock
     * @param namespace the namespace
     * @param lockName  the lock name
     * @param jwt       the JWT
     */
    private void setLockAttributes(@NonNull Lock lock, @NonNull String namespace, @NonNull String lockName, Jwt jwt) {
        lock.setNamespace(namespace);
        lock.setLockName(lockName);
        if (jwt == null) {
            lock.setOwner("anonymous");
        } else {
            lock.setOwner(jwt.getSubject());
        }
    }
}
