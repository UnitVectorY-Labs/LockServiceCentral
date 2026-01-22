# Canonical Structured Logging Library

A reusable Spring MVC library for canonical structured JSON logging where each HTTP request emits exactly one structured log line.

## Overview

This library provides a pattern for structured logging where:

1. **Each HTTP request emits exactly one canonical JSON log line** containing the complete context of the request
2. **Controllers and services can enrich the record** during request processing by adding fields to a request-scoped context
3. **One central initialization point** (servlet filter) and **one central emission point** (interceptor)
4. **Request isolation is guaranteed** via Spring `@RequestScope` beans

## Constraints

### Flat JSON Only

The canonical log line is a single, flat JSON object:
- No nested objects or maps
- No "dot notation" keys
- Arrays are not supported

### Snake Case Keys Only

All field names must be `snake_case` matching this pattern:

```
^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$
```

Invalid keys are silently rejected with a warning log.

### Allowed Value Types

- `String`
- `Number` (Integer, Long, Double, etc.)
- `Boolean`
- `Enum` (stored as name string)
- `Instant` (stored as ISO-8601 string)
- `UUID` (stored as string)

Disallowed types (Map, Collection, POJOs) are silently rejected.

## Components

### CanonicalLogContext

Request-scoped bean for building the log record:

```java
@Autowired
private CanonicalLogContext context;

// Add fields during request processing
context.put("lock_namespace", namespace);
context.put("lock_result", LockResult.SUCCESS);
context.put("backend_duration_ms", duration);
```

### CanonicalInitFilter

`OncePerRequestFilter` that initializes baseline fields at request start:
- `ts_start` - Request start timestamp (ISO-8601)
- `service` - Hardcoded as `lockservicecentral`
- `version` - Taken from build properties
- `env` - Taken from `APP_ENV` environment variable (default `dev`)
- `region` - Taken from `APP_REGION` environment variable (default `local`)
- `kind` - Fixed value "http"
- `request_id` - From `X-Request-Id` header or generated (`req_<uuid>`)
- `http_method` - Request method
- `http_target` - Request URI path
- `http_user_agent` - From `User-Agent` header (truncated)
- `client_ip` - From `X-Forwarded-For` or remote address

### CanonicalEmitInterceptor

`HandlerInterceptor` that finalizes and emits the record exactly once:

**In preHandle:**
- Captures `http_route` (route template)

**In afterCompletion:**
- Sets `ts` (end timestamp)
- Sets `duration_ms`
- Sets `http_status_code`
- Sets `outcome` (`success`, `timeout`, `rejected`, `failure`)
- Emits the JSON record to the `canonical` logger

## Sample Output

```json
{
  "ts_start": "2026-01-20T12:00:00.000Z",
  "service": "lockservicecentral",
  "env": "prod",
  "region": "us-east-1",
  "version": "0.0.1",
  "kind": "http",
  "request_id": "req_550e8400-e29b-41d4-a716-446655440000",
  "http_method": "POST",
  "http_target": "/v1/mynamespace/lock/mylock/acquire",
  "http_route": "/v1/{namespace}/lock/{lockName}/acquire",
  "lock_namespace": "mynamespace",
  "lock_name": "mylock",
  "lock_operation": "acquire",
  "auth_subject": "user@example.com",
  "lock_backend": "memory",
  "backend_duration_ms": 5,
  "lock_result": "success",
  "ts": "2026-01-20T12:00:00.050Z",
  "duration_ms": 50,
  "http_status_code": 200,
  "outcome": "success"
}
```

---

## LockServiceCentral-Specific Usage

This section describes how canonical logging is used specifically in LockServiceCentral.

### Domain Fields (Controller Layer)

| Field | Description |
|-------|-------------|
| `lock_namespace` | The namespace path variable |
| `lock_name` | The lock name path variable |
| `lock_operation` | One of: `get`, `acquire`, `renew`, `release` |
| `auth_subject` | JWT subject or "anonymous" |
| `instance_id_hash` | SHA-256 hash of the instance ID (never log raw instance_id) |
| `requested_lease_duration_sec` | Requested lease duration for POST operations |

### Work Fields (Service Layer)

| Field | Description |
|-------|-------------|
| `lock_backend` | Backend type: `memory`, `firestore`, `etcd` |
| `backend_duration_ms` | Time spent in backend call |
| `lock_result` | Result: `success`, `conflict`, `not_found`, `expired` |
| `computed_expiry_epoch_sec` | Computed expiry timestamp for acquire/renew |

### Outcome Mapping

| HTTP Status | Outcome |
|-------------|---------|
| 2xx | `success` |
| 423 (Locked) | `rejected` |
| 408, 504 | `timeout` |
| 4xx, 5xx | `failure` |
| Exception | `failure` |
