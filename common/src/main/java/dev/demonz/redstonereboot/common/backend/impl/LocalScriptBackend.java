package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Restart backend that relies on a local wrapper script.
 */
public class LocalScriptBackend extends SupervisorBackend {

    private static final String RESTART_MARKER = ".redstonereboot_restart";
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
            return override;
        }

        String cmd = System.getProperty("sun.java.command");

        // Extract and preserve custom JVM arguments (like memory allocations)
        List<String> jvmArgsList = new ArrayList<>();
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> inputArgs = runtimeMxBean.getInputArguments();
            for (String arg : inputArgs) {
                if (!arg.contains("redstonereboot.active")) {
                    jvmArgsList.add(arg);
                }
            }
        } catch (Exception ignored) {}

        String jvmFlags = jvmArgsList.isEmpty() ? "" : String.join(" ", jvmArgsList) + " ";

        if (cmd == null || cmd.isBlank()) {
            return "java " + jvmFlags + "-Dredstonereboot.active=true -jar server.jar nogui";
        }

        // Support spaces in paths by wrapping the JAR path in quotes
        int jarIndex = cmd.toLowerCase().lastIndexOf(".jar");
        if (jarIndex >= 0) {
            String jarPath = cmd.substring(0, jarIndex + 4).trim();
            String rawArgs = cmd.substring(jarIndex + 4).trim();

            String jarQuoted = jarPath.startsWith("\"") && jarPath.endsWith("\"") ? jarPath : "\"" + jarPath + "\"";

            StringBuilder command = new StringBuilder("java ");
            command.append(jvmFlags);
            command.append("-Dredstonereboot.active=true -jar ");
            command.append(jarQuoted);
            if (!rawArgs.isEmpty()) {
                command.append(' ').append(rawArgs);
            }
            return command.toString();
        }

        // Handle main class runs safely
        int firstSpace = cmd.indexOf(' ');
        if (firstSpace >= 0) {
            String mainClass = cmd.substring(0, firstSpace).trim();
            String rawArgs = cmd.substring(firstSpace + 1).trim();

            StringBuilder command = new StringBuilder("java ");
            command.append(jvmFlags);
            command.append("-Dredstonereboot.active=true ");
            command.append(mainClass);
            if (!rawArgs.isEmpty()) {
                command.append(' ').append(rawArgs);
            }
            return command.toString();
        }

        return "java " + jvmFlags + "-Dredstonereboot.active=true -jar \"" + cmd.trim() + "\" nogui";
    }
}
