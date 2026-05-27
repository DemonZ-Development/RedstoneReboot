# Backend Guide

RedstoneReboot can either stop the server itself or hand restart ownership to an environment-specific backend.

## Backend Overview

| Backend | Ownership Model | Typical Use |
|---------|------------------|-------------|
| `SHUTDOWN_ONLY` | Local stop only | Basic hosts where another process already handles startup or where manual restart is acceptable |
| `SYSTEMD` | Supervisor-managed | Linux services managed by `systemd` |
| `DOCKER` | Container-managed | Docker-based deployments |
| `LOCALSCRIPT` | Custom script | Bespoke restart scripts or wrappers |
| `PTERODACTYL` | Controller-owned | Pterodactyl panel-managed servers |

## Config File

Backends are configured through `restart-backends.properties`.

```properties
backends-enabled=false
active-backend=SHUTDOWN_ONLY
lockout-duration-seconds=300
ptero-url=
ptero-token=
ptero-id=
systemd-service=minecraft
localscript-file=start.sh
```

### Key Properties

- **`backends-enabled`**: Must be `true` for backend processing to occur; otherwise defaults to `SHUTDOWN_ONLY`.
- **Environment Variables**: Sensitive properties support dynamic resolution using `${env.VAR_NAME:-fallback}` (e.g., `ptero-token=${env.PTERO_API_KEY}`). This works for variables prefixed with `REBOOT_`, `PTERO_`, `MINECRAFT_`, or `JAVA_`.
- **Supervisor Wiring Requirements**: Supervisor backends (`LOCALSCRIPT`, `SYSTEMD`, and `DOCKER`) require wiring proof so they know they are running under their respective wrappers or supervisors. You **must** pass the system property `-Dredstonereboot.active=true` or set the environment variable `REDSTONEREBOOT_ACTIVE=1` in your container, systemd service unit, or startup script to satisfy this check, otherwise restarts will fail and postpone. (The auto-generated LocalScript wrapper templates export this environment variable automatically).

## Choosing a Backend

### SHUTDOWN_ONLY

Use this when RedstoneReboot should only issue a clean server stop. This is the safest default and the fallback used when no explicit backend is configured.

If you are running under systemd, Docker, or Pterodactyl, SHUTDOWN_ONLY still works — the external supervisor detects the exit and restarts the process. See [FAQ: SHUTDOWN_ONLY but restarts work](FAQ.md#reboot-doctor-shows-shutdown_only-but-restarts-work-fine).

### SYSTEMD

Use this when the server process is already managed as a Linux service. RedstoneReboot requests the stop or restart flow through the named service.

### DOCKER

Use this when the server runs in a Docker container and container restart policy or external orchestration is responsible for bringing it back.

### LOCALSCRIPT

Use this when you need a custom wrapper script. This is useful for panel-less VPS setups or hosts with unusual startup flows.

### PTERODACTYL

Use this when the Pterodactyl panel owns the restart lifecycle. In this mode RedstoneReboot requests the restart and then relinquishes local process ownership.

## Doctor Output

`/reboot doctor` reports the active backend, backend state, and detected environment.

Backend states:

- `FULL`: configured and verified
- `ASSISTED`: configured, but verification is incomplete
- `GENERATED`: artifacts exist, but wiring is incomplete
- `SHUTDOWN_ONLY`: graceful stop only
- `MISCONFIGURED`: required values are missing or invalid

The doctor command also warns about environment mismatches such as selecting `SYSTEMD` while the host looks like a Docker or Pterodactyl deployment.

## Lockout Behavior

When a backend returns an uncertain result, RedstoneReboot enters a temporary lockout using `lockout-duration-seconds`. During that window, new restart requests are blocked to avoid stacking conflicting restart attempts.
