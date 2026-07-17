package de.hallo5000.main;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import de.hallo5000.pingHandler.PingHandler;
import jakarta.json.Json;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
                    plugin.getLogger().info(plugin.getMessage("cant-found", name.toString()));
                    return null;
                })).filter(Objects::nonNull).collect(Collectors.toList());
        if(serverList.isEmpty()) serverList = new ArrayList<>(plugin.getServer().getAllServers());
        List<RegisteredServer> blacklist = Optional.ofNullable(plugin.getToml().getList("blacklist"))
                .orElse(new ArrayList<>(Collections.emptyList()))
                .stream().map(name -> plugin.getServer().getServer((String) name).orElseGet(() -> {
                    plugin.getLogger().info(plugin.getMessage("cant-found", name.toString()));
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
        if(inboundConnection instanceof Player p && p.getClientBrand() != null) clientBrand = p.getClientBrand();
        
        Map<String, Object> explicitRoutings = new LinkedHashMap<>(plugin.getToml().getTable("explicit-routing").toMap());
        //removes all invalid explicit routings
        for(String s : explicitRoutings.keySet()){
            if(s.isEmpty() || !s.matches("^(?=[pvc])((p\\d{3})|(v1_\\d+_\\d+))?(c\\S+)?$")) explicitRoutings.remove(s);
        }
        //removes all explicit routings with unmatching client brands
        for(String s : new HashSet<>(explicitRoutings.keySet())){
            if(s.contains("c") && !s.endsWith("c"+clientBrand)) explicitRoutings.remove(s);
        }

        //matching protocol version + client brand
        String serverName = (String) explicitRoutings.get("p"+protocol+"c"+clientBrand);
        if(serverName != null) return plugin.getServer().getServer(serverName).orElse(null);
        //matching protocol version without client brand
        serverName = (String) explicitRoutings.get("p"+protocol+"c"+clientBrand);
        if(serverName != null) return plugin.getServer().getServer(serverName).orElse(null);

        //matching game versions + client brand
        for(String v : inboundConnection.getProtocolVersion().getVersionsSupportedBy().stream().map(s -> s.replace('.', '_')).toList()){
                serverName = (String) explicitRoutings.get("v"+v+"c"+clientBrand);
                if(serverName != null) return plugin.getServer().getServer(serverName).orElse(null);
        }
        //matching game versions without client brand
        for(String v : inboundConnection.getProtocolVersion().getVersionsSupportedBy().stream().map(s -> s.replace('.', '_')).toList()){
            serverName = (String) explicitRoutings.get("v"+v);
            if(serverName != null) return plugin.getServer().getServer(serverName).orElse(null);
        }

        //match with only client brand
        if(explicitRoutings.containsKey("c"+clientBrand))
            return plugin.getServer().getServer((String) explicitRoutings.get("c"+clientBrand)).orElse(null);

        return null;
    }

    /**
     * Takes in a JSON Payload from the Status Response packet in a Server List Ping
     * @param json the JSON response field (can be obtained by {@link PingHandler#ping(RegisteredServer)  ping()})
     * @return a <code>ServerPing</code> containing every field obtainable from <code>json</code> (if not obtainable the field is set to the given default value)
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

    /**
     * {@link #getPingFromHandshake(String, int, String, int, int, List, Component, Favicon, String, List) getPingFromHandshake()} but with pre-set defaults
     * @param json the json string to parse the <code>ServerPing</code> from
     * @return a server ping derived from the json string
     */
    public @NotNull ServerPing getPingFromHandshake(@NotNull String json){
        return getPingFromHandshake(json, -1, "", -1, -1, Collections.emptyList(), Component.empty(), null, "FML", Collections.emptyList());
    }

    /**
     * First checks if there is an explicit routing in the config with a matching protocol/game version and/or client brand.
     * When there is no explicit routing it checks every server specified by whitelist and blacklist. The first server found by this method will be returned.
     * @param client player to find a matching server for
     * @param serverToExclude <code>RegisteredServer</code> to be excluded from checking (could be <code>null</code> to exclude none)
     * @return a server with matching protocol version (the <code>RegisteredServer</code> inside the <code>CompletableFuture</code> can be <code>null</code> if no server was found or <code>client</code> is <code>null</code>)
     */
    public @NotNull CompletableFuture<RegisteredServer> findMatchingServer(@Nullable InboundConnection client, @Nullable RegisteredServer serverToExclude){
        if(client == null) return CompletableFuture.completedFuture(null);
        RegisteredServer match = plugin.getUtils().checkForExplicitRouting(client);
        if(match != null) {
            plugin.getLogger().info(plugin.getMessage("found-explicit-routing", match.getServerInfo().getName()));
            return CompletableFuture.completedFuture(match);
        }

        //start checking
        plugin.getLogger().info(plugin.getMessage("start-checking", String.valueOf(client.getProtocolVersion().getProtocol())));
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version
        List<RegisteredServer> servers = plugin.getUtils().getConfigServerList();
        List<RegisteredServer> offlineServers = new ArrayList<>(servers);
        offlineServers.removeAll(plugin.getBackendPingService().getPingCache().keySet());
        //check if offline servers are still offline
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for(RegisteredServer s : offlineServers){
            futures.add(CompletableFuture.supplyAsync(() -> {
                try(Socket socket = new Socket()){
                    socket.connect(s.getServerInfo().getAddress(), 1000);
                    return true;
                }catch(IOException ex){
                    return false;
                }
            }).thenCompose((reachable) -> {
                if(reachable) return plugin.getBackendPingService().ping(s);
                return CompletableFuture.completedFuture(null);
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).handle((v, t) -> v)
                .thenApply((v) -> {
            //start checking servers for matches
            for(RegisteredServer s : servers){
                if(serverToExclude != null && s == serverToExclude){
                    plugin.getLogger().info(plugin.getMessage("server-excluded", s.getServerInfo().getName()));
                }else if(plugin.getBackendPingService().getProtocol(s).isEmpty()){
                    plugin.getLogger().info(plugin.getMessage("server-unavailable", s.getServerInfo().getName()));
                }else if(client.getProtocolVersion().getProtocol() == plugin.getBackendPingService().getProtocol(s).getAsInt()){
                    matches.add(s);
                    plugin.getLogger().info(plugin.getMessage("server-compatible", s.getServerInfo().getName(), String.valueOf(plugin.getBackendPingService().getProtocol(s).getAsInt())));
                }else
                    plugin.getLogger().info(plugin.getMessage("server-not-compatible", s.getServerInfo().getName(), String.valueOf(plugin.getBackendPingService().getProtocol(s).getAsInt())));
            }
            if(matches.isEmpty()){
                plugin.getLogger().info(plugin.getMessage("no-server-found"));
                return null;
            }
            while(!matches.isEmpty()){
                //needs to be changed if more than two distribution modes are implemented
                RegisteredServer finalServer = matches.getFirst();
                if("BALANCED".equalsIgnoreCase(plugin.getToml().getString("distribution"))){
                    for(RegisteredServer s : matches){
                        if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                    }
                }
                try(Socket socket = new Socket()){
                    socket.connect(finalServer.getServerInfo().getAddress(), 1000);
                    return finalServer;
                }catch(IOException ex){
                    plugin.getLogger().info(plugin.getMessage("server-unreachable"));
                    plugin.getBackendPingService().removePing(finalServer);
                    matches.remove(finalServer);
                }
            }
            plugin.getLogger().info(plugin.getMessage("no-server-found"));
            return null;
        });
    }

}
