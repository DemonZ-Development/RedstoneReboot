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

public class LocalScriptBackend extends SupervisorBackend {

    private static final String RESTART_MARKER = ".redstonereboot_restart";
    private static final java.util.regex.Pattern SENSITIVE_ARG_PATTERN =
        java.util.regex.Pattern.compile(
            "(-D|--?)(.*?)?(password|secret|token|apikey|key|credential|db\\.|database\\.|jdbc\\.|spring\\.datasource\\.|javax\\.net\\.ssl\\.key|jdk\\.tls\\.client)",
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
        return BackendState.DEPEND_ON_HOST;
    }

    @Override
    protected boolean isWired() {
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
               "export REDSTONEREBOOT_ACTIVE=1\n" +
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
               "set REDSTONEREBOOT_ACTIVE=1\n" +
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

    private String buildFromSunJavaCommand(String cmd) {
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

        List<String> parts = splitCommand(cmd);
        boolean hasJar = parts.stream()
            .map(p -> p.replaceAll("^\"|\"$", "").toLowerCase(Locale.ROOT))
            .anyMatch(p -> p.endsWith(".jar"));

        if (!hasJar) {
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
            String cleanPart = part.replaceAll("^\"|\"$", "");
            String cleanLower = cleanPart.toLowerCase(Locale.ROOT);
            if (isSensitiveArg(part)) {
                logger.info("Filtered sensitive program argument from wrapper script.");
                continue;
            }
            if (cleanLower.endsWith(".jar")) {
                command.append(" -jar");
            }
            command.append(' ').append(isWindows ? windowsEscape(cleanPart) : linuxEscape(cleanPart));
        }

        return command.toString();
    }

    private String buildFallbackCommand() {
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

    private List<String> splitCommand(String cmd) {
        List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
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

    private static String linuxEscape(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private static String windowsEscape(String arg) {
        if (arg.isEmpty()) return "\"\"";
        boolean wrapped = arg.length() >= 2 && arg.startsWith("\"") && arg.endsWith("\"");

        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (c == '"') sb.append("\"\"");
            else if (!wrapped && (c == '^')) sb.append("^^");
            else if (!wrapped && (c == '&' || c == '|' || c == '<' || c == '>' || c == '%')) sb.append('^').append(c);
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }
}
