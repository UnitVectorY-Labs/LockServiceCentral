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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.unitvectory.lockservicecentral.locker.Lock;
import com.unitvectory.lockservicecentral.logging.CanonicalLogContext;

/**
 * The PostgresLockService test.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class PostgresLockServiceTest {

    private ObjectProvider<CanonicalLogContext> createNoOpProvider() {
        return new ObjectProvider<>() {
            @Override
            public CanonicalLogContext getObject() {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getObject(Object... args) {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getIfAvailable() {
                return new CanonicalLogContext();
            }

            @Override
            public CanonicalLogContext getIfUnique() {
                return new CanonicalLogContext();
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getLockTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        PostgresLockService service = new PostgresLockService(mockJdbcTemplate, "locks", createNoOpProvider());

        // Mock empty result
        when(mockJdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Collections.emptyList());

        // Call the method under test
        Lock lock = service.getLock("foo", "bar");

        // Verify the result
        assertNull(lock);
    }

    @Test
    public void invalidTableNameNullTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> {
            new PostgresLockService(mockJdbcTemplate, null, createNoOpProvider());
        });
    }

    @Test
    public void invalidTableNameEmptyTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> {
            new PostgresLockService(mockJdbcTemplate, "", createNoOpProvider());
        });
    }

    @Test
    public void invalidTableNameSqlInjectionTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> {
            new PostgresLockService(mockJdbcTemplate, "locks; DROP TABLE users;", createNoOpProvider());
        });
    }

    @Test
    public void invalidTableNameSpecialCharsTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> {
            new PostgresLockService(mockJdbcTemplate, "my-locks", createNoOpProvider());
        });
    }

    @Test
    public void validTableNameWithUnderscoreTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        // Should not throw
        new PostgresLockService(mockJdbcTemplate, "my_locks", createNoOpProvider());
    }

    @Test
    public void validTableNameStartingWithUnderscoreTest() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        // Should not throw
        new PostgresLockService(mockJdbcTemplate, "_locks", createNoOpProvider());
    }
}
