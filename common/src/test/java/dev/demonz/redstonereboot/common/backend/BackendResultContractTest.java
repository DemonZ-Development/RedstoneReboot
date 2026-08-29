package dev.demonz.redstonereboot.common.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
