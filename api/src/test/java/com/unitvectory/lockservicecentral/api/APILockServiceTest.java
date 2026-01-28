/*
 * Copyright 2024 the original author or authors.
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
package com.unitvectory.lockservicecentral.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitvectory.consistgen.epoch.SettableEpochTimeProvider;
import com.unitvectory.fileparamunit.ListFileSource;
import com.unitvectory.lockservicecentral.api.controller.LockController;
import com.unitvectory.lockservicecentral.locker.LockService;
import com.unitvectory.lockservicecentral.locker.memory.MemoryLockService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

/**
 * The parameterized tests for the Spring Boot API
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(profiles = { "time-disabled", "uuid-disabled" })
@WebMvcTest(value = LockController.class, properties = { "authentication.disabled=true" })
public class APILockServiceTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SettableEpochTimeProvider settableEpochTimeProvider;

    @Autowired
    private LockService lockService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void reset() {
        // The memory lock service needs to be cleared between tests
        if (lockService instanceof MemoryLockService) {
            ((MemoryLockService) this.lockService).clear();
        }
    }

    @ParameterizedTest
    @ListFileSource(resources = "/tests/", fileExtension = ".json", recurse = true)
    public void exampleTest(String fileName) throws Exception {
        JsonNode rootNode = objectMapper.readTree(new File(fileName));

        for (JsonNode node : rootNode) {
            // Grab all of the required fields
            assertTrue(node.has("verb"), "Missing verb");
            String verb = node.get("verb").asText();
            assertTrue(node.has("path"), "Missing path");
            String path = node.get("path").asText();
            assertTrue(node.has("status"), "Missing status");
            int status = node.get("status").asInt();
            assertTrue(node.has("response"), "Missing response");
            String response = node.get("response").toString();

            if (verb.equals("GET")) {
                // Run the GET request
                mockMvc.perform(MockMvcRequestBuilders.get(path))
                        .andExpect(MockMvcResultMatchers.status().is(status))
                        .andExpect(MockMvcResultMatchers.content().json(response, JsonCompareMode.STRICT));
            } else if (verb.equals("POST")) {
                assertTrue(node.has("request"), "Missing request");
                String request = node.get("request").toString();

                assertTrue(node.has("now"), "Missing now");
                long now = node.get("now").asLong();

                // Set now for the static time service used for testing
                settableEpochTimeProvider.setEpochTimeSeconds(now);

                // Run the POST request
                mockMvc.perform(MockMvcRequestBuilders.post(path)
                        .contentType("application/json")
                        .content(request))
                        .andExpect(MockMvcResultMatchers.status().is(status))
                        .andExpect(MockMvcResultMatchers.content().json(response, JsonCompareMode.STRICT));
            } else {
                fail("Unknown verb: " + verb);
            }
        }
    }
}
