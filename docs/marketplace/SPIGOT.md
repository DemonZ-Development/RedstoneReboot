# RedstoneReboot — SpigotMC Resource Copy

<!-- BBCode-oriented resource description for SpigotMC -->

[CENTER]
[IMG]https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png[/IMG]

[SIZE=6][B][COLOR=#DC2626]RedstoneReboot[/COLOR][/B][/SIZE]
[SIZE=4][I]A restart engine for Minecraft servers across multiple platforms.[/I][/SIZE]

[SIZE=5][B]Platform Compatibility[/B][/SIZE]

[LIST]
[*] [B]Bukkit / Spigot / Paper / Purpur[/B]: 1.9.x to 26.x
[*] [B]Folia[/B]: 1.20.1+ to 26.x
[*] [B]Fabric[/B]: 1.20.1+ to 26.x
[*] [B]Forge[/B]: 1.20.4+ to 26.x
[*] [B]NeoForge[/B]: 1.21.1+ to 26.x (Java 21+)
[/LIST]
[/CENTER]

[HR][/HR]

[SIZE=5][B]Why RedstoneReboot?[/B][/SIZE]

RedstoneReboot is a server lifecycle tool — not just a "restart plugin." It gives you control over when, why, and how your server restarts, using live health monitoring and a backend handoff system.

Whether you're running a single survival server or a multi-node network behind Pterodactyl, RedstoneReboot handles the restart plumbing for you.

This page provides builds for [B]Bukkit-family servers and Folia[/B]. [I](Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub)[/I]

[HR][/HR]

[SIZE=5][B]What Sets It Apart[/B][/SIZE]

[LIST]
[*] [B]Intelligent Scheduling[/B] — Multiple daily restart windows with global timezone support and day-of-week filtering.
[*] [B]Real-Time Health Monitoring[/B] — TPS and memory tracking with consecutive-check protection against false positives.
[*] [B]Emergency Fail-safes[/B] — Automatic emergency restarts when critical TPS or memory thresholds are breached.
[*] [B]Rich Multi-Channel Alerts[/B] — Chat, titles, action bar notifications, and configurable sounds.
[*] [B]Backend Handoff System[/B] — Delegate restart execution to Pterodactyl, Systemd, Docker, or local scripts.
[*] [B]Hot-Reload Config[/B] — Change backend settings and run [CODE]/reboot reload[/CODE]. No full server restart.
[*] [B]PlaceholderAPI Integration[/B] — 8 placeholders for scoreboards, tab lists, and MOTD plugins.
[*] [B]bStats Metrics[/B] — Anonymous server telemetry at [URL='https://bstats.org/plugin/bukkit/RedstoneReboot/30751']bstats.org[/URL].
[*] [B]Complete Folia Support[/B] — Dedicated build designed around region-threaded logic.
[*] [B]LuckPerms Integration[/B] — Full permission resolution with group and context support.
[/LIST]

[HR][/HR]

[SIZE=5][B]Installation[/B][/SIZE]

[LIST=1]
[*] Download the correct plugin JAR (Bukkit for standard, Folia for region-threaded).
[*] Place it into your server's [CODE]plugins/[/CODE] folder.
[*] Start the server — configuration files are generated automatically.
[*] Edit [CODE]plugins/RedstoneReboot/config.yml[/CODE] and [CODE]restart-backends.properties[/CODE].
[*] Run [CODE]/reboot reload[/CODE] to apply changes — or restart the server.
[/LIST]

[HR][/HR]

[SIZE=5][B]Commands & Permissions[/B][/SIZE]

[CODE]/reboot[/CODE] — Plugin status & help
[CODE]/reboot now [delay][/CODE] — Restart with optional countdown
[CODE]/reboot schedule <seconds>[/CODE] — Schedule future restart
[CODE]/reboot cancel[/CODE] — Cancel pending countdown
[CODE]/reboot status[/CODE] — Show schedule status
[CODE]/reboot info[/CODE] — Show health diagnostics
[CODE]/reboot doctor[/CODE] — Run backend & environment diagnostics
[CODE]/reboot reload[/CODE] — Hot-reload all configuration files

[B]Permissions:[/B] [CODE]redstonereboot.use[/CODE] (default: true), [CODE]redstonereboot.admin[/CODE] (default: op), [CODE]redstonereboot.doctor[/CODE] (default: op), [CODE]redstonereboot.notify[/CODE] (default: true).

[HR][/HR]

[SIZE=5][B]PlaceholderAPI Placeholders[/B][/SIZE]

[CODE]%redstonereboot_next_restart%[/CODE] — Next scheduled restart date/time
[CODE]%redstonereboot_time_until%[/CODE] — Time remaining until restart
[CODE]%redstonereboot_status%[/CODE] — Current server status
[CODE]%redstonereboot_reason%[/CODE] — Current restart reason
[CODE]%redstonereboot_tps%[/CODE] — Last recorded TPS
[CODE]%redstonereboot_memory%[/CODE] — Current memory usage %
[CODE]%redstonereboot_version%[/CODE] — Plugin version
[CODE]%redstonereboot_timezone%[/CODE] — Configured timezone

[I]Requires PlaceholderAPI. MOTD compatible as of v1.3.3+.[/I]

[HR][/HR]

[SIZE=5][B]Backend System[/B][/SIZE]

RedstoneReboot separates "when to restart" from "how to restart":

[LIST]
[*] [B]SHUTDOWN_ONLY[/B] — Graceful shutdown only (external process manager restarts).
[*] [B]LOCALSCRIPT[/B] — Auto-generated wrapper script handles the restart loop.
[*] [B]SYSTEMD[/B] — Linux servers managed by systemd services.
[*] [B]DOCKER[/B] — Docker containers with restart policies.
[*] [B]PTERODACTYL[/B] — Pterodactyl panel API integration.
[/LIST]

[B]Do I need a custom backend?[/B]
If your server is already wrapped in a startup loop script (a `.sh` or `.bat` file with a `while true` loop, a Docker container set to `restart: always`, or a systemd service), [B]SHUTDOWN_ONLY works out of the box[/B]. When the restart timer runs out, the plugin stops the server cleanly and your script starts it again.

[B]Why use a custom backend then?[/B]
[LIST=1]
[*] [B]Clean handoff (Pterodactyl, Multicraft)[/B] — Avoid panel desyncs or false offline indicators. Instead of just shutting down, the plugin talks to your panel's API to request a clean power cycle.
[*] [B]Self-healing bootups[/B] — If you don't run a startup loop script, the [B]LOCALSCRIPT[/B] backend spawns a new process to bring the server back up.
[*] [B]Crash lockout safety[/B] — Simple loops can get stuck in endless crash loops if a file gets corrupted. Custom backends add safety lockout timers to stop boot hammering.
[/LIST]

Edit [CODE]restart-backends.properties[/CODE] and run [CODE]/reboot reload[/CODE] — changes apply instantly.

[HR][/HR]

[SIZE=5][B]Helpful Links[/B][/SIZE]

[LIST]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot/wiki'][B]Documentation Wiki[/B][/URL]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot'][B]GitHub Repository[/B][/URL]
[*] [URL='https://bstats.org/plugin/bukkit/RedstoneReboot/30751'][B]bStats Metrics[/B][/URL]
[*] [URL='https://discord.gg/GYsTt96ypf'][B]Discord Support[/B][/URL]
[*] [URL='https://github.com/DemonZ-Development/RedstoneReboot/issues'][B]Issue Tracker[/B][/URL]
[/LIST]

[CENTER]
[I]Made by [URL='https://demonzdevelopment.online']DemonZ Development[/URL][/I]
[/CENTER]
