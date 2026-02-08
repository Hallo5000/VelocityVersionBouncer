package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProxyPingListener {

    VelocityVersionBouncer plugin;
    public ProxyPingListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent e){
        if(!plugin.getToml().getBoolean("server-list-ping")) return;

        plugin.getLogger().info("Server List Ping incoming...");
        RegisteredServer match = plugin.getUtils().checkForExplicitRouting(e.getConnection());
        if (match != null) {
            plugin.getLogger().info("Matches explicitly declared server: " + match.getServerInfo().getName());
            if(plugin.getBackendPingService().getPing(match).isPresent()) e.setPing(plugin.getBackendPingService().getPing(match).get());
            return;
        }

        //Toml Vars
        String distribution = Optional.ofNullable(plugin.getToml().getString("distribution")).orElse("FIRST-MATCH");

        //start checking
        plugin.getLogger().info("Start checking for compatibilities (Client-Protocol: " + e.getConnection().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version
        for(RegisteredServer s : plugin.getUtils().getConfigServerList()){
            plugin.getLogger().info("Check " + s.getServerInfo().getName() + " with protocol " + plugin.getBackendPingService().getProtocol(s));
            if(e.getConnection().getProtocolVersion().getProtocol() == plugin.getBackendPingService().getProtocol(s).orElse(-1)){
                matches.add(s);
            }
        }
        if(matches.isEmpty()){
            plugin.getLogger().info("No server found for this client");
        }else{ //needs to be changed if more than two distribution modes exist
            RegisteredServer finalServer = matches.getFirst();
            if(distribution.equalsIgnoreCase("BALANCED")){
                for(RegisteredServer s : matches){
                    if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                }
            }
            plugin.getLogger().info("Matches to: " + finalServer.getServerInfo().getName());
            if(plugin.getBackendPingService().getPing(finalServer).isPresent()) e.setPing(plugin.getBackendPingService().getPing(finalServer).get());
        }
    }

}
