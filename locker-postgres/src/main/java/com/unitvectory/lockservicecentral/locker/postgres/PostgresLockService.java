/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.unitvectory.lockservicecentral.locker.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of {@link LockService} providing distributed lock functionality.
 *
 * <p>This implementation uses PostgreSQL's atomic operations (INSERT ... ON CONFLICT, UPDATE,
 * DELETE with RETURNING) to ensure atomic lock operations across distributed instances.
 * All lock mutations (acquire, renew, release) are performed using single atomic SQL statements
 * with all conditions evaluated server-side, eliminating race conditions that would occur
 * with read-then-write patterns.</p>
 *
 * <h2>Atomicity Guarantees</h2>
 * <ul>
 *   <li><b>Acquire</b>: Single INSERT ... ON CONFLICT DO UPDATE with conditions that succeed
 *       only if the lock doesn't exist, is expired, or belongs to the same owner/instance</li>
 *   <li><b>Renew</b>: Single UPDATE with conditions that succeed only if the lock exists,
 *       is not expired, and matches the owner/instance</li>
 *   <li><b>Release</b>: Single DELETE with conditions that succeed only if the lock
 *       matches the owner/instance</li>
 * </ul>
 *
 * <h2>Lock Expiry Handling</h2>
 * <p>Lock expiry is checked atomically within SQL statements using Postgres's now() function
 * via extract(epoch from now()). This ensures that expiry checks are evaluated at the database
 * level and cannot be affected by clock skew between read and write operations.</p>
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Slf4j
public class PostgresLockService implements LockService {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final ObjectProvider<CanonicalLogContext> canonicalLogContextProvider;

    /**
     * RowMapper for converting result sets to Lock objects.
     */
    private final RowMapper<Lock> lockRowMapper = new RowMapper<>() {
        @Override
        public Lock mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> map = new HashMap<>();
            map.put("namespace", rs.getString("namespace"));
            map.put("lockName", rs.getString("lock_name"));
            map.put("owner", rs.getString("owner"));
            map.put("instanceId", rs.getString("instance_id"));
            map.put("leaseDuration", rs.getLong("lease_duration"));
            map.put("expiry", rs.getLong("expiry"));
            return new Lock(map);
        }
    };

    /**
     * Constructs a new PostgresLockService.
     *
     * @param dataSource the DataSource for Postgres connections
     * @param tableName the Postgres table name for locks
     * @param canonicalLogContextProvider provider for the canonical log context
     */
    public PostgresLockService(DataSource dataSource, String tableName,
            ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.tableName = tableName;
        this.canonicalLogContextProvider = canonicalLogContextProvider;
    }

    /**
     * Constructs a new PostgresLockService with a pre-configured JdbcTemplate.
     *
     * @param jdbcTemplate the JdbcTemplate for database operations
     * @param tableName the Postgres table name for locks
     * @param canonicalLogContextProvider provider for the canonical log context
     */
    PostgresLockService(JdbcTemplate jdbcTemplate, String tableName,
            ObjectProvider<CanonicalLogContext> canonicalLogContextProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.canonicalLogContextProvider = canonicalLogContextProvider;
    }

    /**
     * Records the lock service outcome to the canonical log context.
     *
     * @param outcome the screaming snake case outcome
     */
    private void recordOutcome(String outcome) {
        try {
            CanonicalLogContext context = canonicalLogContextProvider.getObject();
            context.put("lock_service_outcome", outcome);
        } catch (Exception e) {
            // Don't break lock operations if logging fails
        }
    }

    /**
     * Generates the database primary key for a lock based on namespace and lock name.
     *
     * @param namespace the namespace
     * @param lockName  the lock name
     * @return the primary key value
     */
    private String generateKey(String namespace, String lockName) {
        return namespace + ":" + lockName;
    }

    @Override
    public Lock getLock(@NonNull String namespace, @NonNull String lockName) {
        String key = generateKey(namespace, lockName);

        try {
            String sql = "SELECT namespace, lock_name, owner, instance_id, lease_duration, expiry " +
                    "FROM " + tableName + " WHERE lock_id = ?";

            List<Lock> results = jdbcTemplate.query(sql, lockRowMapper, key);

            if (results.isEmpty()) {
                return null;
            }

            return results.get(0);

        } catch (Exception e) {
            log.error("Error getting lock: {} {}", namespace, lockName, e);
            return null;
        }
    }

    @Override
    public Lock acquireLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // Atomic INSERT ... ON CONFLICT DO UPDATE with conditions:
            // - Insert if row doesn't exist
            // - Update if row exists AND (expired OR same owner/instance)
            // The WHERE clause in DO UPDATE controls whether the update happens
            // Using Postgres now() for server-side time evaluation
            String sql = "INSERT INTO " + tableName +
                    " (lock_id, namespace, lock_name, owner, instance_id, lease_duration, expiry) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (lock_id) DO UPDATE SET " +
                    "owner = EXCLUDED.owner, " +
                    "instance_id = EXCLUDED.instance_id, " +
                    "lease_duration = EXCLUDED.lease_duration, " +
                    "expiry = EXCLUDED.expiry " +
                    "WHERE " + tableName + ".expiry < EXTRACT(EPOCH FROM now())::bigint " +
                    "OR (" + tableName + ".owner = EXCLUDED.owner AND " + tableName + ".instance_id = EXCLUDED.instance_id) " +
                    "RETURNING lock_id";

            List<String> result = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getString("lock_id"),
                    key, lock.getNamespace(), lock.getLockName(), lock.getOwner(),
                    lock.getInstanceId(), lock.getLeaseDuration(), lock.getExpiry());

            if (!result.isEmpty()) {
                // Lock was acquired (insert or update succeeded)
                lock.setSuccess();
                recordOutcome("ACQUIRED");
            } else {
                // Conflict: lock exists, is not expired, and belongs to different owner
                lock.setFailed();
                recordOutcome("ACQUIRE_CONFLICT");
            }

        } catch (Exception e) {
            log.error("Error acquiring lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("ACQUIRE_ERROR");
        }

        return lock;
    }

    @Override
    public Lock renewLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // Atomic UPDATE with conditions:
            // - Lock must exist (implicit in UPDATE)
            // - Lock must not be expired (using Postgres now())
            // - Lock must match owner and instance_id
            // Adds leaseDuration to both lease_duration and expiry
            String sql = "UPDATE " + tableName + " SET " +
                    "lease_duration = lease_duration + ?, " +
                    "expiry = expiry + ? " +
                    "WHERE lock_id = ? " +
                    "AND expiry >= EXTRACT(EPOCH FROM now())::bigint " +
                    "AND owner = ? " +
                    "AND instance_id = ? " +
                    "RETURNING namespace, lock_name, owner, instance_id, lease_duration, expiry";

            List<Lock> results = jdbcTemplate.query(sql, lockRowMapper,
                    lock.getLeaseDuration(), lock.getLeaseDuration(),
                    key, lock.getOwner(), lock.getInstanceId());

            if (!results.isEmpty()) {
                // Lock was renewed successfully
                Lock updatedLock = results.get(0);
                lock.setLeaseDuration(updatedLock.getLeaseDuration());
                lock.setExpiry(updatedLock.getExpiry());
                lock.setSuccess();
                recordOutcome("RENEWED");
            } else {
                // Condition failed: lock doesn't exist, is expired, or belongs to different owner
                lock.setFailed();
                recordOutcome("RENEW_CONFLICT");
            }

        } catch (Exception e) {
            log.error("Error renewing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RENEW_ERROR");
        }

        return lock;
    }

    @Override
    public Lock releaseLock(@NonNull Lock originalLock, long now) {
        Lock lock = originalLock.copy();
        String key = generateKey(lock.getNamespace(), lock.getLockName());

        try {
            // First, try to delete with owner/instance match
            // This handles the normal release case
            String deleteSql = "DELETE FROM " + tableName + " " +
                    "WHERE lock_id = ? " +
                    "AND owner = ? " +
                    "AND instance_id = ? " +
                    "RETURNING lock_id";

            List<String> deleteResult = jdbcTemplate.query(deleteSql,
                    (rs, rowNum) -> rs.getString("lock_id"),
                    key, lock.getOwner(), lock.getInstanceId());

            if (!deleteResult.isEmpty()) {
                // Lock was deleted successfully
                lock.setCleared();
                recordOutcome("RELEASED");
            } else {
                // Delete didn't match - check if lock exists and why
                String checkSql = "SELECT namespace, lock_name, owner, instance_id, lease_duration, expiry " +
                        "FROM " + tableName + " WHERE lock_id = ?";
                List<Lock> existing = jdbcTemplate.query(checkSql, lockRowMapper, key);

                if (existing.isEmpty()) {
                    // Lock doesn't exist - treat as success (already released)
                    lock.setCleared();
                    recordOutcome("RELEASED_NOT_FOUND");
                } else {
                    Lock existingLock = existing.get(0);
                    if (existingLock.getExpiry() < now) {
                        // Lock is expired - treat as effectively released
                        lock.setCleared();
                        recordOutcome("RELEASED_EXPIRED");
                    } else {
                        // Lock exists and belongs to different owner, and is not expired
                        lock.setFailed();
                        recordOutcome("RELEASE_CONFLICT");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error releasing lock: {}", lock, e);
            lock.setFailed();
            recordOutcome("RELEASE_ERROR");
        }

        return lock;
    }
}
