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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing sensitive values before logging.
 * 
 * <p>
 * This is useful when you need to log an identifier for correlation purposes
 * but the raw value should not be exposed in logs (e.g., instance IDs, tokens).
 * </p>
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
final class HashUtil {

    private HashUtil() {
        // Utility class
    }

    /**
     * Computes the SHA-256 hex digest of a string.
     * 
     * @param input the input string
     * @return the hex digest, or "hash_error" if hashing fails
     */
    static String sha256Hex(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }
}
