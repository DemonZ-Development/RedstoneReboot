package dev.demonz.redstonereboot.common.text;

/**
 * Small helpers for dealing with Minecraft legacy section-color strings.
 */
public final class LegacyTextUtil {

    private LegacyTextUtil() {
    }

    public static String stripLegacyFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = input.replace("Â§", "§");
        StringBuilder stripped = new StringBuilder(normalized.length());

        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '§' && index + 1 < normalized.length()) {
                index++;
                continue;
            }
            stripped.append(current);
        }

        return stripped.toString();
    }
}
