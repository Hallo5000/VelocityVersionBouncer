package de.hallo5000.main;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import jakarta.json.Json;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    
    private final VelocityVersionBouncer plugin;
    public Utils(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }
    
    /**
     * Gets the whitelist and blacklist from the plugins config.yml and computes the resulting 'serverlist'
     * @return a list of <code>RegisteredServer</code>s, may be empty but not null
     */
    public @NotNull List<RegisteredServer> getConfigServerList(){
        //take all backend servers or only the ones provided by the whitelist (if one exists) and remove the ones on the blacklist
        List<RegisteredServer> serverList = (new ArrayList<>(plugin.getToml().getList("whitelist")))
                .stream().map(name -> plugin.getServer().getServer((String) name).orElseGet(() -> {
                    plugin.getLogger().info("'" + name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).collect(Collectors.toList());
        if(serverList.isEmpty()) serverList = new ArrayList<>(plugin.getServer().getAllServers());
        List<RegisteredServer> blacklist = Optional.ofNullable(plugin.getToml().getList("blacklist"))
                .orElse(new ArrayList<>(Collections.emptyList()))
                .stream().map(name -> plugin.getServer().getServer((String) name).orElseGet(() -> {
                    plugin.getLogger().info("'"+ name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).toList();
        serverList.removeAll(blacklist);
        return serverList;
    }

    /**
     * Gets all explicit routings from the plugins config.yml and searches for matching routings (may not be in order)
     * @param inboundConnection the client whose protocol to compare the explicit routings to
     * @return the first found server or null if no server is found
     */
    public @Nullable RegisteredServer checkForExplicitRouting(InboundConnection inboundConnection){
        if(inboundConnection == null) return null;
        int protocol = inboundConnection.getProtocolVersion().getProtocol();
        String clientBrand = "";
        if(inboundConnection instanceof Player p) clientBrand = p.getClientBrand();
        
        Map<String, Object> explicitRoutings = new LinkedHashMap<>(plugin.getToml().getTable("explicit-routing").toMap());
        //removes all invalid explicit routings
        for(String s : explicitRoutings.keySet()){
            if(s.isEmpty() || !s.matches("^(?=[pvc])((p\\d{3})|(v1.\\d+.\\d+))?(c\\S+)?$")) explicitRoutings.remove(s);
        }
        //removes all explicit routings with unmatching client brands
        for(String s : new HashSet<>(explicitRoutings.keySet())){
            if(s.contains("c") && !s.endsWith("c"+clientBrand)) explicitRoutings.remove(s);
        }
        //matching protocol version + optional client brand
        if(explicitRoutings.keySet().stream().anyMatch(r -> r.startsWith("p"+protocol))) {
            //at this point there is at least one match for the protocol version number
            for(String s : new HashSet<>(explicitRoutings.keySet())){
                if(s.startsWith("p")){
                    if(!s.startsWith("p"+protocol)) explicitRoutings.remove(s);
                    else if(s.endsWith("c"+clientBrand))
                        return plugin.getServer().getServer((String) explicitRoutings.get(s)).orElse(null);
                }
            }
            String serverName = (String) explicitRoutings.get("p" + protocol);
            return plugin.getServer().getServer(serverName).orElse(null);
        }
        //matching game versions + optional client brand
        for(String v : inboundConnection.getProtocolVersion().getVersionsSupportedBy()){
            if(explicitRoutings.keySet().stream().anyMatch(r -> r.startsWith("v"+v))){
                for(String s : new HashSet<>(explicitRoutings.keySet())){
                    if(s.startsWith("v")){
                        if(!s.startsWith("v"+v)) explicitRoutings.remove(s);
                        else if(s.endsWith("c"+clientBrand))
                            return plugin.getServer().getServer((String) explicitRoutings.get(s)).orElse(null);
                    }
                }
                String serverName = (String) explicitRoutings.get("v"+v);
                return plugin.getServer().getServer(serverName).orElse(null);
            }
        }
        //match with only client brand
        if(explicitRoutings.containsKey("c"+clientBrand))
            return plugin.getServer().getServer((String) explicitRoutings.get("c"+clientBrand)).orElse(null);
        return null;
    }

    /**
     * Takes in a JSON Payload from the Status Response packet in a Server List Ping
     * @param json the JSON response field (can be obtained by <code>PingHandler.ping();</code>
     * @return a <code>ServerPing</code> containing every field obtainable from <code>json</code> (if not obtainable the field is replaced by the given default value)
     */
    public @NotNull ServerPing getPingFromHandshake(@NotNull String json,
                                                    int defaultVersionProtocol,
                                                    @NotNull String defaultVersionName,
                                                    int defaultPlayersOnline,
                                                    int defaultPlayersMax,
                                                    @NotNull List<ServerPing.SamplePlayer> defaultPlayersSample,
                                                    @NotNull Component defaultDescription,
                                                    @Nullable Favicon defaultFavicon,
                                                    @NotNull String defaultModInfoType,
                                                    @NotNull List<ModInfo.Mod> defaultModList){
        //fields for ServerPing.Version
        int protocol = plugin.getJsonReader().getIntFromJson(json, new String[]{"version", "protocol"}).orElse(defaultVersionProtocol);
        String name = plugin.getJsonReader().getStringFromJson(json, new String[]{"version", "name"}).orElse(defaultVersionName);

        //fields for ServerPing.Players
        int online = plugin.getJsonReader().getIntFromJson(json, new String[]{"players", "online"}).orElse(defaultPlayersOnline);
        int max = plugin.getJsonReader().getIntFromJson(json, new String[]{"players", "max"}).orElse(defaultPlayersMax);
        List<ServerPing.SamplePlayer> sample = plugin.getJsonReader().getJsonFromJson(json, new String[]{"players", "sample"})
                        .map(jsonArray -> new Gson().fromJson(jsonArray, ServerPing.SamplePlayer[].class))
                        .map(Arrays::asList)
                        .orElse(defaultPlayersSample);
        //when the players field as a whole does not exist, the default values for ServerPing.Players won't be used and instead, it's set to null
        boolean players = online != defaultPlayersOnline || max != defaultPlayersMax || sample != defaultPlayersSample;

        Component description = plugin.getJsonReader().getJsonFromJson(json, new String[]{"description"}).map(j -> JSONComponentSerializer.json().deserialize(j)).orElse(defaultDescription);
        Favicon favicon = plugin.getJsonReader().getStringFromJson(json, new String[]{"favicon"}).map(f -> new Favicon(f.split(",")[1])).orElse(defaultFavicon);

        //fields for ModInfo
        ModInfo modInfo = null;
        if(plugin.getJsonReader().findKeyInJson(Json.createParser(new StringReader(json)), new String[]{"modinfo"})){
            List<ModInfo.Mod> modList = plugin.getJsonReader().getJsonFromJson(json, new String[]{"modinfo", "modList"})
                    .map(jsonArray -> new Gson().fromJson(jsonArray, ModInfo.Mod.class))
                    .map(Arrays::asList)
                    .orElse(defaultModList);
            modInfo = new ModInfo(plugin.getJsonReader().getStringFromJson(json, new String[]{"modinfo", "type"}).orElse(defaultModInfoType), modList);
        }

        return new ServerPing(new ServerPing.Version(protocol, name), players ? new ServerPing.Players(online, max, sample) : null, description, favicon, modInfo);
    }

    public @NotNull ServerPing getPingFromHandshake(@NotNull String json){
        return getPingFromHandshake(json, -1, "", -1, -1, Collections.emptyList(), Component.empty(), null, "FML", Collections.emptyList());
    }

    public RegisteredServer findMatchingServer(InboundConnection client){
        RegisteredServer match = plugin.getUtils().checkForExplicitRouting(client);
        if (match != null) {
            plugin.getLogger().info("Found explicit routing: " + match.getServerInfo().getName());
            return match;
        }

        //Toml Vars
        String distribution = Optional.ofNullable(plugin.getToml().getString("distribution")).orElse("FIRST-MATCH");

        //start checking
        plugin.getLogger().info("Start checking for compatibilities (Client-Protocol: " + client.getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version
        for(RegisteredServer s : plugin.getUtils().getConfigServerList()){
            plugin.getLogger().info("Check " + s.getServerInfo().getName() + " with protocol " + plugin.getBackendPingService().getProtocol(s));
            if (client.getProtocolVersion().getProtocol() == plugin.getBackendPingService().getProtocol(s).orElse(-1)) {
                matches.add(s);
                plugin.getLogger().info("> " + s.getServerInfo().getName() + " is compatible (Server-Protocol: " + plugin.getBackendPingService().getProtocol(s) + ")");
            } else
                plugin.getLogger().info("> " + s.getServerInfo().getName() + " is NOT compatible (Server-Protocol: " + plugin.getBackendPingService().getProtocol(s) + ")");

        }
        if(matches.isEmpty()){
            plugin.getLogger().info("No server found for this client");
            return null;
        }else{ //needs to be changed if more than two distribution modes exist
            RegisteredServer finalServer = matches.getFirst();
            if(distribution.equalsIgnoreCase("BALANCED")){
                for(RegisteredServer s : matches){
                    if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                }
            }
            return finalServer;
        }
    }

}
