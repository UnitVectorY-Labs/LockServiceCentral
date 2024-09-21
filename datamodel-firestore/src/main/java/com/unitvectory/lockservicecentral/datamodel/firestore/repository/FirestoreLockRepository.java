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
package com.unitvectory.lockservicecentral.datamodel.firestore.repository;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.unitvectory.lockservicecentral.datamodel.model.Lock;
import com.unitvectory.lockservicecentral.datamodel.repository.LockRepository;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The data model config for GCP Firestore
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@AllArgsConstructor
@Slf4j
public class FirestoreLockRepository implements LockRepository {

    private Firestore firestore;

    private String collectionLocks;

    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        // Firestore document reference for the lock, structured by namespace and lock
        // name.
        String documentId = namespace + ":" + lockName;
        DocumentReference docRef = firestore.collection(collectionLocks).document(documentId);

        try {
            var snapshot = docRef.get().get();
            if (snapshot.exists()) {
                return snapshot.toObject(Lock.class);
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting lock: {} {}", namespace, lockName, e);
        }

        return null;
    }

    @Override
    public Lock acquireLock(@NonNull Lock lock, long now) {
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
                    // Write the lock to Firestore
                    Map<String, Object> data = lock.toMap();

                    // Set the Firestore specific TTL attribute
                    Timestamp ttl = Timestamp.ofTimeSecondsAndNanos(lock.getExpiry(), 0);
                    data.put("ttl", ttl);

                    transaction.set(docRef, data);
                    lock.setSuccess();
                    log.info("Lock acquired: {}", lock);
                } else {
                    Lock existingLock = snapshot.toObject(Lock.class);

                    if (lock.isMatch(existingLock)) {
                        // Lock is already acquired by the same owner, it can be updated with the new
                        // expiry
                        Map<String, Object> data = lock.toMap();

                        // Set the Firestore specific TTL attribute
                        Timestamp ttl = Timestamp.ofTimeSecondsAndNanos(lock.getExpiry(), 0);
                        data.put("ttl", ttl);

                        transaction.set(docRef, data);
                        lock.setSuccess();
                        log.info("Lock already acquired: {}", lock);

                    } else if (!existingLock.isExpired(now)) {
                        // Lock is still valid, return conflict
                        lock.setFailed();
                        log.warn("Lock conflict, cannot acquire: {}", lock);
                    } else {
                        // Lock is expired, so we can acquire it
                        Map<String, Object> data = lock.toMap();

                        // Set the Firestore specific TTL attribute
                        Timestamp ttl = Timestamp.ofTimeSecondsAndNanos(lock.getExpiry(), 0);
                        data.put("ttl", ttl);

                        transaction.set(docRef, data);
                        lock.setSuccess();
                        log.info("Lock acquired: {}", lock);
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error acquiring lock: {}", lock, e);
            lock.setFailed();
        }

        return lock;
    }

    @Override
    public Lock renewLock(@NonNull Lock lock, long now) {
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
                    log.warn("Lock does not exist, cannot renew: {}", lock);
                } else {
                    Lock existingLock = snapshot.toObject(Lock.class);

                    if (!lock.isMatch(existingLock)) {
                        // Lock doesn't match, so it cannot be renewed
                        lock.setFailed();
                        log.warn("Lock does not match, cannot renew: {}", lock);
                    } else if (existingLock.isExpired(now)) {
                        // Lock is expired, so it cannot be renewed
                        lock.setFailed();
                        log.warn("Expired lock cannot be extended: {}", lock);
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
                        log.info("Lock acquired: {}", lock);
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error renewing lock: {}", lock, e);
            lock.setFailed();
        }

        return lock;
    }

    @Override
    public Lock releaseLock(@NonNull Lock lock, long now) {
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
                    lock.setCleared();
                    log.info("Lock released: {}", lock);
                } else {
                    Lock existingLock = snapshot.toObject(Lock.class);

                    if (lock.isExpired(now)) {
                        // Lock is expired, so it is already released
                        lock.setCleared();
                        log.info("Lock released: {}", lock);
                    } else if (!lock.isMatch(existingLock)) {
                        // Lock doesn't match, so it cannot be released
                        lock.setFailed();
                        log.warn("Lock does not match, cannot clear: {}", lock);
                    } else {
                        // Successfully release the lock by deleting the document
                        transaction.delete(docRef);
                        lock.setCleared();
                        log.info("Lock cleared: {}", lock);
                    }
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error renewing lock: {}", lock, e);
            lock.setFailed();
        }

        return lock;
    }
}
