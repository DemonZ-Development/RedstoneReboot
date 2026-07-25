# Backend Guide

<div align="center">

![RedstoneReboot Logo](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/logo.png)

</div>

RedstoneReboot can perform a server shutdown or hand restart ownership to an external host backend.

## Backend Overview

| Backend | Ownership Model | Typical Use |
|---------|------------------|-------------|
| `DEPEND_ON_HOST` | Local stop only | Default mode — relies on host environment (Pterodactyl, systemd, Docker, panel script) to handle post-shutdown restart |
| `SYSTEMD` | Supervisor-managed | Linux services managed by `systemd` |
| `DOCKER` | Container-managed | Docker-based deployments |
| `LOCALSCRIPT` | Custom script | Custom restart scripts or wrappers |
| `PTERODACTYL` | Controller-owned | Pterodactyl panel-managed servers |

## Config File

Backends are configured through `restart-backends.properties`.

```properties
backends-enabled=false
active-backend=DEPEND_ON_HOST
lockout-duration-seconds=300
ptero-url=
ptero-token=
ptero-id=
systemd-service=minecraft
localscript-file=start.sh
```

### Key Properties

- **`backends-enabled`**: Set to `true` to enable external backend handling; otherwise defaults to `DEPEND_ON_HOST`.
- **Environment Variables**: Sensitive properties support dynamic resolution using `${env.VAR_NAME:-fallback}` (e.g., `ptero-token=${env.PTERO_API_KEY}`). Allowed prefixes include `REBOOT_`, `PTERO_`, `MINECRAFT_`, and `JAVA_`.
- **Supervisor Wiring Requirements**: Supervisor backends (`LOCALSCRIPT`, `SYSTEMD`, and `DOCKER`) check for wiring proof. Pass the system property `-Dredstonereboot.active=true` or set environment variable `REDSTONEREBOOT_ACTIVE=1` in your container or script to satisfy this check. (Auto-generated LocalScript wrapper templates export this environment variable automatically).

## Choosing a Backend

### DEPEND_ON_HOST

Use this when RedstoneReboot should issue a clean server stop and rely on your panel, Docker policy, or systemd service to restart the server process.

### SYSTEMD

Use this when the server process is managed as a Linux service unit. RedstoneReboot triggers stop or restart calls via systemd.

### DOCKER

Use this when the server runs in a Docker container and container restart policy handles container lifecycle.

### LOCALSCRIPT

Use this when using a wrapper script on standalone servers.

### PTERODACTYL

Use this when the Pterodactyl panel API manages the restart lifecycle.

## Doctor Output

`/reboot doctor` reports active backend state and detects potential environment mismatches.

Backend states:

- `FULL`: Configured and verified
- `ASSISTED`: Configured with partial verification
- `GENERATED`: Script or configuration exists, wiring incomplete
- `DEPEND_ON_HOST`: Relies on host environment post-shutdown
- `MISCONFIGURED`: Missing or invalid settings

## Lockout Behavior

If a backend execution returns an uncertain state, RedstoneReboot enforces a temporary lockout based on `lockout-duration-seconds` to block redundant restart requests.
