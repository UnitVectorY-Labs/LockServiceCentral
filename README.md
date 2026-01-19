[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Work In Progress](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)](https://guide.unitvectorylabs.com/bestpractices/status/#work-in-progress) [![codecov](https://codecov.io/gh/UnitVectorY-Labs/LockServiceCentral/graph/badge.svg?token=43dxfrEyrp)](https://codecov.io/gh/UnitVectorY-Labs/LockServiceCentral)

# LockServiceCentral

API that provides a simple interface for distributed locking with lease-based locks, supporting multiple backend systems. By leveraging a modular architecture, it ensures flexibility and scalability to meet diverse application needs.

## Overview

In distributed systems, managing concurrent access to shared resources is critical to maintaining data integrity and ensuring seamless operations. **LockServiceCentral** addresses this challenge by providing a REST-based API for acquiring, renewing, and releasing named locks for specific clients. The service employs JWT-based authentication to secure lock operations, though authentication can be disabled if required backend microservices are trusted and secured through other means.

**LockServiceCentral** defers the complex part of distributed locking to backend implementations. This modular approach allows a variety of backend systems to handle the intricate details of locking mechanisms.

## Features

- **Distributed Locking:** Manage access to shared resources across multiple instances and services.
- **Modular Backend Support:** Select the appropriate backend for your specific deployment.
- **JWT Protection:** Secure lock operations with JSON Web Tokens, ensuring locks are only accessible to authorized clients.
- **RESTful Interface:** Standardized API endpoints for lock management.
- **Lease-Based Locking:** Locks are held for a specified duration, automatically expiring if not renewed or released.

## Supported Backends

| Backend | Module | Description |
|---------|--------|-------------|
| Memory | `locker-memory` | In-memory lock storage for single-instance deployments and testing |
| Firestore | `locker-firestore` | Google Cloud Firestore for distributed deployments on GCP |
| etcd | `locker-etcd` | etcd for distributed deployments using Kubernetes or other etcd-based infrastructure |

## Building

Build with a specific backend using Maven profiles:

```bash
# Memory backend (default)
mvn clean package -P memory

# Firestore backend
mvn clean package -P firestore

# etcd backend
mvn clean package -P etcd

# Build all backends for testing
mvn clean package -P everything
```

## Docker

Build Docker images with a specific backend:

```bash
# Memory backend (default)
docker build -t lockservicecentral-memory .

# Firestore backend
docker build --build-arg LOCKER=firestore -t lockservicecentral-firestore .

# etcd backend
docker build --build-arg LOCKER=etcd -t lockservicecentral-etcd .
```

## Configuration

### etcd Backend

Configure the etcd backend using the following properties:

```properties
# etcd endpoints (comma-separated for cluster)
locker.etcd.endpoints=http://etcd:2379

# Key prefix (optional)
locker.etcd.keyPrefix=locks/

# TLS (optional)
locker.etcd.tls.enabled=true
locker.etcd.tls.caCertPath=/path/to/ca.pem
locker.etcd.tls.clientCertPath=/path/to/client.pem
locker.etcd.tls.clientKeyPath=/path/to/client-key.pem

# Authentication (optional)
locker.etcd.auth.username=user
locker.etcd.auth.password=password
```

See [locker-etcd/README.md](locker-etcd/README.md) for detailed configuration options.
