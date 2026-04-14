# Permissions Reference

RedstoneReboot uses Bukkit permissions with optional LuckPerms integration. On mod platforms (Fabric, Forge, NeoForge), permissions fall back to operator level checks.

## Permission Nodes

| Permission | Description | Default |
|------------|-------------|---------|
| `redstonereboot.*` | Grants all RedstoneReboot permissions | `op` |
| `redstonereboot.use` | Basic plugin usage and `/reboot help` | `true` |
| `redstonereboot.admin` | Full administrative access | `op` |
| `redstonereboot.status` | View restart status via `/reboot status` | `true` |
| `redstonereboot.doctor` | Run diagnostics via `/reboot doctor` | `op` |
| `redstonereboot.restart.now` | Trigger immediate restart via `/reboot now` | `op` |
| `redstonereboot.restart.schedule` | Schedule a restart via `/reboot schedule` | `op` |
| `redstonereboot.restart.cancel` | Cancel a pending restart via `/reboot cancel` | `op` |
| `redstonereboot.config.reload` | Reload configuration via `/reboot reload` | `op` |
| `redstonereboot.notify` | Receive restart countdown notifications | `true` |

## LuckPerms Integration

When LuckPerms is installed and `permissions.luckperms.integration-enabled` is `true` in `config.yml`, RedstoneReboot queries LuckPerms directly via its API for all permission checks. This provides group-based and context-aware permission resolution.

If LuckPerms is not installed, RedstoneReboot falls back to standard Bukkit permission checks.

## Operator Fallback

When `permissions.fallback.use-op-as-admin` is `true` (the default), server operators automatically receive admin-level permissions regardless of the permission plugin in use.

## Mod Platforms

Fabric, Forge, and NeoForge builds do not have a Bukkit permission system. Instead, they use operator permission levels:

- `use-op-as-admin: true` — OP level 4 grants admin access
- `default-permission-level: 2` — The minimum OP level required for commands

These values are configurable in `config/redstonereboot.properties`.
