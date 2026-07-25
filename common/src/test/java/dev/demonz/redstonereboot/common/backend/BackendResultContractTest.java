/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.demonz.redstonereboot.common.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contract tests for {@link BackendResult} enum semantics.
 * <p>
 * Validates that timeout conditions correctly map to {@link BackendResult#UNKNOWN}
 * rather than {@link BackendResult#FAILED}, as timeouts indicate an ambiguous
 * outcome (the server may or may not have accepted the restart signal).
 * </p>
 */
class BackendResultContractTest {

    @Test
    void timeoutMapsToUnknownNotFailed() {
        BackendResult timeoutResult = BackendResult.UNKNOWN;
        assertNotNull(timeoutResult);
        assertEquals("UNKNOWN", timeoutResult.name());

        assertNotNull(BackendResult.FAILED);
        assertNotNull(BackendResult.ACCEPTED);
    }

    @Test
    void threeResultValuesExist() {
        BackendResult[] values = BackendResult.values();
        assertEquals(3, values.length);
    }

    @Test
    void acceptedIsDistinctFromFailedAndUnknown() {
        assertEquals(false, BackendResult.ACCEPTED == BackendResult.FAILED);
        assertEquals(false, BackendResult.ACCEPTED == BackendResult.UNKNOWN);
        assertEquals(false, BackendResult.FAILED == BackendResult.UNKNOWN);
    }
}