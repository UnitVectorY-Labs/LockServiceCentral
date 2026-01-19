# locker

Core abstractions for LockServiceCentral.

## Overview

This module provides the common interfaces and classes that define the lock service contract. All backend implementations (memory, Firestore, etcd) implement the `LockService` interface defined in this module, ensuring consistent behavior across different storage backends.

## LockService Interface

The `LockService` interface defines the core operations for distributed lock management:

| Method | Description |
|--------|-------------|
| `getLock(namespace, lockName)` | Retrieves the current state of a lock |
| `acquireLock(lock, now)` | Attempts to acquire a lock |
| `renewLock(lock, now)` | Extends the lease duration of an existing lock |
| `releaseLock(lock, now)` | Releases a held lock |

## Lock Model

The `Lock` class represents a distributed lock with the following properties:

| Property | Type | Description |
|----------|------|-------------|
| `namespace` | String | The namespace under which the lock is managed (3-64 alphanumeric characters with dashes and underscores) |
| `lockName` | String | The unique name of the lock within the namespace (3-64 alphanumeric characters with dashes and underscores) |
| `owner` | String | The owner of the lock, typically derived from the JWT `sub` claim |
| `instanceId` | String | A unique instance identifier provided by the client, acts as a pseudo-secret |
| `leaseDuration` | Long | The duration in seconds for which the lock is held |
| `expiry` | Long | The epoch timestamp (in seconds) when the lock expires |

## LockAction Enum

The `LockAction` enum represents the different states and actions for a lock:

| Action | Description |
|--------|-------------|
| `GET` | Checks the status of a lock |
| `ACQUIRE` | Acquires the lock |
| `RENEW` | Renews the lock |
| `RELEASE` | Releases the lock |
| `FAILED` | The lock action failed |
| `SUCCESS` | The lock action succeeded |

## Lock Semantics

All implementations must adhere to the following semantics:

- **Acquire**: A lock can be acquired if it doesn't exist, is expired, or is already held by the same owner and instanceId
- **Renew**: A lock can only be renewed by the same owner and instanceId, and only if it hasn't expired
- **Release**: A lock can be released by the same owner and instanceId; releasing an expired or non-existent lock succeeds
- **Expiry**: Locks automatically expire after the lease duration, making them available for acquisition by others

## Implementing a Custom Backend

To create a new lock backend implementation:

1. Create a new module (e.g., `locker-mybackend`)
2. Implement the `LockService` interface
3. Create a Spring `@Configuration` class that provides a `LockService` bean
4. Extend the tests from `locker-tests` module's `AbstractLockServiceTest` to verify correct behavior
