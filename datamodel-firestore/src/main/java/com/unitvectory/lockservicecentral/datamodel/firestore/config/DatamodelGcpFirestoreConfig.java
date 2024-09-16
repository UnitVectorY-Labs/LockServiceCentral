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
package com.unitvectory.lockservicecentral.datamodel.firestore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * The data model config for GCP Firestore
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Configuration
@Profile("datamodel-firestore")
public class DatamodelGcpFirestoreConfig {

	@Value("${google.cloud.project}")
	private String projectId;

	@Value("${sac.datamodel.firestore.database:(default)}")
	private String firestoreDatabase;

	@Bean
	public Firestore firestore() {
		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
				.setDatabaseId(this.firestoreDatabase)
				.setProjectId(this.projectId).build();
		return firestoreOptions.getService();
	}
}
