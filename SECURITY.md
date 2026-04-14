# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 1.3.x   | ✅ Active support   |
| < 1.3   | ❌ No longer supported |

## Reporting a Vulnerability

If you discover a security vulnerability in RedstoneReboot, **please do not open a public issue.**

Instead, report it privately through one of the following channels:

- **Email**: Contact the maintainer directly via the GitHub profile linked to [DemonZ Development](https://github.com/sdemonzdevelopment-spec).
- **Discord**: Send a direct message to a team member in the [DemonZ Development Discord](https://discord.gg/GYsTt96ypf).

We will acknowledge your report within 72 hours and aim to release a fix within 7 days for confirmed vulnerabilities.

## Scope

Security reports are accepted for:

- Authentication bypass in the Pterodactyl backend (API key exposure, token leaks)
- Remote code execution through command injection
- Unintended file system access through `LocalScript` backend or config parsing
- Environment variable leakage through `BackendConfig` property resolution

Reports about denial-of-service through intentional misconfiguration or features working as designed are out of scope.
