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
package com.unitvectory.lockservicecentral.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for HashUtil.
 *
 * @author Jared Hatfield (UnitVectorY Labs)
 */
class HashUtilTest {

    @Test
    void testSha256Hex() {
        // Known SHA-256 hash for "hello"
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        String result = HashUtil.sha256Hex("hello");
        assertEquals(expected, result);
    }

    @Test
    void testSha256HexEmptyString() {
        // Known SHA-256 hash for empty string
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String result = HashUtil.sha256Hex("");
        assertEquals(expected, result);
    }

    @Test
    void testSha256HexNull() {
        assertNull(HashUtil.sha256Hex(null));
    }

    @Test
    void testSha256HexConsistency() {
        String input = "myinstanceid";
        String result1 = HashUtil.sha256Hex(input);
        String result2 = HashUtil.sha256Hex(input);
        assertEquals(result1, result2);
        assertEquals(64, result1.length()); // SHA-256 produces 64 hex characters
    }

    @Test
    void testSha256HexDifferentInputs() {
        String result1 = HashUtil.sha256Hex("input1");
        String result2 = HashUtil.sha256Hex("input2");
        assertNotEquals(result1, result2);
    }
}
