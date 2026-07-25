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

    /**
     * Translate Bukkit alternate color codes ({@code &amp;}) into section-sign
     * color codes ({@code §}).  Identical in behaviour to
     * {@code ChatColor.translateAlternateColorCodes('&amp;', text)} but
     * without depending on the Bukkit API.
     *
     * @param text the input text, may be {@code null}
     * @return the color-translated text, or an empty string if input is {@code null}
     */
    public static String translateAlternateColorCodes(String text) {
        if (text == null) return "";
        char[] b = text.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = '\u00A7';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }
}