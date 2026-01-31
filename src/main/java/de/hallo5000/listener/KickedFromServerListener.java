package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

public class KickedFromServerListener {

    private final VelocityVersionBouncer plugin;

    public KickedFromServerListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent e){
        if(plugin.getToml().getBoolean("enable-fallback-bouncing")){
            if(plugin.getToml().getString("explicit-fallback-server").equalsIgnoreCase("")){ //there is no explicit fallback server
                plugin.getLogger().info("[FALLBACK] Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
                RegisteredServer match = plugin.getUtils().checkForExplicitRouting(e.getPlayer());
                if (match != null && !(plugin.getToml().getBoolean("exclude-previous-server") && match == e.getServer())) {
                    plugin.getLogger().info("Connects to explicitly declared server: " + match.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(match));
                    return;
                }
                List<RegisteredServer> matches = new ArrayList<>();
                List<RegisteredServer> serverList = plugin.getUtils().getConfigServerList();
                if(serverList.remove(null)) plugin.getLogger().info("One or more of the specified servers couldn't be found!");
                if(plugin.getToml().getBoolean("exclude-previous-server")) serverList.remove(e.getServer());
                for(RegisteredServer s : serverList){
                    if (e.getPlayer().getProtocolVersion().getProtocol() == plugin.getBackendPingService().getProtocol(s).orElse(-1)) {
                        matches.add(s);
                        plugin.getLogger().info("> " + s.getServerInfo().getName() + " is compatible with Protocol: " + plugin.getBackendPingService().getProtocol(s));
                    } else
                        plugin.getLogger().info("> " + s.getServerInfo().getName() + " is NOT compatible with Protocol: " + plugin.getBackendPingService().getProtocol(s));
                }
                if(matches.isEmpty()){
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand there is no fallback server with a matching game version available."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("There is no fallback server with a matching game version available.")));
                    plugin.getLogger().info("No fallback server found for this client");
                }else{ //needs to be changed if more than two distribution modes exist
                    RegisteredServer finalServer = matches.getFirst();
                    if(plugin.getToml().getString("distribution").equalsIgnoreCase("BALANCED")){
                        for(RegisteredServer s : matches){
                            if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                        }
                    }
                    plugin.getLogger().info("Connects to: " + finalServer.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(finalServer));
                }
            }else{
                Optional<RegisteredServer> fallback = plugin.getServer().getServer(plugin.getToml().getString("explicit-fallback-server"));
                if(fallback.isPresent()) e.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get()));
                else{
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand the fallback server is currently unavailable."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("The fallback server is currently unavailable.")));
                    plugin.getLogger().info("It seems like the fallback server is offline and therefore " + e.getPlayer().getGameProfile().getName() + " was kicked from the server!");
                }
            }
        }
    }
}
