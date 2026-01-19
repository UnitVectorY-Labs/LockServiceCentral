# locker-etcd

etcd backend implementation for LockServiceCentral.

## Overview

This module provides a distributed lock implementation backed by [etcd](https://etcd.io/), a distributed key-value store. It uses the [jetcd](https://github.com/etcd-io/jetcd) client library to communicate with the etcd cluster.

## Configuration

All configuration properties are prefixed with `locker.etcd.*`.

### Connection Properties

| Property | Default | Description |
|----------|---------|-------------|
| `locker.etcd.endpoints` | `http://localhost:2379` | Comma-separated list of etcd endpoints |
| `locker.etcd.requestTimeoutMs` | `5000` | Request timeout in milliseconds |
| `locker.etcd.maxRetries` | `3` | Maximum number of CAS retries for contention |

### Key Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `locker.etcd.keyPrefix` | `locks/` | Prefix for all lock keys in etcd |

The key format is: `{keyPrefix}{namespace}:{lockName}`

### TLS Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `locker.etcd.tls.enabled` | `false` | Enable TLS for etcd connections |
| `locker.etcd.tls.caCertPath` | | Path to CA certificate file |
| `locker.etcd.tls.clientCertPath` | | Path to client certificate file |
| `locker.etcd.tls.clientKeyPath` | | Path to client private key file |

### Authentication

| Property | Default | Description |
|----------|---------|-------------|
| `locker.etcd.auth.username` | | etcd authentication username |
| `locker.etcd.auth.password` | | etcd authentication password |

## Lease Behavior

This implementation uses etcd leases for automatic lock expiration:

- **Acquire**: Creates a new lease with TTL equal to the remaining lock lifetime (`expiry - now`). The lock key is attached to this lease.
- **Renew**: Creates a new lease with extended TTL (`existingExpiry + requestedExtension - now`) and reattaches the lock key. This preserves the existing semantics where renewal extends from the previous expiry, not from "now".
- **Release**: Deletes the lock key. The associated lease will be cleaned up automatically.

If the etcd lease expires (e.g., due to network issues preventing renewal), the lock key is automatically deleted by etcd.

## Atomicity

All lock operations use etcd's transaction API with compare-and-swap (CAS) semantics:

1. Read the current key value and revision
2. Make a decision based on lock semantics
3. Apply changes atomically using a transaction that compares the revision

If a CAS operation fails due to concurrent modifications, the operation is retried up to `maxRetries` times.

## Data Model

Lock data is serialized as JSON and stored in etcd. The stored fields are:

- `namespace`
- `lockName`
- `owner`
- `instanceId`
- `leaseDuration`
- `expiry`

## Building

Build the etcd-enabled API jar:

```bash
mvn clean package -P etcd
```

Build Docker image:

```bash
docker build --build-arg LOCKER=etcd -t lockservicecentral-etcd .
```

## Example Configuration

```properties
# etcd endpoints (comma-separated for cluster)
locker.etcd.endpoints=http://etcd-1:2379,http://etcd-2:2379,http://etcd-3:2379

# Key prefix
locker.etcd.keyPrefix=myapp/locks/

# Timeouts
locker.etcd.requestTimeoutMs=10000
locker.etcd.maxRetries=5

# TLS (optional)
locker.etcd.tls.enabled=true
locker.etcd.tls.caCertPath=/etc/ssl/etcd/ca.pem
locker.etcd.tls.clientCertPath=/etc/ssl/etcd/client.pem
locker.etcd.tls.clientKeyPath=/etc/ssl/etcd/client-key.pem

# Authentication (optional)
locker.etcd.auth.username=lockservice
locker.etcd.auth.password=secret
```
