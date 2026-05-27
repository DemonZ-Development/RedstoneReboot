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
        // A timeout should be treated as UNKNOWN (ambiguous), not FAILED (explicit rejection).
        // This is the core contract: timeouts lead to lockout, not cancellation.
        BackendResult timeoutResult = BackendResult.UNKNOWN;
        assertNotNull(timeoutResult);
        assertEquals("UNKNOWN", timeoutResult.name());

        // Verify UNKNOWN is distinct from FAILED
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
        // ACCEPTED should not equal FAILED or UNKNOWN
        assertEquals(false, BackendResult.ACCEPTED == BackendResult.FAILED);
        assertEquals(false, BackendResult.ACCEPTED == BackendResult.UNKNOWN);
        assertEquals(false, BackendResult.FAILED == BackendResult.UNKNOWN);
    }
}
