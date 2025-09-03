# VelocityVersionBouncer
### A simple and fully automated way to connect players to the correct server based on their game version.
**VelocityVersionBouncer** is a plugin for the [Velocity proxy](https://papermc.io/software/velocity) (tested on version 3.4) that automatically selects the most compatible backend server for a connecting player, based on their protocol version (Minecraft game version).

---
### 🔧 How It Works
- Everytime a client connects to your proxy the plugin will check all the registered servers and compare their protocol versions (game versions).
- By default, the first server that matches will be selected (you can change that in the config).
- There is a config.toml file located in the plugins folder (`plugins/VelocityVersionBouncer/config.toml`).
- In the config you can do things like changing the order in which the servers are checked or excluding servers from getting checked.
- In the config itself each option is pretty well explained (nevertheless I'll include an example config.toml at the end of this README).
### ❓ Questions you may have:
- **Is this also triggered when changing servers via `/server`?** No, the version checking is only triggered when connecting initially (from the multiplayer server list).
- **What happens if no compatible server is found?** The client will simply be disconnected with the according note/reason.
- **Does this work with modded minecraft servers?** If you're using setups like Ambassador+ProxyCompatibleForge [(more information)](https://docs.papermc.io/velocity/server-compatibility) this plugin will route the client based purely on their protocol version (game version), not their installed mods. _Note: This setup has only been tested with PaperMC and Forge servers._
- **Does this plugin use a lot of CPU resources?** Not at all. The plugin is very lightweight and simply pings each server once on connection. Even in large networks, the performance impact is negligible.
- **Can I setup fallback servers to catch kicked players? Yes this plugin includes a fallback functionality, which can be configured/toggled via the config.**
### 📦 Installation & 🛠️ Requirements
1. Download the `.jar` file ([here](https://github.com/Hallo5000/VelocityVersionBouncer/blob/master/build/libs/VelocityVersionBouncer-1.1.0-release.jar)) or build it yourself (the gradle files are included).
2. Put the file in your servers `plugins/` folder (only the proxy!) and restart the server once to generate the config file.
3. When you're finished editing the config restart the proxy once more and everything should be working.
_Note: this plugin may not work properly if you are not running on `Java 21` (or higher) and `Velocity 3.4.0` or above_

## Example Config:
```toml
# This config is used to determine the order in which servers are tested, and optionally to exclude specific servers.

# 'exclude-servers' is a string array of server names to skip during version comparison.
# Example: ["server1", "server2"]
# Note: Spaces outside the strings will be ignored.
exclude-servers = ["test_server", "privateServer"]

# 'order-mode' defines how the server list should be ordered (case-insensitive).
# Options:
#   "DEFAULT" - servers will be checked in alphabetical order
#   "CUSTOM"  - use the order defined in 'server-list'
order-mode = "CUSTOM"

# 'server-list' is used only if order-mode is set to "CUSTOM".
# It's recommended to list all your servers here and manage exclusions separately using 'exclude-servers',
# but including only specific servers is also possible.
server-list = ["lobby", "minigames", "test_server", "privateServer"]

# 'first-match' determines whether the first or last matching server should be used.
# Set to true to use the first match found, false to use the last.
first-match = true

# If enabled this option ensures that whenever a client is kicked from a server (whether during login, via /kick, or for another reason),
# they will be automatically redirected (bounced) to another server instead of being disconnected.
enable-fallback-bouncing = true
# If set to true, the server the client was kicked from will be temporarily excluded from fallback options,
# preventing the client from being sent back there during fallback-bouncing.
exclude-previous-server = false
# When there is a valid server name in 'explicit-fallback-server' the plugin won't search for a new server and instead only tries the specified server.
# Leave this empty, if you'd like to disable this option (also this is disabled when enable-fallback-bouncing is set to false).
explicit-fallback-server = ""

# an [explicit-routing] option will be added in the future, with which the plugin will no longer automatically look for a matching server but takes a static routing map provided through the config
```