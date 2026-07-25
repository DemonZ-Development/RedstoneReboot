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


package dev.demonz.redstonereboot.common.command;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.backend.EnvironmentDetector;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.manager.RestartHistory;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Shared command processing logic for all platforms.
 * <p>
 * Implements the handler logic for each subcommand of {@code /reboot}.
 * Platform-specific command adapters (Bukkit {@code CommandExecutor}, Brigadier)
 * delegate to this processor after wrapping their command source into a {@link CommandSender}.
 * </p>
 *
 * @since 1.0.0
 */
public class CommandProcessor {

    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT);
    private final RedstoneRebootCore core;

    public CommandProcessor(RedstoneRebootCore core) {
        this.core = core;
    }

    /**
     * Checks whether the given permission string is one of the public-facing
     * permissions that should always be granted regardless of op level.
     *
     * @param permission the permission string to check
     * @return {@code true} if the permission is a public permission
     */
    public static boolean isPublicPermission(String permission) {
        return "redstonereboot.status".equals(permission)
            || "redstonereboot.use".equals(permission)
            || "redstonereboot.notify".equals(permission);
    }

    public void processStatus(CommandSender sender) {
        RestartManager rm = core.getRestartManager();
        sender.sendMessage("\u00A76=== RedstoneReboot Status ===");
        sender.sendMessage("\u00A77Version: \u00A7f" + RedstoneRebootCore.VERSION);
        sender.sendMessage("\u00A77Platform: \u00A7f" + core.getPlatform().getPlatformName());

        if (rm.isRestartInProgress()) {
            long seconds = (long) rm.getSecondsUntilRestart();
            if (seconds < 0) {
                sender.sendMessage("\u00A7cStatus: \u00A7lRestart executing...");
            } else {
                sender.sendMessage("\u00A7cStatus: \u00A7lRestart in progress \u00A7r\u00A77(\u00A7e"
                    + seconds + "s remaining\u00A77)");
            }
            sender.sendMessage("\u00A77Reason: \u00A7f" + rm.getCurrentRestartReason().getDisplayName());
        } else {
            sender.sendMessage("\u00A7aStatus: \u00A7fNormal operation");
        }

        if (rm.getNextScheduledRestart() != null) {
            sender.sendMessage("\u00A7bNext: \u00A7f"
                + rm.getNextScheduledRestart().format(STATUS_TIME_FORMAT)
                + " "
                + core.getConfig().getTimezone());
        }
    }

    public void processReload(CommandSender sender) {
        core.reloadRuntimeState();
        sender.sendMessage("\u00A7aCore engine re-initialized with refreshed backend settings.");
    }

    public void processNow(CommandSender sender, int delay) {
        RestartManager rm = core.getRestartManager();
        String failureReason = getRestartFailureReason(rm);
        if (failureReason != null) {
            sender.sendMessage("\u00A7e" + failureReason);
            return;
        }
        boolean scheduled = rm.scheduleRestart(delay, RestartReason.MANUAL, sender.getName());
        if (scheduled) {
            sender.sendMessage("\u00A7aRestart triggered by " + sender.getName() + " in " + delay + "s.");
        } else {
            sender.sendMessage("\u00A7eA sooner restart is already in progress.");
        }
    }

    public void processSchedule(CommandSender sender, int delay) {
        RestartManager rm = core.getRestartManager();
        String failureReason = getRestartFailureReason(rm);
        if (failureReason != null) {
            sender.sendMessage("\u00A7e" + failureReason);
            return;
        }
        boolean scheduled = rm.scheduleRestart(delay, RestartReason.SCHEDULED_API, sender.getName());
        if (scheduled) {
            sender.sendMessage("\u00A7aManual restart scheduled in " + delay + "s.");
        } else {
            sender.sendMessage("\u00A7eA sooner restart is already in progress.");
        }
    }

    private String getRestartFailureReason(RestartManager rm) {
        if (rm.isLockoutActive()) {
            return "Cannot restart: Lockout is active. Wait for the lockout to expire.";
        }
        if (rm.isControllerRestartPending()) {
            return "Cannot restart: A controller-owned restart (e.g. Pterodactyl) is already pending.";
        }
        return null;
    }

    public void processCancel(CommandSender sender) {
        boolean cancelled = core.getRestartManager().cancelRestart();
        if (cancelled) {
            sender.sendMessage("\u00A7aRestart cancelled.");
        } else {
            sender.sendMessage("\u00A7eNo restart pending.");
        }
    }

    public void processInfo(CommandSender sender) {
        sender.sendMessage("\u00A76=== Server Performance ===");
        sender.sendMessage("\u00A77Platform: \u00A7f" + core.getPlatform().getPlatformName());
        sender.sendMessage("\u00A77TPS: \u00A7f" + String.format(Locale.ROOT, "%.1f", core.getPlatform().getTPS()));

        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0;
        sender.sendMessage("\u00A77Memory: \u00A7f" + String.format(Locale.ROOT, "%.1f%%", memoryUsage));
        sender.sendMessage("\u00A77Players: \u00A7f" + core.getPlatform().getOnlinePlayerCount());

        RestartManager rm = core.getRestartManager();
        if (rm.isRestartInProgress()) {
            long seconds = (long) rm.getSecondsUntilRestart();
            if (seconds < 0) {
                sender.sendMessage("\u00A7cStatus: \u00A7lRestart executing...");
            } else {
                sender.sendMessage("\u00A7cStatus: \u00A7lRestart in progress \u00A7r\u00A77(\u00A7e"
                    + seconds + "s\u00A77)");
            }
        } else {
            sender.sendMessage("\u00A7aStatus: \u00A7fNormal operation");
        }
    }

    public void processHelp(CommandSender sender) {
        sender.sendMessage("\u00A76=== RedstoneReboot Commands ===");
        sender.sendMessage("\u00A77/reboot status \u00A78- \u00A7fView restart status");
        sender.sendMessage("\u00A77/reboot info \u00A78- \u00A7fServer performance");
        sender.sendMessage("\u00A77/reboot doctor \u00A78- \u00A7fSystem diagnostics");
        sender.sendMessage("\u00A77/reboot history \u00A78- \u00A7fRecent restart events");
        sender.sendMessage("\u00A77/reboot now [delay] \u00A78- \u00A7fRestart now");
        sender.sendMessage("\u00A77/reboot schedule <seconds> \u00A78- \u00A7fSchedule restart");
        sender.sendMessage("\u00A77/reboot cancel \u00A78- \u00A7fCancel restart");
        sender.sendMessage("\u00A77/reboot reload \u00A78- \u00A7fReload config");
        sender.sendMessage("\u00A77/reboot help \u00A78- \u00A7fShow this menu");
    }

    public void processHistory(CommandSender sender) {
        sender.sendMessage("\u00A76=== Recent Restarts ===");
        RestartManager rm = core.getRestartManager();
        List<RestartHistory.Entry> entries = rm.getHistory().getRecent(10);
        if (entries.isEmpty()) {
            sender.sendMessage("\u00A77No restart events recorded this session.");
            return;
        }
        for (RestartHistory.Entry entry : entries) {
            sender.sendMessage("\u00A77" + entry.format());
        }
    }

    public void processDoctor(CommandSender sender) {
        sender.sendMessage("\u00A76=== RedstoneReboot Diagnostics ===");

        RestartManager rm = core.getRestartManager();
        PlatformConfig cfg = core.getConfig();

        double tps = core.getPlatform().getTPS();
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0;
        int players = core.getPlatform().getOnlinePlayerCount();

        sender.sendMessage("\u00A77Live Stats:");
        sender.sendMessage("  \u00A77TPS: \u00A7f" + String.format(Locale.ROOT, "%.1f", tps));
        sender.sendMessage("  \u00A77Memory: \u00A7f" + String.format(Locale.ROOT, "%.1f%%", memoryUsage));
        sender.sendMessage("  \u00A77Players: \u00A7f" + players);

        if (rm.isRestartInProgress()) {
            long seconds = (long) rm.getSecondsUntilRestart();
            if (seconds < 0) {
                sender.sendMessage("\u00A7cRestart Status: \u00A7lExecuting now...");
            } else {
                sender.sendMessage("\u00A7cRestart Status: \u00A7lIn progress \u00A7r\u00A77(\u00A7e" + seconds + "s remaining\u00A77)");
            }
            sender.sendMessage("  \u00A77Reason: \u00A7f" + rm.getCurrentRestartReason().getDisplayName());
            sender.sendMessage("  \u00A77Initiator: \u00A7f" + rm.getRestartInitiator());
        } else {
            sender.sendMessage("\u00A7aRestart Status: \u00A7fNormal operation (no restart pending)");
        }

        if (cfg.isScheduledRestartsEnabled()) {
            if (rm.getNextScheduledRestart() != null) {
                sender.sendMessage("\u00A7bNext Scheduled: \u00A7f"
                    + rm.getNextScheduledRestart().format(STATUS_TIME_FORMAT) + " " + cfg.getTimezone());
            } else {
                sender.sendMessage("\u00A7bScheduled Restarts: \u00A7fenabled, but no upcoming time matched the configured days");
            }
        } else {
            sender.sendMessage("\u00A7bScheduled Restarts: \u00A77disabled");
        }

        if (rm.isLockoutActive()) {
            sender.sendMessage("\u00A7c[!] Lockout Active: New restarts suppressed.");
        }

        core.getScheduler().runLaterAsync(() -> {
            RestartBackend backend = core.getBackendRegistry().getActiveBackend();
            RestartBackend.BackendState state = backend.getState();
            List<String> detected = EnvironmentDetector.detectPotentialBackends();

            core.getScheduler().runLater(() -> {
                sender.sendMessage("\u00A77Active Backend: \u00A7b" + backend.getName());

                String stateColor = "\u00A7a";
                if (state == RestartBackend.BackendState.MISCONFIGURED) {
                    stateColor = "\u00A7c";
                } else if (state == RestartBackend.BackendState.GENERATED || state == RestartBackend.BackendState.ASSISTED) {
                    stateColor = "\u00A7e";
                }

                sender.sendMessage("\u00A77Backend State: " + stateColor + "\u00A7l" + state.name());

                if (state == RestartBackend.BackendState.GENERATED) {
                    sender.sendMessage("\u00A7e[!] Script generated, but no 'Wired' proof found.");
                    sender.sendMessage("\u00A7e    Add \u00A7f-Dredstonereboot.active=true \u00A7eto startup.");
                } else if (state == RestartBackend.BackendState.ASSISTED) {
                    if ("Pterodactyl".equalsIgnoreCase(backend.getName())) {
                        sender.sendMessage("\u00A7e[!] Pterodactyl API verification failed. Please verify your ptero-url, ptero-token, and ptero-id settings.");
                    } else {
                        sender.sendMessage("\u00A7e[!] Backend is not wired! Please add -Dredstonereboot.active=true to your server startup command or set environment variable REDSTONEREBOOT_ACTIVE=1.");
                    }
                } else if (state == RestartBackend.BackendState.DEPEND_ON_HOST || state == RestartBackend.BackendState.SHUTDOWN_ONLY) {
                    sender.sendMessage("\u00A77[i] Mode: \u00A7bDEPEND_ON_HOST \u00A77— Server relies on your hosting environment (Pterodactyl, systemd, Docker, auto-restart script, etc.) to perform the reboot after shutdown.");
                }

                if (!detected.isEmpty()) {
                    sender.sendMessage("\u00A77Detected Env: \u00A7f" + String.join(", ", detected));
                    if (!detected.contains(backend.getName().toUpperCase())
                        && !backend.getName().equals("LocalScript")
                        && !backend.getName().equals("ShutdownOnly")) {
                        sender.sendMessage("\u00A7e[i] Mismatch Advice: Detected " + String.join("/", detected) + " but backend is " + backend.getName() + ". Ensure your external supervisor or active-backend handles reboots.");
                    }
                } else {
                    sender.sendMessage("\u00A77Detected Env: \u00A7fGeneric VPS/Local");
                }
            }, 0);
        }, 0);
    }

    /**
     * Platform-neutral command sender abstraction.
     */
    public interface CommandSender {
        void sendMessage(String message);
        String getName();
        boolean hasPermission(String permission);
    }
}