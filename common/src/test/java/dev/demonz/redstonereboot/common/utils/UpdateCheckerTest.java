package dev.demonz.redstonereboot.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void ignoresOlderLoaderSpecificVersion() {
        assertFalse(UpdateChecker.isNewerVersion("1.6.1", "1.5.0-fabric"));
    }

    @Test
    void treatsSameLoaderSpecificVersionAsCurrent() {
        assertFalse(UpdateChecker.isNewerVersion("1.6.1", "1.6.1-forge"));
    }

    @Test
    void detectsNewerPatchAndMinorVersions() {
        assertTrue(UpdateChecker.isNewerVersion("1.6.1", "1.6.2-fabric"));
        assertTrue(UpdateChecker.isNewerVersion("1.6.1", "1.7.0-neoforge"));
        assertTrue(UpdateChecker.isNewerVersion("1.9.9", "1.10.0"));
    }

    @Test
    void handlesLeadingVersionPrefixAndMissingSegments() {
        assertTrue(UpdateChecker.isNewerVersion("v1.6", "1.6.1"));
        assertFalse(UpdateChecker.isNewerVersion("1.6.0", "v1.6"));
    }

    @Test
    void ignoresUnparseableRemoteVersions() {
        assertFalse(UpdateChecker.isNewerVersion("1.6.1", "release-latest"));
    }
}
