package de.hallo5000.main;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
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
     * @param player the client whose protocol to compare the explicit routings to
     * @return the first found server or null if no server is found
     * @throws NullPointerException when player is null
     */
    public @Nullable RegisteredServer checkForExplicitRouting(Player player){
        Map<String, Object> explicitRoutings = new TreeMap<>();
        //takes all valid explicit routings
        for(String s : plugin.getToml().getTable("explicit-routing").toMap().keySet()){
            if(!s.isEmpty() && s.matches("^((p\\d{3})|(v\\d+.\\d+.\\d+))?(c[^\\d]+)?$")) explicitRoutings.put(s, plugin.getToml().getTable("explicit-routing").toMap().get(s));
        }
        //removes all explicit routings with unmatching client brands
        for(String s : new HashSet<>(explicitRoutings.keySet())){
            if(s.contains("c") && !s.endsWith("c"+player.getClientBrand())) explicitRoutings.remove(s);
        }
        //matching protocol version + optional client brand
        if(explicitRoutings.keySet().stream().anyMatch(r -> r.startsWith("p"+player.getProtocolVersion().getProtocol()))) {
            //at this point there is at least one match for the protocol version number
            for(String s : new HashSet<>(explicitRoutings.keySet())){
                if(s.startsWith("p")){
                    if(!s.startsWith("p"+player.getProtocolVersion().getProtocol())) explicitRoutings.remove(s);
                    else if(s.endsWith("c"+player.getClientBrand()))
                        return plugin.getServer().getServer((String) explicitRoutings.get(s)).orElse(null);
                }
            }
            String serverName = (String) explicitRoutings.get("p" + player.getProtocolVersion().getProtocol());
            return plugin.getServer().getServer(serverName).orElse(null);
        }
        //matching game versions + optional client brand
        for(String v : player.getProtocolVersion().getVersionsSupportedBy()){
            if(explicitRoutings.keySet().stream().anyMatch(r -> r.startsWith("v"+v))){
                for(String s : new HashSet<>(explicitRoutings.keySet())){
                    if(s.startsWith("v")){
                        if(!s.startsWith("v"+v)) explicitRoutings.remove(s);
                        else if(s.endsWith("c"+player.getClientBrand()))
                            return plugin.getServer().getServer((String) explicitRoutings.get(s)).orElse(null);
                    }
                }
                String serverName = (String) explicitRoutings.get("v"+v);
                return plugin.getServer().getServer(serverName).orElse(null);
            }
        }
        //match with only client brand
        if(explicitRoutings.containsKey("c"+player.getClientBrand()))
            return plugin.getServer().getServer((String) explicitRoutings.get("c"+player.getClientBrand())).orElse(null);
        return null;
    }

    /**
     * Takes in a JSON Payload from the Status Response packet in a Server List Ping
     * @param json the JSON response field (can be obtained by <code>PingHandler.ping();</code>
     * @return a <code>ServerPing</code> containing every field obtainable from <code>json</code> (if not obtainable the field is replaced by the given default value)
     */
    public @NotNull ServerPing getPingFromHandshake(@NotNull String json,
                                                               int defaultVersionProtocol,
                                                               String defaultVersionName,
                                                               int defaultPlayersOnline,
                                                               int defaultPlayersMax,
                                                               List<ServerPing.SamplePlayer> defaultPlayersSample,
                                                               Component defaultDescription,
                                                               @Nullable Favicon defaultFavicon){
        int protocol = plugin.getJsonReader().getIntFromJson(json, new String[]{"version", "protocol"}).orElse(defaultVersionProtocol);
        String name = plugin.getJsonReader().getStringFromJson(json, new String[]{"version", "name"}).orElse(defaultVersionName);
        int online = plugin.getJsonReader().getIntFromJson(json, new String[]{"players", "online"}).orElse(defaultPlayersOnline);
        int max = plugin.getJsonReader().getIntFromJson(json, new String[]{"players", "max"}).orElse(defaultPlayersMax);
        List<ServerPing.SamplePlayer> sample = plugin.getJsonReader().getJsonFromJson(json, new String[]{"players", "sample"})
                        .map(jsonArray -> new Gson().fromJson(jsonArray, ServerPing.SamplePlayer[].class))
                        .map(Arrays::asList)
                        .orElse(defaultPlayersSample);
        Component description = plugin.getJsonReader().getJsonFromJson(json, new String[]{"description"}).map(j -> JSONComponentSerializer.json().deserialize(j)).orElse(defaultDescription);
        Favicon favicon = plugin.getJsonReader().getStringFromJson(json, new String[]{"favicon"}).map(f -> new Favicon(f.split(",")[1])).orElse(defaultFavicon);

        return new ServerPing(new ServerPing.Version(protocol, name), new ServerPing.Players(online, max, sample), description, favicon);
    }

    public @NotNull ServerPing getPingFromHandshake(@NotNull String json){
        return getPingFromHandshake(json, -1, "", -1, -1, Collections.emptyList(), Component.empty(), null);
    }

}
