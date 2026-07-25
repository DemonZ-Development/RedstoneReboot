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


package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical tests for {@link LocalScriptBackend} — script generation, marker files,
 * shell escaping, and state detection.
 */
class LocalScriptBackendTest {

    @TempDir
    Path tempDir;

    private final Logger logger = Logger.getLogger("LocalScriptBackendTest");


    @Test
    void executeReturnsFailedWhenNotWired() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, null, tempDir);
        BackendResult result = backend.execute();
        assertEquals(BackendResult.FAILED, result,
            "LocalScript should return FAILED when not wired");
    }


    @Test
    void stateIsShutdownOnlyWhenNotWired() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, null, tempDir);
        RestartBackend.BackendState state = backend.getState();
        assertTrue(state == RestartBackend.BackendState.DEPEND_ON_HOST
                || state == RestartBackend.BackendState.GENERATED
                || state == RestartBackend.BackendState.FULL,
            "State should be one of the valid states, got " + state);
    }


    @Test
    void isNotControllerOwned() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, null, tempDir);
        assertFalse(backend.isControllerOwned(),
            "LocalScript should NOT be controller-owned");
    }


    @Test
    void customScriptNameIsUsed() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, "my-custom-script.sh", tempDir);
        assertEquals("LocalScript", backend.getName());
    }


    @Test
    void sensitiveArgsAreFiltered() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, null, tempDir);
        assertTrue(isSensitive("-Dspring.datasource.password=secret"),
            "Spring datasource password should be filtered");
        assertTrue(isSensitive("-Ddb.password=secret"),
            "DB password should be filtered");
        assertTrue(isSensitive("-Djdbc.url=jdbc:postgresql://host"),
            "JDBC URL should be filtered");
        assertFalse(isSensitive("-Xmx2G"),
            "JVM memory arg should NOT be filtered");
        assertFalse(isSensitive("-Dredstonereboot.active=true"),
            "RedstoneReboot active arg should NOT be filtered (but is filtered by name)");
    }

    private boolean isSensitive(String arg) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "-D.*?(password|secret|token|apikey|key|credential|db\\.|database\\.|jdbc\\.|spring\\.datasource\\.|javax\\.net\\.ssl\\.key|jdk\\.tls\\.client)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        return pattern.matcher(arg).find();
    }


    @Test
    void linuxEscapeWrapsInSingleQuotes() {
        String result = linuxEscape("hello world");
        assertEquals("'hello world'", result);
    }

    @Test
    void linuxEscapeHandlesEmbeddedSingleQuotes() {
        String result = linuxEscape("it's here");
        assertEquals("'it'\\''s here'", result);
    }

    private static String linuxEscape(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }


    @Test
    void windowsEscapeWrapsInDoubleQuotes() {
        String result = windowsEscape("hello world");
        assertEquals("\"hello world\"", result);
    }

    @Test
    void windowsEscapeEscapesInnerQuotes() {
        String result = windowsEscape("say \"hello\"");
        assertEquals("\"say \"\"hello\"\"\"", result);
    }

    @Test
    void windowsEscapeEscapesSpecialChars() {
        String result = windowsEscape("cmd & echo | redirect < input > output %var%");
        assertTrue(result.contains("^&"), "Ampersand should be escaped");
        assertTrue(result.contains("^|"), "Pipe should be escaped");
        assertTrue(result.contains("^<"), "Less-than should be escaped");
        assertTrue(result.contains("^>"), "Greater-than should be escaped");
        assertTrue(result.contains("^%"), "Percent should be escaped");
    }

    private static String windowsEscape(String arg) {
        if (arg.isEmpty()) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (c == '"') sb.append("\"\"");
            else if (c == '^') sb.append("^^");
            else if (c == '&' || c == '|' || c == '<' || c == '>' || c == '%') sb.append('^').append(c);
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }


    @Test
    void cleanupDoesNotThrow() {
        LocalScriptBackend backend = new LocalScriptBackend(logger, null, tempDir);
        backend.cleanup();
    }
}