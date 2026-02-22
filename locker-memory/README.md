# locker-memory

In-memory backend implementation for LockServiceCentral.

## Quick Start

Build and run with the memory backend:

```bash
mvn clean package -DskipTests -Pmemory -ntp
SPRING_PROFILES_ACTIVE=memory AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Or with Docker:

```bash
docker build -t lockservicecentral-memory .
docker run -p 8080:8080 -e AUTHENTICATION_DISABLED=true lockservicecentral-memory
```

## Overview

This module provides an in-memory lock implementation using a `ConcurrentHashMap`. It is designed for:

- **Single-instance deployments**
- **Local development and testing**
- **Unit testing other components**

**Important:** This implementation is not suitable for distributed deployments as locks are stored in-memory and not shared across instances. For distributed deployments, use the Firestore or etcd backends.

## Configuration

The memory backend has no additional configuration properties.

## Building

Build the memory-enabled API jar:

```bash
mvn clean package -DskipTests -Pmemory -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```bash
docker build -t lockservicecentral-memory .
```

## Implementation Details

### Lock Storage

Locks are stored in a `ConcurrentHashMap` with keys in the format `{namespace}:{lockName}`.

### Thread Safety

All lock operations are synchronized using Lombok's `@Synchronized` annotation on the underlying `ConcurrentHashMap` to ensure thread-safe access.

### Lock Expiry

Lock expiry is checked at operation time. Expired locks are treated as available for acquisition. Unlike the etcd backend, expired locks are not automatically removed from memory until a new operation is performed on them.

### Behavior

- **Acquire**: Succeeds if no lock exists, the existing lock is expired, or the existing lock matches the same owner and instanceId
- **Renew**: Extends `leaseDuration` and `expiry` by adding the requested lease duration to the existing values
- **Release**: Removes the lock from memory; releasing an expired or non-existent lock succeeds

### Limitations

- **No persistence**: All locks are lost when the application restarts
- **Single instance only**: Locks are not shared across multiple running instances
- **No automatic cleanup**: Expired locks remain in memory until accessed
