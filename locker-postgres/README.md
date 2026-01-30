# locker-postgres

PostgreSQL backend implementation for LockServiceCentral.

## Quick Start

Build and run with the Postgres backend:

```bash
mvn clean package -DskipTests -Ppostgres -ntp
SPRING_PROFILES_ACTIVE=postgres LOCKER_POSTGRES_HOST=localhost LOCKER_POSTGRES_PASSWORD=yourpassword AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Or with Docker:

```bash
docker build --build-arg LOCKER=postgres -t lockservicecentral-postgres .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgres \
  -e LOCKER_POSTGRES_HOST=host.docker.internal \
  -e LOCKER_POSTGRES_PASSWORD=yourpassword \
  -e AUTHENTICATION_DISABLED=true \
  lockservicecentral-postgres
```

## Overview

This module provides a distributed lock implementation backed by [PostgreSQL](https://www.postgresql.org/). It uses PostgreSQL's atomic operations (INSERT ... ON CONFLICT, UPDATE, DELETE with RETURNING) to ensure atomic lock operations across distributed instances.

## Configuration

All configuration properties are prefixed with `locker.postgres.*`.

### Connection Properties

| Property | Default | Description |
|----------|---------|-------------|
| `locker.postgres.host` | `localhost` | PostgreSQL server hostname |
| `locker.postgres.port` | `5432` | PostgreSQL server port |
| `locker.postgres.database` | `lockservice` | Database name |
| `locker.postgres.schema` | | Database schema (optional) |
| `locker.postgres.username` | `postgres` | Database username |
| `locker.postgres.password` | | Database password |
| `locker.postgres.ssl` | `false` | Enable SSL connection |
| `locker.postgres.connectionPoolSize` | `10` | Maximum connection pool size |
| `locker.postgres.tableName` | `locks` | Table name for storing locks |

### Environment Variables

All properties can be configured via environment variables using the standard Spring convention (uppercase with underscores):

```bash
LOCKER_POSTGRES_HOST=mydb.example.com
LOCKER_POSTGRES_PORT=5432
LOCKER_POSTGRES_DATABASE=lockservice
LOCKER_POSTGRES_USERNAME=lockuser
LOCKER_POSTGRES_PASSWORD=secretpassword
LOCKER_POSTGRES_SSL=true
LOCKER_POSTGRES_TABLENAME=my_locks
```

## Building

Build the Postgres-enabled API jar:

```bash
mvn clean package -DskipTests -Ppostgres -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```bash
docker build --build-arg LOCKER=postgres -t lockservicecentral-postgres .
```

## PostgreSQL Setup

### Required Table Schema

Create the locks table with the following schema:

```sql
CREATE TABLE IF NOT EXISTS locks (
    lock_id VARCHAR(512) PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    lock_name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    lease_duration BIGINT NOT NULL,
    expiry BIGINT NOT NULL
);

-- Index for efficient expiry-based queries
CREATE INDEX IF NOT EXISTS idx_locks_expiry ON locks (expiry);
```

### Complete Setup Script

```sql
-- Create database (run as superuser)
CREATE DATABASE lockservice;

-- Connect to lockservice database
\c lockservice

-- Create table
CREATE TABLE IF NOT EXISTS locks (
    lock_id VARCHAR(512) PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    lock_name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    lease_duration BIGINT NOT NULL,
    expiry BIGINT NOT NULL
);

-- Create index for expiry queries
CREATE INDEX IF NOT EXISTS idx_locks_expiry ON locks (expiry);

-- Optional: Create a dedicated user
CREATE USER lockuser WITH PASSWORD 'yourpassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON locks TO lockuser;
```

## Implementation Details

### Item Structure

Lock items are stored with the following columns:

- `lock_id`: Primary key in format `{namespace}:{lockName}`
- `namespace`: The lock namespace
- `lock_name`: The lock name
- `owner`: The lock owner
- `instance_id`: The client instance ID
- `lease_duration`: The total accumulated lease duration in seconds
- `expiry`: The expiry timestamp in epoch seconds

### Atomicity

All lock operations use PostgreSQL's atomic SQL statements to ensure fully atomic lock semantics. Each operation performs all condition checks and the mutation in a single SQL statement, eliminating race conditions that would occur with read-then-write patterns.

**Key atomicity guarantees:**

- **Single-statement mutations**: Acquire, renew, and release each complete in a single SQL statement with conditions evaluated atomically by PostgreSQL.
- **Expiry checks in SQL**: Lock expiration is evaluated within SQL WHERE clauses using `EXTRACT(EPOCH FROM now())`, ensuring no time-of-check to time-of-use (TOCTOU) vulnerabilities.
- **RETURNING clause**: All mutations use `RETURNING` to get the result of the operation without a separate query.

### Lock Expiry

Lock expiry is represented as a Unix epoch timestamp (seconds since 1970-01-01 00:00:00 UTC) in the `expiry` column. The expiry is calculated as `now + leaseDuration` when acquiring a lock.

Expiry checks are performed server-side using PostgreSQL's `now()` function to ensure consistent time evaluation across distributed clients.

**Note:** Unlike some other backends, PostgreSQL does not automatically delete expired rows. Consider running a periodic cleanup job if you need to remove stale lock entries:

```sql
-- Optional: Delete expired locks (run periodically)
DELETE FROM locks WHERE expiry < EXTRACT(EPOCH FROM now())::bigint;
```

### Behavior

- **Acquire**: Uses `INSERT ... ON CONFLICT DO UPDATE` with a compound condition:
  - Lock doesn't exist (INSERT succeeds), OR
  - Lock is expired (`expiry < now()`), OR
  - Lock belongs to the same owner/instance (`owner = :owner AND instance_id = :instanceId`)
  
- **Renew**: Uses a single `UPDATE` statement that:
  - Validates the lock exists, is not expired, and matches owner/instance
  - Atomically adds the requested duration to both `lease_duration` and `expiry`
  - Returns the updated values via `RETURNING`
  
- **Release**: Uses a single `DELETE` statement that:
  - Validates ownership (`owner` and `instance_id` must match)
  - Uses `RETURNING` to confirm deletion
  - Treats "lock not found" as success (already released)
  - Treats releasing an expired lock (owned by another) as failure

## Example Configuration

```properties
# PostgreSQL connection
locker.postgres.host=mydb.example.com
locker.postgres.port=5432
locker.postgres.database=lockservice
locker.postgres.schema=public
locker.postgres.username=lockuser
locker.postgres.password=secretpassword

# Enable SSL
locker.postgres.ssl=true

# Connection pool
locker.postgres.connectionPoolSize=20

# Table name
locker.postgres.tableName=my_locks
```

## Local Development with Docker

For local development, you can use Docker to run PostgreSQL:

```bash
# Run PostgreSQL with Docker
docker run -d \
  --name lockservice-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=lockservice \
  -p 5432:5432 \
  postgres:16

# Wait for PostgreSQL to start
sleep 5

# Create the locks table
docker exec -i lockservice-postgres psql -U postgres -d lockservice <<EOF
CREATE TABLE IF NOT EXISTS locks (
    lock_id VARCHAR(512) PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    lock_name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    lease_duration BIGINT NOT NULL,
    expiry BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_locks_expiry ON locks (expiry);
EOF

# Run the application
LOCKER_POSTGRES_HOST=localhost \
LOCKER_POSTGRES_PASSWORD=postgres \
SPRING_PROFILES_ACTIVE=postgres \
AUTHENTICATION_DISABLED=true \
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

## Cleanup

To stop and remove the local PostgreSQL container:

```bash
docker stop lockservice-postgres
docker rm lockservice-postgres
```
