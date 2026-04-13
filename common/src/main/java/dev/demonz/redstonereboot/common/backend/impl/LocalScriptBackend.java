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

    public LocalScriptBackend(Logger logger) {
        super(logger, "LocalScript");
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        this.scriptName = isWindows ? "redstonereboot-start.bat" : "redstonereboot-start.sh";
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
            Files.writeString(Paths.get(RESTART_MARKER), "restart");
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
        if (Files.exists(Paths.get(scriptName))) {
            return BackendState.GENERATED;
        }
        return BackendState.SHUTDOWN_ONLY;
    }

    private boolean isWired() {
        // Wiring proof: -D property or Env Var or Marker File
        if (Boolean.getBoolean("redstonereboot.active")) return true;
        if ("1".equals(System.getenv("REDSTONEREBOOT_ACTIVE"))) return true;
        return Files.exists(Paths.get(".redstonereboot_wired"));
    }

    public void generateScript(boolean overwrite) {
        Path path = Paths.get(scriptName);
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
        return "#!/bin/bash\n" +
               "# RedstoneReboot Auto-Restart Wrapper\n" +
               "while true; do\n" +
               "    " + detectStartupCommand() + "\n" +
               "    if [ ! -f \"" + RESTART_MARKER + "\" ]; then\n" +
               "        exit 0\n" +
               "    fi\n" +
               "    rm -f \"" + RESTART_MARKER + "\"\n" +
               "    echo \"Server stopped. Restarting in 5 seconds... (Press Ctrl+C to cancel)\"\n" +
               "    sleep 5\n" +
               "done\n";
    }

    private String getWindowsTemplate() {
        return "@echo off\n" +
               "title RedstoneReboot Restart Wrapper\n" +
               ":start\n" +
               "    " + detectStartupCommand() + "\n" +
               "if not exist " + RESTART_MARKER + " goto end\n" +
               "del /f /q " + RESTART_MARKER + " >nul 2>&1\n" +
               "echo Server stopped. Restarting in 5 seconds... (Press Ctrl+C to cancel)\n" +
               "timeout /t 5\n" +
               "goto start\n" +
               ":end\n" +
               "exit /b 0\n";
    }

    private String detectStartupCommand() {
        String cmd = System.getProperty("sun.java.command");
        if (cmd == null || cmd.isBlank()) {
            return "java -Dredstonereboot.active=true -jar server.jar nogui";
        }

        String[] parts = cmd.trim().split("\\s+");
        int jarIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].endsWith(".jar")) {
                jarIndex = i;
                break;
            }
        }

        if (jarIndex >= 0) {
            String jar = parts[jarIndex];
            List<String> args = new ArrayList<>();
            for (int i = jarIndex + 1; i < parts.length; i++) {
                args.add(parts[i]);
            }

            StringBuilder command = new StringBuilder("java -Dredstonereboot.active=true -jar ");
            command.append(jar);
            if (!args.isEmpty()) {
                command.append(' ').append(String.join(" ", args));
            }
            return command.toString();
        }

        return "java -Dredstonereboot.active=true -jar server.jar nogui";
    }
}
