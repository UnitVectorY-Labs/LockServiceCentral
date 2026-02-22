# locker-firestore

Google Cloud Firestore backend implementation for LockServiceCentral.

## Quick Start

Build and run with the Firestore backend:

```bash
mvn clean package -DskipTests -Pfirestore -ntp
SPRING_PROFILES_ACTIVE=firestore GOOGLE_CLOUD_PROJECT=your-project-id AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Or with Docker:

```bash
docker build --build-arg LOCKER=firestore -t lockservicecentral-firestore .
docker run -p 8080:8080 \
  -e GOOGLE_CLOUD_PROJECT=your-project-id \
  -e AUTHENTICATION_DISABLED=true \
  lockservicecentral-firestore
```

## Overview

This module provides a distributed lock implementation backed by [Google Cloud Firestore](https://cloud.google.com/firestore). It uses Firestore transactions to ensure atomic lock operations across distributed instances.

## Configuration

All configuration properties are prefixed with `locker.firestore.*`.

### Firestore Properties

| Property | Default | Description |
|----------|---------|-------------|
| `locker.firestore.collection` | `locks` | Firestore collection name for storing locks |
| `locker.firestore.database` | `(default)` | Firestore database ID |

### GCP Properties

| Property | Default | Description |
|----------|---------|-------------|
| `google.cloud.project` | | GCP project ID (required) |

**Note:** When running on GCP (Cloud Run, GKE, etc.), the project ID is automatically detected from the environment. For local development, set the `GOOGLE_CLOUD_PROJECT` environment variable.

## Building

Build the Firestore-enabled API jar:

```bash
mvn clean package -DskipTests -Pfirestore -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```bash
docker build --build-arg LOCKER=firestore -t lockservicecentral-firestore .
```

## Firestore Setup

### Required Configuration

1. Create a Firestore database in your GCP project (Native mode recommended)
2. Configure authentication:
   - On GCP: Use default service account or Workload Identity
   - Locally: Use `gcloud auth application-default login` or set `GOOGLE_APPLICATION_CREDENTIALS`

### TTL Policy (Recommended)

Configure a TTL policy on the `ttl` field to automatically delete expired lock documents:

1. In the GCP Console, navigate to Firestore
2. Select the `locks` collection (or your configured collection name)
3. Create a TTL policy on the `ttl` field

This ensures expired locks are automatically cleaned up by Firestore.

## Implementation Details

### Document Structure

Lock documents are stored with the document ID `{namespace}:{lockName}` and contain:

- `namespace`: The lock namespace
- `lockName`: The lock name
- `owner`: The lock owner
- `instanceId`: The client instance ID
- `leaseDuration`: The lease duration in seconds
- `expiry`: The expiry timestamp in epoch seconds
- `ttl`: A Firestore Timestamp used for automatic document expiration

### Atomicity

All lock operations use Firestore transactions to ensure atomic compare-and-swap semantics. This guarantees that concurrent lock operations are handled correctly.

### Lock Expiry

Lock expiry is handled in two ways:

1. **Application-level**: The application checks the `expiry` field during operations
2. **Firestore TTL**: If configured, Firestore automatically deletes expired documents based on the `ttl` field

### Behavior

- **Acquire**: Uses a transaction to check if the lock is available, expired, or owned by the same client before creating/updating
- **Renew**: Extends `leaseDuration` and `expiry` by adding the requested lease duration to the existing values, updates `ttl`
- **Release**: Deletes the lock document within a transaction after verifying ownership

## Example Configuration

```properties
# GCP project (required for local development)
google.cloud.project=my-project-id

# Custom Firestore database
locker.firestore.database=my-database

# Custom collection name
locker.firestore.collection=my-locks
```
