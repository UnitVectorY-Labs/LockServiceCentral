# api

REST API for LockServiceCentral.

## Quick Start

Build and run with the memory backend for local testing:

```bash
mvn clean package -DskipTests -Pmemory -ntp
SPRING_PROFILES_ACTIVE=memory AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Or with Docker:

```bash
docker build -t lockservicecentral-memory .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=memory -e AUTHENTICATION_DISABLED=true lockservicecentral-memory
```

Once running, access the Swagger UI at [http://localhost:8080/](http://localhost:8080/) to explore and test the API.

## Overview

This module provides the REST API for LockServiceCentral, offering endpoints for acquiring, renewing, and releasing distributed locks. The API is documented via Swagger UI, available at the root of the running service.

Refer to the Swagger UI for detailed API endpoint documentation, request/response schemas, and interactive testing.

## Configuration

### Authentication

| Environment Variable / Property | Default | Description |
|--------------------------------|---------|-------------|
| `AUTHENTICATION_DISABLED` / `authentication.disabled` | `false` | When `true`, disables all authentication. Useful for local development. |
| `JWT_ISSUER` / `jwt.issuer` | | The expected JWT issuer. If set without `jwt.jwks`, OpenID Connect discovery is used. |
| `JWT_JWKS` / `jwt.jwks` | | The JWKS endpoint URL for JWT validation. Takes priority over issuer discovery. |
| `JWT_AUDIENCE` / `jwt.audience` | | Optional expected audience claim in the JWT. |

**Authentication Modes:**

1. **JWT Authentication** (recommended for production): Set `jwt.issuer` and optionally `jwt.jwks` and `jwt.audience`
2. **Disabled Authentication** (for local testing): Set `authentication.disabled=true`
3. **Default** (unconfigured): API endpoints require authentication but no valid configuration exists, making the API unusable

### Spring Profiles

| Profile | Description |
|---------|-------------|
| `memory` | Uses the in-memory lock backend (single instance only) |
| `firestore` | Uses Google Cloud Firestore as the lock backend |
| `etcd` | Uses etcd as the lock backend |

Set the profile using `SPRING_PROFILES_ACTIVE` environment variable or `spring.profiles.active` property.

## Building

Build with a specific backend:

```bash
# Memory backend
mvn clean package -DskipTests -Pmemory -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar

# Firestore backend
mvn clean package -DskipTests -Pfirestore -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar

# etcd backend
mvn clean package -DskipTests -Petcd -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Build Docker images:

```bash
# Memory backend (default)
docker build -t lockservicecentral-memory .

# Firestore backend
docker build --build-arg LOCKER=firestore -t lockservicecentral-firestore .

# etcd backend
docker build --build-arg LOCKER=etcd -t lockservicecentral-etcd .
```

## Running Locally

For local development with authentication disabled:

```bash
# Using Java
SPRING_PROFILES_ACTIVE=memory AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar

# Using Docker
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=memory -e AUTHENTICATION_DISABLED=true lockservicecentral-memory
```

For local development with JWT authentication (example using a local issuer):

```bash
SPRING_PROFILES_ACTIVE=memory JWT_ISSUER=https://your-issuer.example.com java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```
