# Frequently Asked Questions

## General

### What Java version do I need?

- **Bukkit-family servers** (1.9–1.16): Java 8+
- **Modern servers** (1.17+), Folia, Fabric, Forge, NeoForge: Java 17+
- **Building from source**: Java 21+ (required by the NeoForge module toolchain)

### Does RedstoneReboot support Folia?

Yes. The Bukkit build includes an automatic Folia scheduler bridge that detects Folia at runtime and switches to the region-threaded `GlobalRegionScheduler`. There is also a dedicated `RedstoneReboot-Folia` jar for explicit Folia deployments.

---

## Configuration

### My timezone isn't working

Use a valid Java `ZoneId` string such as `Asia/Kolkata`, `America/New_York`, or `UTC`. Short abbreviations like `IST` or `EST` are ambiguous and may not resolve correctly. Run `/reboot status` after startup to confirm the active timezone.

### Can I change the backend without restarting the server?

Yes. Edit `restart-backends.properties` and then run `/reboot reload`. The engine re-reads the backend configuration and re-initializes the active backend live.

### What does `metrics-enabled` do?

When set to `true` in `config.yml`, RedstoneReboot reports anonymous usage statistics to [bStats](https://bstats.org/plugin/bukkit/RedstoneReboot/30751). This helps the development team understand which platforms and features are most used. You can opt out by setting it to `false`.

---

## Commands

### Commands are not appearing

- **Plugin builds**: Check that the plugin loaded by running `/plugins`. Verify `plugin.yml` registered the `reboot` command.
- **Mod builds**: Confirm the dedicated server finished startup and the Brigadier command tree was registered. Check the server log for `RedstoneReboot command registered`.

### What does `/reboot doctor` do?

It runs a diagnostic that shows:
- The active restart backend and its verification state
- The detected server environment (Systemd, Docker, Pterodactyl, or generic)
- Any mismatches between your chosen backend and the detected environment
- Whether a lockout is currently active

---

## PlaceholderAPI

### My placeholders aren't working in MOTD

Make sure you are running RedstoneReboot **v1.3.3+**, which includes null-safety fixes for MOTD compatibility. Earlier versions could fail silently when MOTD plugins requested placeholders during server-list pings before the server fully started.

Also verify that `placeholders.enabled` is set to `true` in `config.yml` and that PlaceholderAPI is installed.

### What placeholders are available?

See the full list on the [Placeholders](Placeholders.md) wiki page.

---

## Backends

### `/reboot doctor` says "Potential Mismatch"

This means the backend you configured in `restart-backends.properties` does not match the environment RedstoneReboot detected. For example, selecting `SYSTEMD` while running inside a Docker container. Review the [Backend Guide](Backends.md) and adjust your `active-backend` value.

### What is the lockout?

When a backend returns an uncertain result (e.g., a Pterodactyl API timeout), RedstoneReboot enters a temporary lockout period (`lockout-duration-seconds` in the backend config). During this window, new restart requests are blocked to avoid stacking conflicting attempts.

---

## bStats

### How do I opt out of bStats?

Set `metrics-enabled: false` under the `advanced:` section in `config.yml`. Alternatively, you can disable bStats globally through the bStats config file at `plugins/bStats/config.yml`.
