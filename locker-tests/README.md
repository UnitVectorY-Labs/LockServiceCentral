# locker-tests

Test suite for LockServiceCentral lock implementations.

## Overview

This module provides an abstract test class `AbstractLockServiceTest` that contains a comprehensive suite of tests verifying the expected behavior of any `LockService` implementation. All backend implementations (memory, Firestore, etcd) should extend this class to ensure consistent behavior.

## Usage

To use this test suite for a new lock implementation:

1. Add `locker-tests` as a test dependency in your module's `pom.xml`:

```xml
<dependency>
    <groupId>com.unitvectory</groupId>
    <artifactId>locker-tests</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

2. Create a test class that extends `AbstractLockServiceTest`:

```java
public class MyLockServiceTest extends AbstractLockServiceTest {

    @Override
    protected LockService createLockService() {
        return new MyLockService();
    }
}
```

3. Run the tests:

```bash
mvn test
```

## Test Coverage

The test suite covers the following scenarios:

### Basic Operations

- Get lock (not found, exists, expired)
- Acquire lock (new, existing owner, expired, conflict)
- Renew lock (success, not found, expired, owner mismatch)
- Release lock (success, not found, expired, owner mismatch)

### Edge Cases

- Null parameter handling (NPE expected)
- Sequential operations (acquire → renew → release)
- Multiple locks in different namespaces
- Acquiring lock at same timestamp as existing lock
- Acquiring lock in the past (relative to existing lock)
- Releasing already released locks

### Validation

Each test validates:

- Correct `LockAction` in response
- Correct `success` flag
- Proper clearing of sensitive fields on failure/release
- Correct `leaseDuration` and `expiry` calculations for renewals
