package dev.demonz.redstonereboot.common.text;

/**
 * Small helpers for dealing with Minecraft legacy section-color strings.
 */
public final class LegacyTextUtil {

    private static final String SECTION = "\u00A7";
    private static final String MOJIBAKE_SECTION = "\u00C2\u00A7";
    private static final String DOUBLE_MOJIBAKE_SECTION = "\u00C3\u201A\u00C2\u00A7";

    private LegacyTextUtil() {
    }

    public static String stripLegacyFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = input
            .replace(DOUBLE_MOJIBAKE_SECTION, SECTION)
            .replace(MOJIBAKE_SECTION, SECTION);
        StringBuilder stripped = new StringBuilder(normalized.length());

        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '\u00A7' && index + 1 < normalized.length()) {
                index++;
                continue;
            }
            stripped.append(current);
        }

        return stripped.toString();
    }
}
