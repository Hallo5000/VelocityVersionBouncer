package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

public class PlayerChooseInitialServerListener {

    private final VelocityVersionBouncer plugin;

    public PlayerChooseInitialServerListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    private void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e){
        RegisteredServer match = plugin.getUtils().checkForExplicitRouting(e.getPlayer());
        if (match != null) {
            plugin.getLogger().info("Connects to explicitly declared server: " + match.getServerInfo().getName());
            e.setInitialServer(match);
            return;
        }

        //Toml Vars
        String distribution = Optional.ofNullable(plugin.getToml().getString("distribution")).orElse("FIRST-MATCH");

        //start checking
        plugin.getLogger().info("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version
        for(RegisteredServer s : plugin.getUtils().getConfigServerList()){
            plugin.getLogger().info("Check " + s.getServerInfo().getName() + " with protocol " + plugin.getBackendPingService().getProtocol(s));
            if(e.getPlayer().getProtocolVersion().getProtocol() == plugin.getBackendPingService().getProtocol(s).orElse(-1)){
                matches.add(s);
            }
        }
        if(matches.isEmpty()){
            e.getPlayer().disconnect(Component.text("Disconnected: There is no server with a matching game version available!"));
            plugin.getLogger().info("No server found for this client");
        }else{ //needs to be changed if more than two distribution modes exist
            RegisteredServer finalServer = matches.getFirst();
            if(distribution.equalsIgnoreCase("BALANCED")){
                for(RegisteredServer s : matches){
                    if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                }
            }
            plugin.getLogger().info("Connects to: " + finalServer.getServerInfo().getName());
            e.setInitialServer(finalServer);
        }
    }
}
