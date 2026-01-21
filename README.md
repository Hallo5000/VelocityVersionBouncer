# VelocityVersionBouncer
**A simple and fully automated way to connect players to the correct server based on their game version.**

**VelocityVersionBouncer** is a plugin for the [Velocity proxy](https://papermc.io/software/velocity) (tested on version 3.4) that automatically selects the most compatible backend server for a connecting player, based on their protocol version (Minecraft game version).

---
### 🔧 How It Works
- Everytime a client connects to your proxy the plugin will check all the registered servers and compare their protocol versions (game versions).
- By default, the first server that matches will be selected (you can change that in the config).
- There is a config.toml file located in the plugins folder (`plugins/VelocityVersionBouncer/config.toml`).
- In the config itself each option is pretty well explained (nevertheless I'll include an example config.toml at the end of this README).
- As an optional feature this plugin also provides an extensive fallback functionality.
### ❓ Questions you may have:
- **Is this also triggered when changing servers via `/server`?** No, the version checking is only triggered when connecting initially (from the multiplayer server list) or when using the fallback functionality.
- **What happens if no compatible server is found?** The client will simply be disconnected with the according note/reason.
- **Does this work with modded minecraft servers?** If you're using setups like Ambassador+ProxyCompatibleForge [(more information)](https://docs.papermc.io/velocity/server-compatibility) this plugin will route the client based purely on their protocol version (game version), not their installed mods. _Note: This setup has only been tested with PaperMC and (Neo)Forge servers._
### 📦 Installation & 🛠️ Requirements
1. Download the `.jar` file of the last stable release ([here](https://github.com/Hallo5000/VelocityVersionBouncer/blob/master/build/libs/VelocityVersionBouncer-1.2.0-release.jar)) or build it yourself (the gradle files are included).
2. Put the file in your servers `plugins/` folder (only the proxy!) and restart the server once to generate the config file.
3. When you're finished editing the config restart the proxy once more and everything should be working.
_Note: this plugin may not work properly if you are not running on `Java 21` (or higher) and `Velocity 3.4.0` or above_

### Known Bugs:
- Some mods modify the servers ping request resulting in an error in velocity and a failed routing. This is probably the case if you see `(ping response is corrupted)` as a reason for a failed ping. (I'm currently working on a solution but you can also just use the explicit-routing feature available since `1.2.0-release`)

## Example Config:
```toml
# This config is used to determine how and if servers are checked for their protocol versions whenever a client tries to connect to a backend server (and optionally if they lost connection)

# 'whitelist' is a string array of server names to check for their protocol version when a client tries to connect.
# leaving this empty is equal to putting all registered servers in
# Example: ["server1", "server2"]
# Note: spaces outside the strings will be ignored.
whitelist = ["lobby1", "lobby2", "fun-minigame", "devServer"]

# 'blacklist' is an array of strings to exclude from the temporary list of servers during version comparison.
# first the whitelist is added and then all servers contained in the blacklist are removed from that temporary list.
blacklist = ["devServer"]

# If enabled this option ensures that joining players will be distributed evenly over all servers
# Options: (case-insensitive)
#   "FIRST-MATCH" - players will be sent to the first matching server
#   "BALANCED" - players will be evenly distributed over all matching servers
distribution = "FIRST-MATCH"

# If enabled this option ensures that whenever a client is kicked from a server (whether during login, via /kick, or for another reason),
# they will be automatically redirected (bounced) to another server instead of being disconnected. (obviously if a client disconnects explicitly via leaving the server they will not be redirected)
enable-fallback-bouncing = true
# If set to true, the server the client was kicked from will be temporarily excluded from fallback options,
# preventing the client from being sent back there during fallback-bouncing.
exclude-previous-server = false
# When there is a valid server name in 'explicit-fallback-server' the plugin won't search for a new server and instead only tries the specified server.
# Leave this empty, if you'd like to disable this option (also this is disabled when enable-fallback-bouncing is set to false).
explicit-fallback-server = "lobby"

# ------ for now only the 'p' prefix (protocol) is supported ------
# Syntax
#   vX.X.X - game version
#   p000 - protocol version (Protocol Versions for every release/snapshot can be found here: https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol_version_numbers)
#   cFORGE - Client brand (for example: vanilla, forge, fabric, geyser (for player joining through GeyserMC), badlion, lunar, etc.
# either a game version (v) or a protocol version (p) is necessary but client brands (c) are completely optional
# when no client brand is defined the routing just takes every client joining with the defined game/protocol version
[explicit-routing]
# Example: v1-21-8cFORGE = "lobby-1"
# Example: p123 = "lobby-2"
```