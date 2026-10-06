[CENTER]
[IMG]https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png[/IMG]

[SIZE=6][B][COLOR=#DC2626]RedstoneReboot[/COLOR][/B][/SIZE]
[SIZE=4][I]Automated scheduled restarts and health monitoring for Paper, Purpur, and Folia servers.[/I][/SIZE]

[SIZE=5][B]Compatibility[/B][/SIZE]
[LIST]
[*] [B]Paper / Purpur / Spigot[/B]: Minecraft 1.9 through 26.3+ (Java 17 minimum; Java 25 on 26.x).
[*] [B]Folia[/B]: Dedicated region-threaded build for Folia 1.20+ through 26.3+.
[/LIST]
[/CENTER]

[HR][/HR]

[SIZE=5][B]Overview[/B][/SIZE]

RedstoneReboot manages routine restarts and server performance health. It handles scheduled daily reboots, monitors TPS and RAM to trigger emergency restarts, warns players through chat, titles, action bar, and note block chimes, delays shutdown for chunk saving, and coordinates process reboots with your hosting panel.

[HR][/HR]

[SIZE=5][B]Features[/B][/SIZE]

[LIST]
[*] [B]Routine Schedules[/B]: Multiple daily restart times with timezone support and day-of-week filters.
[*] [B]Performance Watchdog[/B]: Real-time TPS and memory tracking with consecutive check thresholds to prevent false alarms.
[*] [B]Emergency Restarts[/B]: Triggers an alert countdown if TPS drops critically or RAM remains exhausted.
[*] [B]Player Alerts[/B]: Configurable notices across chat, titles, action bar, and sounds.
[*] [B]World Save Protection[/B]: Configurable delay to ensure chunks save before the process exits.
[*] [B]Hot Reload[/B]: Update configuration files live via [CODE]/reboot reload[/CODE].
[*] [B]Integrations[/B]: Full LuckPerms permission support, PlaceholderAPI expansion, and bStats telemetry.
[/LIST]

[HR][/HR]

[SIZE=5][B]How Restarts Work (Backends)[/B][/SIZE]

A Minecraft plugin cannot boot itself back up from inside a terminated Java process. When the restart countdown finishes, RedstoneReboot cleanly stops the server. Bringing it back online requires an external watcher:

[LIST]
[*] [B]DEPEND_ON_HOST[/B] (Default): RedstoneReboot stops the server cleanly. Your game hosting panel (Pterodactyl, Bloom, PebbleHost, Bisect, Docker, etc.) boots it back up. No extra flags or API keys needed.
[*] [B]LOCALSCRIPT[/B]: Uses an auto-generated script ([CODE]redstonereboot-start.sh[/CODE] / [CODE].bat[/CODE]) with a restart loop for local machines or dedicated servers.
[*] [B]SYSTEMD / DOCKER / PTERODACTYL[/B]: Explicit integrations for Linux systemd services, Docker restart policies, and Pterodactyl Client API calls.
[/LIST]

[B]Seeing ASSISTED in /reboot doctor?[/B]
When you enable an external supervisor backend (LOCALSCRIPT, SYSTEMD, DOCKER), RedstoneReboot checks for proof that an outside restart loop is actually watching the process ([CODE]-Dredstonereboot.active=true[/CODE] in startup arguments or [CODE]REDSTONEREBOOT_ACTIVE=1[/CODE] in the environment).

Without this flag, [CODE]/reboot doctor[/CODE] reports the [B]ASSISTED[/B] state to prevent shutting down on a host without an active restart loop.

If your game host already reboots stopped servers automatically, you do not need supervisor wiring. Keep [CODE]backends-enabled=false[/CODE] or set [CODE]active-backend=DEPEND_ON_HOST[/CODE] in [CODE]restart-backends.properties[/CODE].

[HR][/HR]

[SIZE=5][B]Installation[/B][/SIZE]

[LIST=1]
[*] Download the correct jar for your server (Bukkit artifact for standard Paper/Purpur, Folia artifact for Folia).
[*] Place it into your server's [CODE]plugins/[/CODE] folder.
[*] Start the server to generate configuration files.
[*] Configure [CODE]plugins/RedstoneReboot/config.yml[/CODE] and [CODE]restart-backends.properties[/CODE].
[*] Run [CODE]/reboot reload[/CODE] to apply your changes.
[/LIST]

[HR][/HR]

[SIZE=5][B]Commands & Permissions[/B][/SIZE]

[CODE]/reboot[/CODE] — Overview and status ([CODE]redstonereboot.use[/CODE])
[CODE]/reboot now [delay][/CODE] — Trigger a restart countdown ([CODE]redstonereboot.now[/CODE])
[CODE]/reboot schedule <seconds>[/CODE] — Schedule a future restart ([CODE]redstonereboot.schedule[/CODE])
[CODE]/reboot cancel[/CODE] — Cancel an active countdown ([CODE]redstonereboot.cancel[/CODE])
[CODE]/reboot status[/CODE] — Check next scheduled restart ([CODE]redstonereboot.status[/CODE])
[CODE]/reboot info[/CODE] — Show server health metrics ([CODE]redstonereboot.info[/CODE])
[CODE]/reboot doctor[/CODE] — Run backend and environment diagnostics ([CODE]redstonereboot.doctor[/CODE])
[CODE]/reboot reload[/CODE] — Reload configuration files live ([CODE]redstonereboot.reload[/CODE])

[HR][/HR]

[SIZE=5][B]PlaceholderAPI[/B][/SIZE]

[CODE]%redstonereboot_next_restart%[/CODE] — Next scheduled restart date and time
[CODE]%redstonereboot_time_until%[/CODE] — Time remaining until next reboot
[CODE]%redstonereboot_status%[/CODE] — Current server state or countdown status
[CODE]%redstonereboot_reason%[/CODE] — Reason or initiator of current restart
[CODE]%redstonereboot_tps%[/CODE] — Current server TPS
[CODE]%redstonereboot_memory%[/CODE] — Current RAM consumption percentage
[CODE]%redstonereboot_version%[/CODE] — Installed plugin version
[CODE]%redstonereboot_timezone%[/CODE] — Configured timezone

[HR][/HR]

[SIZE=5][B]Links & Community[/B][/SIZE]

[LIST]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot/wiki'][B]Documentation Wiki[/B][/URL]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot'][B]Source Code[/B][/URL]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot/issues'][B]Issue Tracker[/B][/URL]
[*] [URL='https://discord.gg/GYsTt96ypf'][B]Discord Support[/B][/URL]
[*] [URL='https://bstats.org/plugin/bukkit/RedstoneReboot/30751'][B]bStats Metrics[/B][/URL]
[/LIST]

[CENTER]
[SIZE=5][B]Sponsored by Nexeu Hosting[/B][/SIZE]

[URL='https://nexeu.zip/'][IMG]https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png[/IMG][/URL]

Server hosting for this project is sponsored by Nexeu Hosting.

[I]Maintained by [URL='https://demonz.org']DemonZ Development[/URL][/I]
[/CENTER]
