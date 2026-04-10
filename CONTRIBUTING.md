# 🤝 Contributing to RedstoneReboot

Thank you for your interest in contributing to RedstoneReboot! This document provides guidelines and information for contributors.

## 📋 Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. Be kind, constructive, and professional in all interactions.

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (Eclipse Temurin recommended)
- **Gradle 8.4+** (included via wrapper)
- **Git**

### Setup

```bash
# Clone the repository
git clone https://github.com/sdemonzdevelopment-spec/RedstoneReboot.git
cd RedstoneReboot

# Build the project
./gradlew build

# Output JARs are in:
# bukkit/build/libs/
# folia/build/libs/
# common/build/libs/
```

## 📐 Project Structure

```
RedstoneReboot/
├── common/        # Platform-agnostic core logic
│   └── src/main/java/dev/demonz/redstonereboot/common/
│       ├── RedstoneRebootCore.java
│       └── platform/
│           └── ServerPlatform.java
├── bukkit/        # Bukkit/Spigot/Paper implementation
│   └── src/main/java/dev/demonz/redstonereboot/bukkit/
├── folia/         # Folia implementation
├── fabric/        # Fabric mod (future)
├── forge/         # Forge mod (future)
├── neoforge/      # NeoForge mod (future)
├── docs/          # Documentation
│   ├── api/       # Developer API docs
│   ├── wiki/      # User-facing wiki
│   └── marketplace/  # Store listings
└── assets/        # Images, banners, logos
```

## 🔧 Development Workflow

### Branching Strategy
- `main` — Stable, release-ready code
- `dev` — Active development
- `feature/*` — New features
- `fix/*` — Bug fixes

### Making Changes

1. **Fork** the repository
2. **Create** a branch: `git checkout -b feature/my-feature`
3. **Write** your code following our style guidelines
4. **Test** on a local server
5. **Commit** with conventional commits: `git commit -m 'feat: add cool feature'`
6. **Push** and create a Pull Request

### Commit Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | Purpose |
|--------|---------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation only |
| `refactor:` | Code restructuring |
| `test:` | Adding tests |
| `chore:` | Build/tooling changes |

### Code Style

- **Java 17** features are welcome (records, sealed classes, pattern matching)
- Use **SLF4J** for logging (`LoggerFactory.getLogger(...)`)
- Keep platform-specific code in platform modules — `common` must have zero platform imports
- Follow existing naming conventions and package structure

## 🧪 Testing

Before submitting a PR, test your changes:

1. Build: `./gradlew build`
2. Copy the output JAR to a test server
3. Verify the feature or fix works correctly
4. Ensure no regressions in existing functionality

## 📝 Documentation

If your change affects user-facing behavior:
- Update the relevant wiki page in `docs/wiki/`
- Update the API docs in `docs/api/` if applicable
- Add inline Javadoc for any public API methods

## 💡 Need Help?

- Open a [Discussion](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/discussions) for questions
- Join our [Discord](https://discord.gg/demonz) for real-time help
- Check existing [Issues](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues) for known problems

---

*Thank you for making RedstoneReboot better! ❤️*
