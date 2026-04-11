package dev.demonz.redstonereboot.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyTextUtilTest {

    @Test
    void stripsStandardSectionFormatting() {
        assertEquals("RedstoneReboot Ready", LegacyTextUtil.stripLegacyFormatting("§cRedstoneReboot §aReady"));
    }

    @Test
    void normalizesMojibakeSectionFormatting() {
        assertEquals("Server Restart", LegacyTextUtil.stripLegacyFormatting("Â§cServer Â§eRestart"));
    }

    @Test
    void handlesNullInputAsEmptyText() {
        assertEquals("", LegacyTextUtil.stripLegacyFormatting(null));
    }
}
