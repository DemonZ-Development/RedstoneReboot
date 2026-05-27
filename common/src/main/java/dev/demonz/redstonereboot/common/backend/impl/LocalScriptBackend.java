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
    private static final java.util.regex.Pattern SENSITIVE_ARG_PATTERN =
        java.util.regex.Pattern.compile(
            "-D.*?(password|secret|token|apikey|key|credential|db\\.|database\\.|jdbc\\.|spring\\.datasource\\.|javax\\.net\\.ssl\\.key|jdk\\.tls\\.client)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );

    private final String scriptName;
    private final boolean isWindows;
    private final Path dataFolder;
    private final Path executionRoot;

    public LocalScriptBackend(Logger logger, String customScriptName, Path dataFolder) {
        super(logger, "LocalScript");
        this.dataFolder = dataFolder;
        this.executionRoot = Paths.get("").toAbsolutePath();
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (customScriptName != null && !customScriptName.trim().isEmpty()) {
            this.scriptName = customScriptName.trim();
        } else {
            this.scriptName = isWindows ? "redstonereboot-start.bat" : "redstonereboot-start.sh";
        }
    }

    @Override
    public void prepare() {
        if (!generateScript(false)) {
            logger.severe("Failed to generate restart wrapper script! Restart may not auto-reboot.");
        }
    }

    @Override
    public BackendResult execute() {
        if (!isWired()) {
            logger.warning("LocalScript backend executed but no wiring detected! Server might not restart.");
            return BackendResult.FAILED;
        }

        try {
            Path markerPath = dataFolder.resolve(RESTART_MARKER).toAbsolutePath();
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
        if (Files.exists(executionRoot.resolve(scriptName).toAbsolutePath())) {
            return BackendState.GENERATED;
        }
        return BackendState.SHUTDOWN_ONLY;
    }

    @Override
    protected boolean isWired() {
        // Wiring proof: -D property, Env Var, or Marker File
        if (super.isWired()) return true;
        return Files.exists(dataFolder.resolve(".redstonereboot_wired").toAbsolutePath());
    }

    public boolean generateScript(boolean overwrite) {
        Path path = executionRoot.resolve(scriptName).toAbsolutePath();
        if (Files.exists(path) && !overwrite) {
            return true;
        }

        try {
            String content = isWindows ? getWindowsTemplate() : getLinuxTemplate();
            Files.writeString(path, content);
            if (!isWindows) {
                path.toFile().setExecutable(true);
            }
            logger.info("Generated restart wrapper: " + scriptName);
            return true;
        } catch (IOException exception) {
            logger.warning("Failed to generate restart script: " + exception.getMessage());
            return false;
        }
    }

    private String getLinuxTemplate() {
        String absoluteMarker = dataFolder.resolve(RESTART_MARKER).toAbsolutePath().toString().replace("\\", "/");
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
        String absoluteMarker = dataFolder.resolve(RESTART_MARKER).toAbsolutePath().toString();
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
        String override = resolveOverrideCommand();
        if (override != null) {
            return override;
        }

        String cmd;
        try {
            cmd = System.getProperty("sun.java.command");
        } catch (SecurityException exception) {
            logger.warning("SecurityManager blocked reading sun.java.command — using fallback: " + exception.getMessage());
            cmd = null;
        }

        if (cmd == null || cmd.isBlank()) {
            return buildFallbackCommand();
        }

        return buildFromSunJavaCommand(cmd);
    }

    /**
     * Check for an explicit override command from system property or environment variable.
     *
     * @return the escaped override command, or {@code null} if no override is set
     */
    private String resolveOverrideCommand() {
        String override = System.getProperty("redstonereboot.localscript-command");
        if (override == null || override.isBlank()) {
            override = System.getenv("REDSTONEREBOOT_LOCALSCRIPT_COMMAND");
        }
        if (override != null && !override.isBlank()) {
            return isWindows ? windowsEscape(override) : linuxEscape(override);
        }
        return null;
    }

    /**
     * Build the startup command by inspecting {@code sun.java.command} and JVM input arguments.
     *
     * @param cmd the value of {@code sun.java.command}
     * @return the full startup command string
     */
    private String buildFromSunJavaCommand(String cmd) {
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

        // Parse sun.java.command respecting embedded quotes
        List<String> parts = splitCommand(cmd);
        boolean hasJar = parts.stream().map(p -> p.toLowerCase(Locale.ROOT)).anyMatch(p -> p.endsWith(".jar"));
        
        if (!hasJar) {
            // Main-class run — add classpath from java.class.path BEFORE the main class
            String classPath = System.getProperty("java.class.path", "");
            if (!classPath.isEmpty()) {
                command.append(" -cp ");
                if (isWindows) {
                    command.append(windowsEscape(classPath));
                } else {
                    command.append(linuxEscape(classPath));
                }
            }
        }

        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            String lower = part.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jar")) {
                command.append(" -jar");
            }
            command.append(' ').append(isWindows ? windowsEscape(part) : linuxEscape(part));
        }

        return command.toString();
    }

    /**
     * Build a minimal fallback startup command when {@code sun.java.command} is unavailable.
     *
     * @return a fallback command string using {@code -jar server.jar nogui}
     */
    private String buildFallbackCommand() {
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
        command.append(" -Dredstonereboot.active=true -jar server.jar nogui");
        return command.toString();
    }

    private static boolean isSensitiveArg(String arg) {
        return SENSITIVE_ARG_PATTERN.matcher(arg).find();
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
