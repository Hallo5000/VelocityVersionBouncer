package de.hallo5000.main;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
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
     * @return the protocol version number from the packet or <code>-1</code> if not found
     */
    public static int getProtocolFromHandshake(String json){
        if(json == null) return -1;
        JsonParser parser = Json.createParser(new StringReader(json));
        boolean inVersion = false;

        while(parser.hasNext()){
            JsonParser.Event event = parser.next();
            if(event == JsonParser.Event.KEY_NAME){
                String key = parser.getString();
                if(!inVersion && key.equals("version")){
                    parser.next();//START_OBJECT
                    inVersion = true;
                }else if(inVersion && key.equals("protocol")){
                    parser.next();//VALUE_NUMBER
                    return parser.getInt();
                }
            }
        }
        return -1;
    }
}
