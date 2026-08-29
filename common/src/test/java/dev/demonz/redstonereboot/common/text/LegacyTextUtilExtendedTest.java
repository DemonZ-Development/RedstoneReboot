package dev.demonz.redstonereboot.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTextUtilExtendedTest {

    @Test
    void translatesAmpersandToSection() {
        assertEquals("\u00A7cHello \u00A7aWorld",
            LegacyTextUtil.translateAlternateColorCodes("&cHello &aWorld"));
    }

    @Test
    void onlyTranslatesValidColorCodes() {
        String result = LegacyTextUtil.translateAlternateColorCodes("&cRed &zNotAColor &aGreen");
        assertTrue(result.contains("\u00A7c"), "&c should be translated");
        assertTrue(result.contains("&z"), "&z should NOT be translated");
        assertTrue(result.contains("\u00A7a"), "&a should be translated");
    }

    @Test
    void lowercaseColorCodesWork() {
        assertEquals("\u00A7cRed", LegacyTextUtil.translateAlternateColorCodes("&cRed"));
        assertEquals("\u00A7cRed", LegacyTextUtil.translateAlternateColorCodes("&CRed"));
    }

    @Test
    void formattingCodesWork() {
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&lBold").startsWith("\u00A7l"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&nUnderline").startsWith("\u00A7n"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&oItalic").startsWith("\u00A7o"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&mStrike").startsWith("\u00A7m"));
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&kMagic").startsWith("\u00A7k"));
    }

    @Test
    void resetCodeWorks() {
        assertTrue(LegacyTextUtil.translateAlternateColorCodes("&rReset").startsWith("\u00A7r"));
    }

    @Test
    void translateNullReturnsEmpty() {
        assertEquals("", LegacyTextUtil.translateAlternateColorCodes(null));
    }

    @Test
    void translateEmptyStaysEmpty() {
        assertEquals("", LegacyTextUtil.translateAlternateColorCodes(""));
    }

    @Test
    void stripThenTranslateRoundTrip() {
        String original = "&cRed &aGreen &9Blue";
        String translated = LegacyTextUtil.translateAlternateColorCodes(original);
        String stripped = LegacyTextUtil.stripLegacyFormatting(translated);
        assertEquals("Red Green Blue", stripped);
    }

    @Test
    void standaloneAmpersandPreserved() {
        String result = LegacyTextUtil.translateAlternateColorCodes("Hello & World");
        assertTrue(result.contains("&"), "Standalone & should be preserved");
    }

    @Test
    void doubleAmpersandNotTranslated() {
        String result = LegacyTextUtil.translateAlternateColorCodes("&&cTest");
        assertTrue(result.contains("\u00A7c"), "&c after && should still translate");
    }

    @Test
    void hexColorPrefixWorks() {
        String result = LegacyTextUtil.translateAlternateColorCodes("&xTest");
        assertTrue(result.startsWith("\u00A7x"), "&x should be translated as a valid code");
    }
}
