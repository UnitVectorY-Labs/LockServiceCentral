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
package com.unitvectory.lockservicecentral.locker.dynamodb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * The DynamoDB LockService.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Slf4j
public class DynamoDbLockService implements LockService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

    /**
     * Constructs a new DynamoDbLockService.
     * 
     * @param dynamoDbClient the DynamoDB client
     * @param tableName the DynamoDB table name for locks
     * @param canonicalLogContextProvider provider for the canonical log context
     */
    public DynamoDbLockService(DynamoDbClient dynamoDbClient, String tableName,
            ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.canonicalLogContextProvider = canonicalLogContextProvider;
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
     * Generates the DynamoDB primary key for a lock based on namespace and lock name.
     * 
     * @param namespace the namespace
     * @param lockName  the lock name
     * @return the primary key value
     */
    private String generateKey(String namespace, String lockName) {
        return namespace + ":" + lockName;
    }

    /**
     * Converts a DynamoDB item to a Lock object.
     * 
     * @param item the DynamoDB item
     * @return the Lock object, or null if item is empty
     */
    private Lock itemToLock(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("namespace", item.get("namespace").s());
        map.put("lockName", item.get("lockName").s());
        map.put("owner", item.get("owner").s());
        map.put("instanceId", item.get("instanceId").s());
        map.put("leaseDuration", Long.parseLong(item.get("leaseDuration").n()));
        map.put("expiry", Long.parseLong(item.get("expiry").n()));

        return new Lock(map);
    }

    /**
     * Converts a Lock object to a DynamoDB item.
     * 
     * @param lock the Lock object
     * @return the DynamoDB item
     */
    private Map<String, AttributeValue> lockToItem(Lock lock) {
        Map<String, AttributeValue> item = new HashMap<>();
        String key = generateKey(lock.getNamespace(), lock.getLockName());
        
        item.put("lockId", AttributeValue.builder().s(key).build());
        item.put("namespace", AttributeValue.builder().s(lock.getNamespace()).build());
        item.put("lockName", AttributeValue.builder().s(lock.getLockName()).build());
        item.put("owner", AttributeValue.builder().s(lock.getOwner()).build());
        item.put("instanceId", AttributeValue.builder().s(lock.getInstanceId()).build());
        item.put("leaseDuration", AttributeValue.builder().n(String.valueOf(lock.getLeaseDuration())).build());
        item.put("expiry", AttributeValue.builder().n(String.valueOf(lock.getExpiry())).build());

        return item;
    }

    @Override
    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        String key = generateKey(namespace, lockName);

        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("lockId", AttributeValue.builder().s(key).build()))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem() || response.item().isEmpty()) {
                return null;
            }

            return itemToLock(response.item());

        } catch (DynamoDbException e) {
            log.error("Error getting lock: {} {}", namespace, lockName, e);
            return null;
        }
    }

    @Override
    public Lock acquireLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();

        try {
            // First, try to get the existing lock
            Lock existingLock = getLock(lock.getNamespace(), lock.getLockName());

            if (existingLock == null) {
                // No existing lock - use conditional put to ensure it doesn't exist
                Map<String, AttributeValue> item = lockToItem(lock);

                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("attribute_not_exists(lockId)")
                        .build();

                dynamoDbClient.putItem(putRequest);
                lock.setSuccess();
                recordOutcome("ACQUIRED");

            } else if (lock.isMatch(existingLock)) {
                // Lock is already acquired by the same owner, it can be updated with the new expiry
                Map<String, AttributeValue> item = lockToItem(lock);

                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .build();

                dynamoDbClient.putItem(putRequest);
                lock.setSuccess();
                recordOutcome("LOCK_REPLACED");

            } else if (!existingLock.isExpired(now)) {
                // Lock is still valid, return conflict
                lock.setFailed();
                recordOutcome("ACQUIRE_CONFLICT");

            } else {
                // Lock is expired, so we can acquire it
                // Use conditional put to ensure the lock hasn't changed since we read it
                Map<String, AttributeValue> item = lockToItem(lock);

                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("expiry = :oldExpiry")
                        .expressionAttributeValues(Map.of(
                                ":oldExpiry", AttributeValue.builder().n(String.valueOf(existingLock.getExpiry())).build()))
                        .build();

                dynamoDbClient.putItem(putRequest);
                lock.setSuccess();
                recordOutcome("ACQUIRED");
            }

        } catch (ConditionalCheckFailedException e) {
            // Conditional check failed - someone else modified the lock
            log.debug("Conditional check failed for acquire lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("ACQUIRE_CONFLICT");

        } catch (DynamoDbException e) {
            log.error("Error acquiring lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("ACQUIRE_ERROR");
        }

        return lock;
    }

    @Override
    public Lock renewLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();

        try {
            // Get the current lock
            Lock existingLock = getLock(lock.getNamespace(), lock.getLockName());

            if (existingLock == null) {
                // Lock doesn't exist, so it cannot be renewed
                lock.setFailed();
                recordOutcome("RENEW_NOT_FOUND");

            } else if (!lock.isMatch(existingLock)) {
                // Lock doesn't match, so it cannot be renewed
                lock.setFailed();
                recordOutcome("RENEW_MISMATCH");

            } else if (existingLock.isExpired(now)) {
                // Lock is expired, so it cannot be renewed
                lock.setFailed();
                recordOutcome("RENEW_EXPIRED");

            } else {
                // Successfully renew the lock
                long newLeaseDuration = existingLock.getLeaseDuration() + lock.getLeaseDuration();
                long newExpiry = existingLock.getExpiry() + lock.getLeaseDuration();
                lock.setLeaseDuration(newLeaseDuration);
                lock.setExpiry(newExpiry);

                Map<String, AttributeValue> item = lockToItem(lock);

                // Use conditional put to ensure the lock hasn't changed since we read it
                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("expiry = :oldExpiry AND owner = :owner AND instanceId = :instanceId")
                        .expressionAttributeValues(Map.of(
                                ":oldExpiry", AttributeValue.builder().n(String.valueOf(existingLock.getExpiry())).build(),
                                ":owner", AttributeValue.builder().s(existingLock.getOwner()).build(),
                                ":instanceId", AttributeValue.builder().s(existingLock.getInstanceId()).build()))
                        .build();

                dynamoDbClient.putItem(putRequest);
                lock.setSuccess();
                recordOutcome("RENEWED");
            }

        } catch (ConditionalCheckFailedException e) {
            log.debug("Conditional check failed for renew lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RENEW_CONFLICT");

        } catch (DynamoDbException e) {
            log.error("Error renewing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RENEW_ERROR");
        }

        return lock;
    }

    @Override
    public Lock releaseLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // Get the current lock
            Lock existingLock = getLock(lock.getNamespace(), lock.getLockName());

            if (existingLock == null) {
                // Lock doesn't exist, so it is already released
                lock.setCleared();
                recordOutcome("RELEASED_NOT_FOUND");

            } else if (existingLock.isExpired(now)) {
                // Lock is expired, so it is already released
                lock.setCleared();
                recordOutcome("RELEASED_EXPIRED");

            } else if (!lock.isMatch(existingLock)) {
                // Lock doesn't match, so it cannot be released
                lock.setFailed();
                recordOutcome("RELEASE_CONFLICT");

            } else {
                // Successfully release the lock by deleting the item
                // Use conditional delete to ensure the lock hasn't changed since we read it
                DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                        .tableName(tableName)
                        .key(Map.of("lockId", AttributeValue.builder().s(key).build()))
                        .conditionExpression("expiry = :oldExpiry AND owner = :owner AND instanceId = :instanceId")
                        .expressionAttributeValues(Map.of(
                                ":oldExpiry", AttributeValue.builder().n(String.valueOf(existingLock.getExpiry())).build(),
                                ":owner", AttributeValue.builder().s(existingLock.getOwner()).build(),
                                ":instanceId", AttributeValue.builder().s(existingLock.getInstanceId()).build()))
                        .build();

                dynamoDbClient.deleteItem(deleteRequest);
                lock.setCleared();
                recordOutcome("RELEASED");
            }

        } catch (ConditionalCheckFailedException e) {
            log.debug("Conditional check failed for release lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RELEASE_CONFLICT");

        } catch (DynamoDbException e) {
            log.error("Error releasing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RELEASE_ERROR");
        }

        return lock;
    }
}
