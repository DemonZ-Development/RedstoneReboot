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
active-backend=SHUTDOWN_ONLY
lockout-duration-seconds=300
ptero-url=
ptero-token=
ptero-id=
systemd-service=minecraft
localscript-file=start.sh
```

## Choosing a Backend

### SHUTDOWN_ONLY

Use this when RedstoneReboot should only issue a clean server stop. This is the safest default and the fallback used when no explicit backend is configured.

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
