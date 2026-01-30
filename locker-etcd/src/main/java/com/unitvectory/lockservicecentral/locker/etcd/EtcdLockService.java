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
package com.unitvectory.lockservicecentral.locker.etcd;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.ObjectProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The etcd LockService.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Slf4j
public class EtcdLockService implements LockService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    private final Client client;
    private final String keyPrefix;
    private final int maxRetries;
    private final long requestTimeoutMs;
    private final ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

    /**
     * Constructs a new EtcdLockService.
     *
     * @param client the etcd client
     * @param keyPrefix the key prefix for locks
     * @param maxRetries the maximum number of retries
     * @param requestTimeoutMs the request timeout in milliseconds
     * @param canonicalLogContextProvider provider for the canonical log context
     */
    public EtcdLockService(Client client, String keyPrefix, int maxRetries, long requestTimeoutMs,
            ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
        this.client = client;
        this.keyPrefix = keyPrefix;
        this.maxRetries = maxRetries;
        this.requestTimeoutMs = requestTimeoutMs;
        this.canonicalLogContextProvider = canonicalLogContextProvider;
    }

    @Override
    public String getBackendName() {
        return "etcd";
    }

    /**
     * Records the lock service outcome to the canonical log context.
     *
     * @param outcome the screaming snake case outcome
     */
    private void recordOutcome(String outcome) {
        try {
            CanonicalLogContext context = canonicalLogContextProvider.getObject();
            context.put("lock_service_outcome", outcome);
        } catch (Exception e) {
            // Don't break lock operations if logging fails
        }
    }

    /**
     * Generates the etcd key for a lock based on namespace and lock name.
     *
     * @param namespace the namespace
     * @param lockName  the lock name
     * @return the etcd key as ByteSequence
     */
    private ByteSequence generateKey(String namespace, String lockName) {
        String key = this.keyPrefix + namespace + ":" + lockName;
        return ByteSequence.from(key, StandardCharsets.UTF_8);
    }

    /**
     * Serializes a Lock to JSON bytes for storage in etcd.
     *
     * @param lock the lock to serialize
     * @return the JSON bytes
     * @throws JsonProcessingException if serialization fails
     */
    private ByteSequence serializeLock(Lock lock) throws JsonProcessingException {
        Map<String, Object> data = lock.toMap();
        String json = OBJECT_MAPPER.writeValueAsString(data);
        return ByteSequence.from(json, StandardCharsets.UTF_8);
    }

    /**
     * Deserializes a Lock from JSON bytes stored in etcd.
     *
     * @param value the JSON bytes
     * @return the Lock object
     * @throws JsonProcessingException if deserialization fails
     */
    private Lock deserializeLock(ByteSequence value) throws JsonProcessingException {
        String json = value.toString(StandardCharsets.UTF_8);
        Map<String, Object> map = OBJECT_MAPPER.readValue(json, MAP_TYPE_REF);

        // Ensure numeric fields are Long to satisfy Lock constructor expectations
        Map<String, Object> normalizedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Number) {
                normalizedMap.put(entry.getKey(), ((Number) val).longValue());
            } else {
                normalizedMap.put(entry.getKey(), val);
            }
        }
        return new Lock(normalizedMap);
    }

    @Override
    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        ByteSequence key = generateKey(namespace, lockName);
        KV kvClient = client.getKVClient();

        try {
            CompletableFuture<GetResponse> future = kvClient.get(key);
            GetResponse response = future.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            if (response.getKvs().isEmpty()) {
                return null;
            }

            KeyValue kv = response.getKvs().get(0);
            return deserializeLock(kv.getValue());

        } catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
            log.error("Error getting lock: {} {}", namespace, lockName, e);
            return null;
        }
    }

    @Override
    public Lock acquireLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        ByteSequence key = generateKey(lock.getNamespace(), lock.getLockName());
        KV kvClient = client.getKVClient();
        Lease leaseClient = client.getLeaseClient();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Get the current state of the lock
                CompletableFuture<GetResponse> getFuture = kvClient.get(key);
                GetResponse getResponse = getFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

                boolean keyExists = !getResponse.getKvs().isEmpty();
                long currentModRevision = 0;
                Lock existingLock = null;

                if (keyExists) {
                    KeyValue kv = getResponse.getKvs().get(0);
                    currentModRevision = kv.getModRevision();
                    existingLock = deserializeLock(kv.getValue());
                }

                // Decision logic: can we acquire the lock?
                boolean canAcquire = false;
                if (!keyExists) {
                    // No existing lock
                    canAcquire = true;
                } else if (lock.isMatch(existingLock)) {
                    // Lock is already acquired by the same owner
                    canAcquire = true;
                } else if (existingLock.isExpired(now)) {
                    // Lock is expired
                    canAcquire = true;
                }

                if (!canAcquire) {
                    // Lock conflict
                    lock.setFailed();
                    recordOutcome("ACQUIRE_CONFLICT");
                    return lock;
                }

                // Calculate lease TTL (time remaining until expiry)
                long ttlSeconds = lock.getExpiry() - now;
                if (ttlSeconds <= 0) {
                    ttlSeconds = 1; // Minimum TTL of 1 second
                }

                // Create a lease for the lock
                CompletableFuture<LeaseGrantResponse> leaseFuture = leaseClient.grant(ttlSeconds);
                LeaseGrantResponse leaseResponse = leaseFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                long leaseId = leaseResponse.getID();

                // Serialize the lock data
                ByteSequence value = serializeLock(lock);

                // Build the transaction with CAS semantics
                Txn txn = kvClient.txn();
                TxnResponse txnResponse;

                if (!keyExists) {
                    // Key doesn't exist: compare createRevision == 0
                    txnResponse = txn
                            .If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.createRevision(0)))
                            .Then(Op.put(key, value, PutOption.builder().withLeaseId(leaseId).build()))
                            .commit()
                            .get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                } else {
                    // Key exists: compare modRevision matches what we read
                    txnResponse = txn
                            .If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.modRevision(currentModRevision)))
                            .Then(Op.put(key, value, PutOption.builder().withLeaseId(leaseId).build()))
                            .commit()
                            .get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                }

                if (txnResponse.isSucceeded()) {
                    lock.setSuccess();
                    recordOutcome("ACQUIRED");
                    return lock;
                } else {
                    // CAS failed, retry
                    leaseClient.revoke(leaseId).get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                    log.debug("CAS failed for acquire, attempt {}/{}", attempt + 1, maxRetries + 1);
                }

            } catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
                log.error("Error acquiring lock: {}", lock, e);
                lock.setFailed();
                recordOutcome("ACQUIRE_ERROR");
                return lock;
            }
        }

        // Max retries exceeded
        log.error("Max retries exceeded acquiring lock: {}", lock);
        lock.setFailed();
        recordOutcome("ACQUIRE_MAX_RETRIES");
        return lock;
    }

    @Override
    public Lock renewLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        ByteSequence key = generateKey(lock.getNamespace(), lock.getLockName());
        KV kvClient = client.getKVClient();
        Lease leaseClient = client.getLeaseClient();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Get the current state of the lock
                CompletableFuture<GetResponse> getFuture = kvClient.get(key);
                GetResponse getResponse = getFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

                if (getResponse.getKvs().isEmpty()) {
                    // Lock doesn't exist
                    lock.setFailed();
                    recordOutcome("RENEW_NOT_FOUND");
                    return lock;
                }

                KeyValue kv = getResponse.getKvs().get(0);
                long currentModRevision = kv.getModRevision();
                Lock existingLock = deserializeLock(kv.getValue());

                // Check if we can renew
                if (!lock.isMatch(existingLock)) {
                    lock.setFailed();
                    recordOutcome("RENEW_MISMATCH");
                    return lock;
                }

                if (existingLock.isExpired(now)) {
                    lock.setFailed();
                    recordOutcome("RENEW_EXPIRED");
                    return lock;
                }

                // Calculate new lease duration and expiry
                // Renew extends from the previous expiry, not from now
                long newLeaseDuration = existingLock.getLeaseDuration() + lock.getLeaseDuration();
                long newExpiry = existingLock.getExpiry() + lock.getLeaseDuration();
                lock.setLeaseDuration(newLeaseDuration);
                lock.setExpiry(newExpiry);

                // Calculate new TTL for the lease
                long ttlSeconds = newExpiry - now;
                if (ttlSeconds <= 0) {
                    ttlSeconds = 1; // Minimum TTL of 1 second
                }

                // Create a new lease with the extended TTL
                CompletableFuture<LeaseGrantResponse> leaseFuture = leaseClient.grant(ttlSeconds);
                LeaseGrantResponse leaseResponse = leaseFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                long leaseId = leaseResponse.getID();

                // Serialize the updated lock data
                ByteSequence value = serializeLock(lock);

                // Build the transaction with CAS semantics
                Txn txn = kvClient.txn();
                TxnResponse txnResponse = txn
                        .If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.modRevision(currentModRevision)))
                        .Then(Op.put(key, value, PutOption.builder().withLeaseId(leaseId).build()))
                        .commit()
                        .get(requestTimeoutMs, TimeUnit.MILLISECONDS);

                if (txnResponse.isSucceeded()) {
                    lock.setSuccess();
                    recordOutcome("RENEWED");
                    return lock;
                } else {
                    // CAS failed, retry
                    leaseClient.revoke(leaseId).get(requestTimeoutMs, TimeUnit.MILLISECONDS);
                    log.debug("CAS failed for renew, attempt {}/{}", attempt + 1, maxRetries + 1);
                }

            } catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
                log.error("Error renewing lock: {}", lock, e);
                lock.setFailed();
                recordOutcome("RENEW_ERROR");
                return lock;
            }
        }

        // Max retries exceeded
        log.error("Max retries exceeded renewing lock: {}", lock);
        lock.setFailed();
        recordOutcome("RENEW_MAX_RETRIES");
        return lock;
    }

    @Override
    public Lock releaseLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        ByteSequence key = generateKey(lock.getNamespace(), lock.getLockName());
        KV kvClient = client.getKVClient();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Get the current state of the lock
                CompletableFuture<GetResponse> getFuture = kvClient.get(key);
                GetResponse getResponse = getFuture.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

                if (getResponse.getKvs().isEmpty()) {
                    // Lock doesn't exist, already released
                    lock.setCleared();
                    recordOutcome("RELEASED_NOT_FOUND");
                    return lock;
                }

                KeyValue kv = getResponse.getKvs().get(0);
                long currentModRevision = kv.getModRevision();
                Lock existingLock = deserializeLock(kv.getValue());

                if (existingLock.isExpired(now)) {
                    // Lock is expired, already released
                    lock.setCleared();
                    recordOutcome("RELEASED_EXPIRED");
                    return lock;
                }

                if (!lock.isMatch(existingLock)) {
                    // Lock doesn't match, cannot release
                    lock.setFailed();
                    recordOutcome("RELEASE_CONFLICT");
                    return lock;
                }

                // Build the transaction with CAS semantics to delete
                Txn txn = kvClient.txn();
                TxnResponse txnResponse = txn
                        .If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.modRevision(currentModRevision)))
                        .Then(Op.delete(key, io.etcd.jetcd.options.DeleteOption.DEFAULT))
                        .commit()
                        .get(requestTimeoutMs, TimeUnit.MILLISECONDS);

                if (txnResponse.isSucceeded()) {
                    lock.setCleared();
                    recordOutcome("RELEASED");
                    return lock;
                } else {
                    // CAS failed, retry
                    log.debug("CAS failed for release, attempt {}/{}", attempt + 1, maxRetries + 1);
                }

            } catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
                log.error("Error releasing lock: {}", lock, e);
                lock.setFailed();
                recordOutcome("RELEASE_ERROR");
                return lock;
            }
        }

        // Max retries exceeded
        log.error("Max retries exceeded releasing lock: {}", lock);
        lock.setFailed();
        recordOutcome("RELEASE_MAX_RETRIES");
        return lock;
    }
}
