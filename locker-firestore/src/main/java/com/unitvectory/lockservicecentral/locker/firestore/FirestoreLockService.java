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
package com.unitvectory.lockservicecentral.locker.firestore;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.ObjectProvider;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The Firestore LockService.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Slf4j
public class FirestoreLockService implements LockService {

    private final Firestore firestore;
    private final String collectionLocks;
    private final ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

    /**
     * Constructs a new FirestoreLockService.
     *
     * @param firestore the Firestore client
     * @param collectionLocks the collection name for locks
     * @param canonicalLogContextProvider provider for the canonical log context
     */
    public FirestoreLockService(Firestore firestore, String collectionLocks,
            ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
        this.firestore = firestore;
        this.collectionLocks = collectionLocks;
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

    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        // Firestore document reference for the lock, structured by namespace and lock
        // name.
        String documentId = namespace + ":" + lockName;
        DocumentReference docRef = firestore.collection(collectionLocks).document(documentId);

        try {
            var snapshot = docRef.get().get();
            if (snapshot.exists()) {
                return new Lock(snapshot.getData());
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting lock: {} {}", namespace, lockName, e);
        }

        return null;
    }

    @Override
    public Lock acquireLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        Map<String, Object> data = lock.toMap();

        // Set the Firestore specific TTL attribute
        Timestamp ttl = Timestamp.ofTimeSecondsAndNanos(lock.getExpiry(), 0);
        data.put("ttl", ttl);

        // Firestore document reference for the lock, structured by namespace and lock
        // name.
        String documentId = lock.getNamespace() + ":" + lock.getLockName();
        DocumentReference docRef = firestore.collection(collectionLocks)
                .document(documentId);

        try {
            // Run a Firestore transaction to ensure atomicity
            firestore.runTransaction((Transaction transaction) -> {
                var snapshot = transaction.get(docRef).get();

                if (!snapshot.exists()) {
                    // New record
                    transaction.set(docRef, data);
                    lock.setSuccess();
                    recordOutcome("ACQUIRED");
                } else {
                    Lock existingLock = new Lock(snapshot.getData());

                    if (lock.isMatch(existingLock)) {
                        // Lock is already acquired by the same owner, it can be updated with the new
                        // expiry
                        transaction.set(docRef, data);
                        lock.setSuccess();
                        recordOutcome("LOCK_REPLACED");

                    } else if (!existingLock.isExpired(now)) {
                        // Lock is still valid, return conflict
                        lock.setFailed();
                        recordOutcome("ACQUIRE_CONFLICT");
                    } else {
                        // Lock is expired, so we can acquire it
                        transaction.set(docRef, data);
                        lock.setSuccess();
                        recordOutcome("ACQUIRED");
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error acquiring lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("ACQUIRE_ERROR");
        }

        return lock;
    }

    @Override
    public Lock renewLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();

        // Firestore document reference for the lock, structured by namespace and lock
        // name.
        String documentId = lock.getNamespace() + ":" + lock.getLockName();
        DocumentReference docRef = firestore.collection(collectionLocks).document(documentId);

        try {
            // Run a Firestore transaction to ensure atomicity
            firestore.runTransaction((Transaction transaction) -> {
                var snapshot = transaction.get(docRef).get();

                if (!snapshot.exists()) {
                    // Lock doesn't exist, so it cannot be renewed
                    lock.setFailed();
                    recordOutcome("RENEW_NOT_FOUND");
                } else {
                    Lock existingLock = new Lock(snapshot.getData());

                    if (!lock.isMatch(existingLock)) {
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

                        Map<String, Object> data = lock.toMap();

                        // Set the Firestore specific TTL attribute
                        Timestamp ttl = Timestamp.ofTimeSecondsAndNanos(lock.getExpiry(), 0);
                        data.put("ttl", ttl);

                        transaction.set(docRef, data);

                        lock.setSuccess();
                        recordOutcome("RENEWED");
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error renewing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RENEW_ERROR");
        }

        return lock;
    }

    @Override
    public Lock releaseLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();

        // Firestore document reference for the lock, structured by namespace and lock
        // name.
        String documentId = lock.getNamespace() + ":" + lock.getLockName();
        DocumentReference docRef = firestore.collection(collectionLocks).document(documentId);

        try {
            // Run a Firestore transaction to ensure atomicity
            firestore.runTransaction((Transaction transaction) -> {
                var snapshot = transaction.get(docRef).get();

                if (!snapshot.exists()) {
                    // Lock doesn't exist, so it is already released
                    lock.setCleared();
                    recordOutcome("RELEASED_NOT_FOUND");
                } else {
                    Lock existingLock = new Lock(snapshot.getData());

                    if (existingLock.isExpired(now)) {
                        // Lock is expired, so it is already released
                        lock.setCleared();
                        recordOutcome("RELEASED_EXPIRED");
                    } else if (!lock.isMatch(existingLock)) {
                        // Lock doesn't match, so it cannot be released
                        lock.setFailed();
                        recordOutcome("RELEASE_CONFLICT");
                    } else {
                        // Successfully release the lock by deleting the document
                        transaction.delete(docRef);
                        lock.setCleared();
                        recordOutcome("RELEASED");
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error releasing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RELEASE_ERROR");
        }

        return lock;
    }
}
