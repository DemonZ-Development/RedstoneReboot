package dev.demonz.redstonereboot.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Extended tests for {@link LegacyTextUtil} — color code translation,
 * edge cases, and combined formatting.
 */
class LegacyTextUtilExtendedTest {

    // --- translateAlternateColorCodes basic ---

    @Test
    void translatesAmpersandToSection() {
        assertEquals("\u00A7cHello \u00A7aWorld",
            LegacyTextUtil.translateAlternateColorCodes("&cHello &aWorld"));
    }

    // --- Only translates valid color codes ---

    @Test
    void onlyTranslatesValidColorCodes() {
        // &z is not a valid color code, should stay as &z
        String result = LegacyTextUtil.translateAlternateColorCodes("&cRed &zNotAColor &aGreen");
        assertTrue(result.contains("\u00A7c"), "&c should be translated");
        assertTrue(result.contains("&z"), "&z should NOT be translated");
        assertTrue(result.contains("\u00A7a"), "&a should be translated");
    }

    // --- Lowercase color codes work ---

    @Test
    void lowercaseColorCodesWork() {
        assertEquals("\u00A7cRed", LegacyTextUtil.translateAlternateColorCodes("&cRed"));
        assertEquals("\u00A7cRed", LegacyTextUtil.translateAlternateColorCodes("&CRed"));
    }

    // --- Formatting codes work ---

    @Test
    void formattingCodesWork() {
        // &l = bold, &n = underline, &o = italic, &m = strikethrough, &k = obfuscated
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&lBold").startsWith("\u00A7l"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&nUnderline").startsWith("\u00A7n"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&oItalic").startsWith("\u00A7o"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&mStrike").startsWith("\u00A7m"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&kMagic").startsWith("\u00A7k"));
    }

    // --- Reset code works ---

    @Test
    void resetCodeWorks() {
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&rReset").startsWith("\u00A7r"));
    }

    // --- Null input returns empty string ---

    @Test
    void translateNullReturnsEmpty() {
        assertEquals("", LegacyTextUtil.translateAlternateColorCodes(null));
    }

    // --- Empty string stays empty ---

    @Test
    void translateEmptyStaysEmpty() {
        assertEquals("", LegacyTextUtil.translateAlternateColorCodes(""));
    }

    // --- Strip then translate round-trip ---

    @Test
    void stripThenTranslateRoundTrip() {
        String original = "&cRed &aGreen &9Blue";
        String translated = LegacyTextUtil.translateAlternateColorCodes(original);
        String stripped = LegacyTextUtil.stripLegacyFormatting(translated);
        assertEquals("Red Green Blue", stripped);
    }

    // --- Standalone ampersand at end is preserved ---

    @Test
    void standaloneAmpersandPreserved() {
        String result = LegacyTextUtil.translateAlternateColorCodes("Hello & World");
        assertTrue(result.contains("&"), "Standalone & should be preserved");
    }

    // --- Double ampersand is not translated ---

    @Test
    void doubleAmpersandNotTranslated() {
        // &&c should translate the second &c (first & is just a character)
        String result = LegacyTextUtil.translateAlternateColorCodes("&&cTest");
        // The first & pairs with & to become §& which is invalid,
        // but the second & pairs with c to become §c
        // Actually: &(&)c → the first & sees the second & which is not a valid color char
        // so first & stays, then &c gets translated
        assertTrue(result.contains("\u00A7c"), "&c after && should still translate");
    }

    // --- Hex color code support (&x) ---

    @Test
    void hexColorPrefixWorks() {
        // &x is the hex color prefix in modern Minecraft
        String result = LegacyTextUtil.translateAlternateColorCodes("&xTest");
        assertTrue(result.startsWith("\u00A7x"), "&x should be translated as a valid code");
    }
}
