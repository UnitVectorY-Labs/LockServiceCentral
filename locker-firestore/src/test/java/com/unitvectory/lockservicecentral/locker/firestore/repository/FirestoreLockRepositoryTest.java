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
package com.unitvectory.lockservicecentral.locker.firestore.repository;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.unitvectory.lockservicecentral.locker.model.Lock;

/**
 * The FirestoreLockRepository test.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class FirestoreLockRepositoryTest {

    @Test
    public void getLockTest() throws InterruptedException, ExecutionException {
        Firestore mockFirestore = mock(Firestore.class);
        FirestoreLockRepository repository = new FirestoreLockRepository(mockFirestore, "locks");

        // Mock the CollectionReference
        CollectionReference mockCollectionRef = mock(CollectionReference.class);
        when(mockFirestore.collection("locks")).thenReturn(mockCollectionRef);

        // Mock the DocumentReference
        DocumentReference mockDocRef = mock(DocumentReference.class);
        when(mockCollectionRef.document("foo:bar")).thenReturn(mockDocRef);

        // Mock the DocumentSnapshot
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        // Mock the ApiFuture
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> mockFuture = mock(ApiFuture.class);
        when(mockDocRef.get()).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(mockSnapshot);

        // Call the method under test
        Lock lock = repository.getLock("foo", "bar");

        // Verify the result
        assertNull(lock);
    }
}
