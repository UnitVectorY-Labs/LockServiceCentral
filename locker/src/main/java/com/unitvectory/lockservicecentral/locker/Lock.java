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
package com.unitvectory.lockservicecentral.locker;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The lock.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lock {

    @JsonIgnore
    private LockAction action;

    private Boolean success;

    private String namespace;

    private String lockName;

    private String owner;

    private String instanceId;

    private Long leaseDuration;

    private Long expiry;

    /**
     * Constructs a Lock from a map of properties.
     *
     * @param map the map containing lock properties
     */
    public Lock(Map<String, Object> map) {
        this.namespace = (String) map.get("namespace");
        this.lockName = (String) map.get("lockName");
        this.owner = (String) map.get("owner");
        this.instanceId = (String) map.get("instanceId");
        this.leaseDuration = (Long) map.get("leaseDuration");
        this.expiry = (Long) map.get("expiry");
    }

    /**
     * Creates a copy of this Lock.
     *
     * @return a new Lock instance with the same values
     */
    public Lock copy() {
        return new Lock(this.action, this.success, this.namespace, this.lockName, this.owner, this.instanceId,
                this.leaseDuration, this.expiry);
    }

    /**
     * Converts this Lock to a map representation.
     *
     * @return a map containing the lock properties
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("namespace", this.namespace);
        map.put("lockName", this.lockName);
        map.put("owner", this.owner);
        map.put("instanceId", this.instanceId);
        map.put("leaseDuration", this.leaseDuration);
        map.put("expiry", this.expiry);
        return map;
    }

    /**
     * Sets the lock state for a GET operation.
     *
     * @param now the current time in epoch seconds
     */
    public void setGet(long now) {
        this.action = LockAction.GET;
        this.success = null;

        // The instanceId is treated similar to a secret
        this.instanceId = null;

        if (expiry != null && expiry < now) {
            this.owner = null;
            this.leaseDuration = null;
            this.expiry = null;
        } else if (expiry != null) {
            this.leaseDuration = null;
        }
    }

    /**
     * Sets the lock state to indicate a failed operation.
     */
    public void setFailed() {
        this.action = LockAction.FAILED;
        this.success = false;
        this.owner = null;
        this.instanceId = null;
        this.leaseDuration = null;
        this.expiry = null;
    }

    /**
     * Sets the lock state to indicate a successful operation.
     */
    public void setSuccess() {
        this.action = LockAction.SUCCESS;
        this.success = true;
    }

    /**
     * Sets the lock state to indicate the lock has been cleared.
     */
    public void setCleared() {
        this.action = LockAction.SUCCESS;
        this.success = true;
        this.owner = null;
        this.instanceId = null;
        this.leaseDuration = null;
        this.expiry = null;
    }

    /**
     * Checks if the lock is currently active.
     *
     * @param now the current time in epoch seconds
     * @return true if the lock is active, false otherwise
     */
    public boolean isActive(long now) {
        return this.expiry == null || this.expiry >= now;
    }

    /**
     * Checks if the lock has expired.
     *
     * @param now the current time in epoch seconds
     * @return true if the lock has expired, false otherwise
     */
    public boolean isExpired(long now) {
        return this.expiry != null && this.expiry < now;
    }

    /**
     * Checks if this lock matches the given lock.
     *
     * @param lock the lock to compare against
     * @return true if the locks match, false otherwise
     */
    public boolean isMatch(Lock lock) {
        if (lock == null) {
            return false;
        }

        return (this.namespace == null || this.namespace.equals(lock.namespace)) &&
                (this.lockName == null || this.lockName.equals(lock.lockName)) &&
                (this.owner == null || this.owner.equals(lock.owner)) &&
                (this.instanceId == null || this.instanceId.equals(lock.instanceId));
    }
}