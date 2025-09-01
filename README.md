# VelocityVersionBouncer
### A simple and fully automated way to connect players to the correct server based on their game version.
**VelocityVersionBouncer** is a plugin for the [Velocity proxy](https://papermc.io/software/velocity) (tested on version 3.4.0) that automatically selects the most compatible backend server for a connecting player, based on their protocol version (Minecraft game version).

---
## 🔧 How It Works
- Everytime a client connects to your proxy the plugin will check all the registered servers and compare their protocol versions (game versions).
- The last server that matches (based on alphabetical order) will be selected.
- There is a config.toml file located in the plugins folder (`plugins/VelocityVersionBouncer/config.toml`).
- In the config you can add servers to exclude when checking the versions like this: `exclude-servers = ["server1", "server2"]`.
- The servers must be named the same as defined in `velocity.toml`.
### ❓ Questions you may have:
- **Is this also triggered when changing servers via `/server`?** No, the version checking is only triggered when connecting initially (from the multiplayer server list).
- **What happens if no compatible server is found?** The client will simply be disconnected with the according note/reason.
- **Does this work with modded minecraft servers?** If you're using setups like Ambassador+ProxyCompatibleForge [(more information)](https://docs.papermc.io/velocity/server-compatibility) this plugin will route the client based purely on their protocol version (game version), not their installed mods. _Note: This setup has only been tested with PaperMC and Forge servers._
- **Does this plugin use a lot of CPU resources?** Not at all. The plugin is very lightweight and simply pings each server once on connection. Even in large networks, the performance impact is negligible.
### 📋 Current Limitations
- Server list is currently processed alphabetically - I'll probably make this customizable in the future.
- The last matching server is used even if for example it's heavily modded and you're joining with the vanilla client.
### 📦 Installation & 🛠️ Requirements
1. Download the `.jar` file or build it yourself (the gradle files are included).
2. Put the file in your servers `plugins/` folder (only the proxy!) and restart the server once to generate the config file.
3. When you're finished editing the config restart the proxy once more and everything should be working.
_Note: this plugin may not work properly if you are not running on `Java 21` (or higher) and `Velocity 3.4.0` and above