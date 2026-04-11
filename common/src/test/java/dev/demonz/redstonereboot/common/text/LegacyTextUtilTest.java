package dev.demonz.redstonereboot.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyTextUtilTest {

    @Test
    void stripsStandardSectionFormatting() {
        assertEquals(
            "RedstoneReboot Ready",
            LegacyTextUtil.stripLegacyFormatting("\u00A7cRedstoneReboot \u00A7aReady")
        );
    }

    @Test
    void normalizesMojibakeSectionFormatting() {
        assertEquals(
            "Server Restart",
            LegacyTextUtil.stripLegacyFormatting("\u00C2\u00A7cServer \u00C2\u00A7eRestart")
        );
    }

    @Test
    void normalizesDoubleMojibakeSectionFormatting() {
        assertEquals(
            "Server Restart",
            LegacyTextUtil.stripLegacyFormatting("\u00C3\u201A\u00C2\u00A7cServer \u00C3\u201A\u00C2\u00A7eRestart")
        );
    }

    @Test
    void handlesNullInputAsEmptyText() {
        assertEquals("", LegacyTextUtil.stripLegacyFormatting(null));
    }
}
