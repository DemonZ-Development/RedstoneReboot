package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Restart backend that relies on a local wrapper script.
 */
public class LocalScriptBackend extends SupervisorBackend {

    private static final String RESTART_MARKER = ".redstonereboot_restart";
    private static final List<String> SENSITIVE_ARG_PREFIXES = Arrays.asList(
        "-dpassword", "-dsecret", "-dtoken", "-dapikey", "-dkey", "-dcredential",
        "-ddb.", "-ddatabase.", "-djdbc.", "-dspring.datasource.",
        "-djavax.net.ssl.key", "-djdk.tls.client"
    );

    private final String scriptName;
    private final boolean isWindows;

    public LocalScriptBackend(Logger logger, String customScriptName) {
        super(logger, "LocalScript");
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (customScriptName != null && !customScriptName.trim().isEmpty()) {
            this.scriptName = customScriptName.trim();
        } else {
            this.scriptName = isWindows ? "redstonereboot-start.bat" : "redstonereboot-start.sh";
        }
    }

    @Override
    public void prepare() {
        generateScript(false);
    }

    @Override
    public BackendResult execute() {
        if (!isWired()) {
            logger.warning("LocalScript backend executed but no wiring detected! Server might not restart.");
            return BackendResult.FAILED;
        }

        try {
            Path markerPath = Paths.get(RESTART_MARKER).toAbsolutePath();
            Files.writeString(markerPath, "restart",
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.SYNC);
        } catch (IOException exception) {
            logger.warning("Failed to arm LocalScript restart marker: " + exception.getMessage());
            return BackendResult.FAILED;
        }
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        if (isWired()) {
            return BackendState.FULL;
        }
        if (Files.exists(Paths.get(scriptName).toAbsolutePath())) {
            return BackendState.GENERATED;
        }
        return BackendState.SHUTDOWN_ONLY;
    }

    private boolean isWired() {
        // Wiring proof: -D property or Env Var or Marker File
        if (Boolean.getBoolean("redstonereboot.active")) return true;
        if ("1".equals(System.getenv("REDSTONEREBOOT_ACTIVE"))) return true;
        return Files.exists(Paths.get(".redstonereboot_wired").toAbsolutePath());
    }

    public void generateScript(boolean overwrite) {
        Path path = Paths.get(scriptName).toAbsolutePath();
        if (Files.exists(path) && !overwrite) {
            return;
        }

        try {
            String content = isWindows ? getWindowsTemplate() : getLinuxTemplate();
            Files.writeString(path, content);
            if (!isWindows) {
                path.toFile().setExecutable(true);
            }
            logger.info("Generated restart wrapper: " + scriptName);
        } catch (IOException e) {
            logger.warning("Failed to generate restart script: " + e.getMessage());
        }
    }

    private String getLinuxTemplate() {
        String absoluteMarker = Paths.get(RESTART_MARKER).toAbsolutePath().toString().replace("\\", "/");
        return "#!/bin/bash\n" +
               "# RedstoneReboot Auto-Restart Wrapper\n" +
               "while true; do\n" +
               "    " + detectStartupCommand() + "\n" +
               "    if [ ! -f \"" + absoluteMarker + "\" ]; then\n" +
               "        exit 0\n" +
               "    fi\n" +
               "    rm -f \"" + absoluteMarker + "\"\n" +
               "    echo \"Server stopped. Restarting in 5 seconds... (Press Ctrl+C to cancel)\"\n" +
               "    sleep 5\n" +
               "done\n";
    }

    private String getWindowsTemplate() {
        String absoluteMarker = Paths.get(RESTART_MARKER).toAbsolutePath().toString();
        return "@echo off\n" +
               "title RedstoneReboot Restart Wrapper\n" +
               ":start\n" +
               "    " + detectStartupCommand() + "\n" +
               "if not exist \"" + absoluteMarker + "\" goto end\n" +
               "del /f /q \"" + absoluteMarker + "\" >nul 2>&1\n" +
               "echo Server stopped. Restarting in 5 seconds... (Press Ctrl+C to cancel)\n" +
               "timeout /t 5\n" +
               "goto start\n" +
               ":end\n" +
               "exit /b 0\n";
    }

    private String detectStartupCommand() {
        String override = System.getProperty("redstonereboot.localscript-command");
        if (override == null || override.isBlank()) {
            override = System.getenv("REDSTONEREBOOT_LOCALSCRIPT_COMMAND");
        }
        if (override != null && !override.isBlank()) {
            return isWindows ? windowsEscape(override) : linuxEscape(override);
        }

        String cmd = System.getProperty("sun.java.command");

        // Extract and preserve JVM arguments, filtering sensitive ones
        List<String> safeArgs = new ArrayList<>();
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> inputArgs = runtimeMxBean.getInputArguments();
            for (String arg : inputArgs) {
                if (arg.contains("redstonereboot.active")) {
                    continue;
                }
                if (isSensitiveArg(arg)) {
                    logger.warning("Filtered sensitive JVM argument from restart script: " + arg.split("=")[0] + "=***");
                    continue;
                }
                safeArgs.add(arg);
            }
        } catch (Exception ignored) {}

        StringBuilder command = new StringBuilder("java");
        for (String arg : safeArgs) {
            command.append(' ').append(isWindows ? windowsEscape(arg) : linuxEscape(arg));
        }

        if (cmd == null || cmd.isBlank()) {
            command.append(isWindows ? " -Dredstonereboot.active=true -jar server.jar nogui" : " -Dredstonereboot.active=true -jar server.jar nogui");
            return command.toString();
        }

        // Parse sun.java.command respecting embedded quotes
        List<String> parts = splitCommand(cmd);
        boolean hasJar = false;
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            String lower = part.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jar")) {
                command.append(" -jar");
                hasJar = true;
            }
            command.append(' ').append(isWindows ? windowsEscape(part) : linuxEscape(part));
        }

        if (!hasJar) {
            // Main-class run — no -jar flag needed
        }

        return command.toString();
    }

    private static boolean isSensitiveArg(String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        for (String prefix : SENSITIVE_ARG_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitCommand(String cmd) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ' ' && !inQuote) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    /**
     * Shell-escape a string for bash: single-quote wrapping with embedded single-quote escaping.
     */
    private static String linuxEscape(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    /**
     * Shell-escape a string for Windows cmd.exe: wrap in double quotes, escape inner quotes and special chars.
     */
    private static String windowsEscape(String arg) {
        if (arg.isEmpty()) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (c == '"') {
                sb.append("\"\"");
            } else if (c == '^') {
                sb.append("^^");
            } else if (c == '&' || c == '|' || c == '<' || c == '>' || c == '%') {
                sb.append('^').append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
