[CENTER]
[IMG]https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png[/IMG]

[SIZE=6][B][COLOR=#DC2626]RedstoneReboot[/COLOR][/B][/SIZE]
[SIZE=4][I]Planned and health-based restarts for Bukkit-family and Folia servers.[/I][/SIZE]

[SIZE=5][B]Platform Compatibility[/B][/SIZE]

[LIST]
[*] [B]Bukkit / Spigot / Paper / Purpur[/B]: API 1.13+; maintainer-tested on 26.1 and 26.2. Java 17 minimum, or the version required by your server.
[*] [B]Folia[/B]: use the dedicated region-threaded artifact and verify it on your Folia build.
[/LIST]
[/CENTER]

[HR][/HR]

[SIZE=5][B]What it does[/B][/SIZE]

RedstoneReboot schedules routine restarts, warns players before shutdown, saves worlds, and hands the final restart to your hosting environment. It can also watch TPS and memory, then schedule an emergency restart only after the server stays below your configured limits for several checks.

This page provides builds for [B]Bukkit-family servers and Folia[/B]. [I](Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub)[/I]

[HR][/HR]

[SIZE=5][B]Features[/B][/SIZE]

[LIST]
[*] [B]Scheduling[/B] — Multiple daily restart times with timezone support and day-of-week filtering.
[*] [B]Health Monitoring[/B] — TPS and memory tracking with consecutive checks to prevent false triggers.
[*] [B]Emergency Restarts[/B] — Restarts server if TPS drops or memory usage exceeds safety limits.
[*] [B]Notifications[/B] — Chat, titles, action bar, and sounds for warning countdowns.
[*] [B]Backend Integration[/B] — Hand off restarts to Pterodactyl, systemd, Docker, or local scripts.
[*] [B]Hot Reload[/B] — Update settings using [CODE]/reboot reload[/CODE] without restarting the server.
[*] [B]PlaceholderAPI[/B] — 8 placeholders for scoreboards, tab lists, and MOTD plugins.
[*] [B]bStats Metrics[/B] — Anonymous server statistics at [URL='https://bstats.org/plugin/bukkit/RedstoneReboot/30751']bstats.org[/URL].
[*] [B]Folia Support[/B] — Dedicated build designed for region-threaded servers.
[*] [B]LuckPerms Support[/B] — Full permission checks with group and context resolution.
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
[*] [B]DEPEND_ON_HOST[/B] — Stops Minecraft cleanly and relies on your panel, service, container policy, or loop script to start it again.
[*] [B]LOCALSCRIPT[/B] — Auto-generated wrapper script handles the restart loop.
[*] [B]SYSTEMD[/B] — Linux servers managed by systemd services.
[*] [B]DOCKER[/B] — Docker containers with restart policies.
[*] [B]PTERODACTYL[/B] — Pterodactyl panel API integration.
[/LIST]

[B]DEPEND_ON_HOST[/B] is the default and does not need panel credentials. Use it when your host already restarts the server after a clean shutdown. Choose another backend only when you want RedstoneReboot to make that handoff itself, then run [CODE]/reboot doctor[/CODE] to check the setup.

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
[SIZE=5][B]Sponsored by Nexeu Hosting[/B][/SIZE]

[URL='https://nexeu.zip/'][IMG]https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png[/IMG][/URL]

Server hosting for this project is sponsored by Nexeu Hosting.

[I]Made by [URL='https://demonzdevelopment.online']DemonZ Development[/URL][/I]
[/CENTER]
