# locker-dynamodb

AWS DynamoDB backend implementation for LockServiceCentral.

## Quick Start

Build and run with the DynamoDB backend:

```bash
mvn clean package -DskipTests -Pdynamodb -ntp
SPRING_PROFILES_ACTIVE=dynamodb LOCKER_DYNAMODB_REGION=us-east-1 AUTHENTICATION_DISABLED=true java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Or with Docker:

```bash
docker build --build-arg LOCKER=dynamodb -t lockservicecentral-dynamodb .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dynamodb \
  -e LOCKER_DYNAMODB_REGION=us-east-1 \
  -e AUTHENTICATION_DISABLED=true \
  lockservicecentral-dynamodb
```

## Overview

This module provides a distributed lock implementation backed by [AWS DynamoDB](https://aws.amazon.com/dynamodb/). It uses DynamoDB's conditional write operations to ensure atomic lock operations across distributed instances.

## Configuration

All configuration properties are prefixed with `locker.dynamodb.*`.

### DynamoDB Properties

| Property | Default | Description |
|----------|---------|-------------|
| `locker.dynamodb.tableName` | `locks` | DynamoDB table name for storing locks |
| `locker.dynamodb.region` | `us-east-1` | AWS region for DynamoDB |
| `locker.dynamodb.endpoint` | | Custom endpoint URL (optional, for local development) |

### AWS Credentials

| Property | Default | Description |
|----------|---------|-------------|
| `locker.dynamodb.accessKeyId` | | AWS access key ID (optional) |
| `locker.dynamodb.secretAccessKey` | | AWS secret access key (optional) |

**Note:** When running on AWS (ECS, EKS, Lambda, etc.), credentials are automatically detected from the environment using the default credentials provider chain. For local development, you can either:
1. Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables
2. Configure the properties above
3. Use AWS CLI credentials (`~/.aws/credentials`)

## Building

Build the DynamoDB-enabled API jar:

```bash
mvn clean package -DskipTests -Pdynamodb -ntp
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```bash
docker build --build-arg LOCKER=dynamodb -t lockservicecentral-dynamodb .
```

## DynamoDB Setup

### Required Table Configuration

Create a DynamoDB table with the following schema:

1. **Table Name**: `locks` (or your configured table name)
2. **Partition Key**: `lockId` (String)
3. **Billing Mode**: On-Demand or Provisioned (based on your needs)

Example AWS CLI command to create the table:

```bash
aws dynamodb create-table \
  --table-name locks \
  --attribute-definitions AttributeName=lockId,AttributeType=S \
  --key-schema AttributeName=lockId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### TTL Configuration (Recommended)

Configure a TTL (Time To Live) attribute on the `expiry` field to automatically delete expired lock items:

```bash
aws dynamodb update-time-to-live \
  --table-name locks \
  --time-to-live-specification "Enabled=true, AttributeName=expiry" \
  --region us-east-1
```

This ensures expired locks are automatically cleaned up by DynamoDB, typically within 48 hours of expiration.

## Implementation Details

### Item Structure

Lock items are stored with the following attributes:

- `lockId`: Partition key in format `{namespace}:{lockName}`
- `namespace`: The lock namespace
- `lockName`: The lock name
- `owner`: The lock owner
- `instanceId`: The client instance ID
- `leaseDuration`: The lease duration in seconds
- `expiry`: The expiry timestamp in epoch seconds (used for TTL)

### Atomicity

All lock operations use DynamoDB's conditional write operations to ensure fully atomic lock semantics. Each operation performs all condition checks and the mutation in a single atomic DynamoDB call, eliminating race conditions that would occur with read-then-write patterns.

**Key atomicity guarantees:**

- **Single-operation mutations**: Acquire, renew, and release each complete in a single DynamoDB API call with conditions evaluated atomically.
- **Expiry checks in conditions**: Lock expiration is evaluated within DynamoDB's condition expressions using the current timestamp, ensuring no time-of-check to time-of-use (TOCTOU) vulnerabilities.
- **ReturnValuesOnConditionCheckFailure**: For release operations, when the condition fails, the existing item is returned atomically with the exception, avoiding a separate read.

### Lock Expiry

Lock expiry is handled in two ways:

1. **Condition-level**: Expiry is checked atomically within DynamoDB condition expressions during lock operations
2. **DynamoDB TTL**: If configured, DynamoDB automatically deletes expired items based on the `expiry` field

### Behavior

- **Acquire**: Uses a single conditional `PutItem` operation with a compound condition:
  - Lock doesn't exist (`attribute_not_exists`), OR
  - Lock is expired (`expiry < :now`), OR
  - Lock belongs to the same owner/instance (`owner = :owner AND instanceId = :instanceId`)
  
- **Renew**: Uses a single conditional `UpdateItem` operation that:
  - Validates the lock exists (via `attribute_exists`), is not expired, and matches owner/instance
  - Atomically adds the requested duration to both `leaseDuration` and `expiry`
  - Returns the updated values via `ReturnValues.ALL_NEW`
  
- **Release**: Uses a single conditional `DeleteItem` operation that:
  - Validates ownership (`owner` and `instanceId` must match)
  - Allows releasing expired locks owned by the same owner
  - Uses `ReturnValuesOnConditionCheckFailure.ALL_OLD` to distinguish between non-existent locks (success) and locks owned by others (conflict)

## Example Configuration

```properties
# DynamoDB table
locker.dynamodb.tableName=my-locks

# AWS region
locker.dynamodb.region=us-west-2

# Custom endpoint (for local DynamoDB)
locker.dynamodb.endpoint=http://localhost:8000

# Static credentials (optional, not recommended for production)
locker.dynamodb.accessKeyId=YOUR_ACCESS_KEY
locker.dynamodb.secretAccessKey=YOUR_SECRET_KEY
```

## Local Development with DynamoDB Local

For local development, you can use [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html):

```bash
# Run DynamoDB Local with Docker
docker run -p 8000:8000 amazon/dynamodb-local

# Create the locks table
aws dynamodb create-table \
  --table-name locks \
  --attribute-definitions AttributeName=lockId,AttributeType=S \
  --key-schema AttributeName=lockId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000 \
  --region us-east-1

# Run the application
LOCKER_DYNAMODB_ENDPOINT=http://localhost:8000 \
LOCKER_DYNAMODB_REGION=us-east-1 \
SPRING_PROFILES_ACTIVE=dynamodb \
AUTHENTICATION_DISABLED=true \
java -jar ./api/target/api-0.0.1-SNAPSHOT.jar
```
