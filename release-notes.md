## RedstoneReboot 1.6.6

Feature release introducing native Pumpkin MC support and dedicated restart history management across all platforms.

### Highlights

• **Native Pumpkin MC Support**: Added an official Rust WebAssembly plugin (`wasm32-wasip2`) for Pumpkin 0.2.0+ servers with countdown alerts, sound notifications, and runtime commands.  
• **Intelligent Backend Detection**: Automatically switches between host-managed restarts (Docker, Pterodactyl) and local loop scripts on standalone servers.  
• **Auto Script Generation**: Automatically writes `run.bat` (Windows) and `run.sh` (Linux) with multi-level directory discovery.  
• **Dedicated Restart Logs**: Relocated default history logging to `logs/redstonereboot/restarts.log` so modpack updates wiping `config/` preserve your restart logs without cluttering the root logs folder.  
• **Configurable History Path**: Added a configurable `history-file` setting across Paper, Purpur, Folia, Fabric, Forge, NeoForge, and Pumpkin MC.  
• **Configuration Version Bump**: Upgraded config versions (Bukkit v4, Mod Platforms v3) with automated migration and backup preservation.  
• **Legacy Log Migration**: Automatically relocates existing `restarts.log` files from data folders to the new logs directory on first boot.  

### Requirements

• Bukkit, Paper, Spigot, and Purpur: Java 17+  
• Folia: Java 17+  
• Fabric 1.20.1: Java 17+ (Fabric 26.x: Java 25)  
• Forge 1.20.4: Java 17+ (Forge 26.x: Java 25)  
• NeoForge 1.21.1: Java 21+ (NeoForge 26.x: Java 25)  
• Pumpkin MC: 0.2.0+ (WASI Component Model runtime)  
