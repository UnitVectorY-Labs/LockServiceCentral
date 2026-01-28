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
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * DynamoDB implementation of {@link LockService} providing distributed lock functionality.
 * 
 * <p>This implementation uses DynamoDB's conditional write operations to ensure atomic
 * lock operations across distributed instances. All lock mutations (acquire, renew, release)
 * are performed using single atomic operations with comprehensive condition expressions,
 * eliminating race conditions that would occur with read-then-write patterns.</p>
 * 
 * <h2>Atomicity Guarantees</h2>
 * <ul>
 *   <li><b>Acquire</b>: Single PutItem with condition that succeeds only if the lock doesn't
 *       exist, is expired, or belongs to the same owner/instance</li>
 *   <li><b>Renew</b>: Single UpdateItem with condition that succeeds only if the lock exists,
 *       is not expired, and matches the owner/instance</li>
 *   <li><b>Release</b>: Single DeleteItem with condition that succeeds only if the lock
 *       matches the owner/instance (expired locks are also deletable)</li>
 * </ul>
 * 
 * <h2>Lock Expiry Handling</h2>
 * <p>Lock expiry is checked atomically within DynamoDB condition expressions using the
 * current timestamp passed to each operation. This ensures that expiry checks cannot
 * be affected by clock skew between read and write operations.</p>
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

    /**
     * Acquires a lock atomically using a single DynamoDB PutItem operation.
     * 
     * <p>The condition expression handles all edge cases atomically:</p>
     * <ul>
     *   <li>Lock doesn't exist: attribute_not_exists(lockId)</li>
     *   <li>Lock is expired: expiry &lt; :now</li>
     *   <li>Lock belongs to same owner/instance: owner = :owner AND instanceId = :instanceId</li>
     * </ul>
     * 
     * <p>This eliminates race conditions by performing all checks and the write in a single
     * atomic operation. No read-before-write pattern is used.</p>
     * 
     * @param originalLock the lock request containing namespace, lockName, owner, instanceId,
     *                     leaseDuration, and expiry
     * @param now the current timestamp in epoch seconds
     * @return the lock with success/failure status set
     */
    @Override
    public Lock acquireLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();

        try {
            Map<String, AttributeValue> item = lockToItem(lock);

            // Atomic condition: lock doesn't exist OR lock is expired OR same owner/instance
            // This handles all acquire scenarios in a single atomic operation
            String conditionExpression = 
                "attribute_not_exists(lockId) OR " +
                "expiry < :now OR " +
                "(#owner = :owner AND instanceId = :instanceId)";

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":now", AttributeValue.builder().n(String.valueOf(now)).build());
            expressionValues.put(":owner", AttributeValue.builder().s(lock.getOwner()).build());
            expressionValues.put(":instanceId", AttributeValue.builder().s(lock.getInstanceId()).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#owner", "owner");

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .conditionExpression(conditionExpression)
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .build();

            dynamoDbClient.putItem(putRequest);
            lock.setSuccess();
            recordOutcome("ACQUIRED");

        } catch (ConditionalCheckFailedException e) {
            // Condition failed: lock exists, is not expired, and belongs to different owner
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

    /**
     * Renews a lock atomically using a single DynamoDB UpdateItem operation.
     * 
     * <p>The condition expression ensures all requirements are met atomically:</p>
     * <ul>
     *   <li>Lock must exist (implicit in UpdateItem on existing key)</li>
     *   <li>Lock must not be expired: expiry &gt;= :now</li>
     *   <li>Lock must match owner: owner = :owner</li>
     *   <li>Lock must match instanceId: instanceId = :instanceId</li>
     * </ul>
     * 
     * <p>The update atomically adds the requested leaseDuration to both the existing
     * leaseDuration and expiry values. This eliminates race conditions by performing
     * condition checks and updates in a single atomic operation.</p>
     * 
     * @param originalLock the lock request containing namespace, lockName, owner, instanceId,
     *                     and leaseDuration to add
     * @param now the current timestamp in epoch seconds
     * @return the lock with updated leaseDuration/expiry and success/failure status set
     */
    @Override
    public Lock renewLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // Atomic condition: lock exists, is not expired, and matches owner/instance
            String conditionExpression = 
                "attribute_exists(lockId) AND " +
                "expiry >= :now AND " +
                "#owner = :owner AND " +
                "instanceId = :instanceId";

            // Update expression: add leaseDuration to both leaseDuration and expiry
            String updateExpression = 
                "SET leaseDuration = leaseDuration + :addDuration, " +
                "expiry = expiry + :addDuration";

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":now", AttributeValue.builder().n(String.valueOf(now)).build());
            expressionValues.put(":owner", AttributeValue.builder().s(lock.getOwner()).build());
            expressionValues.put(":instanceId", AttributeValue.builder().s(lock.getInstanceId()).build());
            expressionValues.put(":addDuration", AttributeValue.builder().n(String.valueOf(lock.getLeaseDuration())).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#owner", "owner");

            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("lockId", AttributeValue.builder().s(key).build()))
                    .conditionExpression(conditionExpression)
                    .updateExpression(updateExpression)
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build();

            UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);
            
            // Extract the updated values from the response
            Lock updatedLock = itemToLock(response.attributes());
            if (updatedLock != null) {
                lock.setLeaseDuration(updatedLock.getLeaseDuration());
                lock.setExpiry(updatedLock.getExpiry());
            }
            
            lock.setSuccess();
            recordOutcome("RENEWED");

        } catch (ConditionalCheckFailedException e) {
            // Condition failed: lock doesn't exist, is expired, or belongs to different owner
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

    /**
     * Releases a lock atomically using a single DynamoDB DeleteItem operation.
     * 
     * <p>The condition expression ensures ownership before deletion:</p>
     * <ul>
     *   <li>Lock must match owner: owner = :owner</li>
     *   <li>Lock must match instanceId: instanceId = :instanceId</li>
     * </ul>
     * 
     * <p>Note: Expired locks owned by the same owner/instance can still be released.
     * If the lock doesn't exist, the delete succeeds (idempotent behavior).
     * If the lock exists but belongs to a different owner, the condition fails.</p>
     * 
     * @param originalLock the lock request containing namespace, lockName, owner, and instanceId
     * @param now the current timestamp in epoch seconds (not used for expiry check in release)
     * @return the lock with cleared values and success/failure status set
     */
    @Override
    public Lock releaseLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // Atomic condition: lock must match owner and instanceId
            // We allow releasing expired locks that we own
            String conditionExpression = 
                "#owner = :owner AND instanceId = :instanceId";

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":owner", AttributeValue.builder().s(lock.getOwner()).build());
            expressionValues.put(":instanceId", AttributeValue.builder().s(lock.getInstanceId()).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#owner", "owner");

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("lockId", AttributeValue.builder().s(key).build()))
                    .conditionExpression(conditionExpression)
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .build();

            dynamoDbClient.deleteItem(deleteRequest);
            lock.setCleared();
            recordOutcome("RELEASED");

        } catch (ConditionalCheckFailedException e) {
            // Condition failed: could mean lock doesn't exist or belongs to different owner
            // Check if lock doesn't exist (which is success - already released)
            Lock existingLock = getLock(lock.getNamespace(), lock.getLockName());
            if (existingLock == null) {
                // Lock doesn't exist - treat as success
                lock.setCleared();
                recordOutcome("RELEASED_NOT_FOUND");
            } else if (existingLock.isExpired(now)) {
                // Lock exists but is expired and belongs to someone else
                // Still consider this a success since the lock is effectively released
                lock.setCleared();
                recordOutcome("RELEASED_EXPIRED");
            } else {
                // Lock exists and belongs to different owner
                log.debug("Conditional check failed for release lock: {}", lock, e);
                lock.setFailed();
                recordOutcome("RELEASE_CONFLICT");
            }

        } catch (DynamoDbException e) {
            log.error("Error releasing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RELEASE_ERROR");
        }

        return lock;
    }
}
