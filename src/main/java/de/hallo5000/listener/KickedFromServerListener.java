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
        plugin.getLogger().info("[FALLBACK-bouncing]");
        if(plugin.getToml().getBoolean("enable-fallback-bouncing")){
            if(plugin.getToml().getString("explicit-fallback-server").equalsIgnoreCase("")){ //there is no explicit fallback server
                RegisteredServer serverToExclude = plugin.getToml().getBoolean("exclude-previous-server") ? e.getServer() : null;
                RegisteredServer s = plugin.getUtils().findMatchingServer(e.getPlayer(), serverToExclude);
                if(e.getServer() == s){
                    plugin.getBackendPingService().ping(e.getServer()).join();
                    s = plugin.getUtils().findMatchingServer(e.getPlayer(), serverToExclude);
                }
                if(s != null){
                    plugin.getLogger().info("Connects to: " + s.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(s));
                }else{
                    if(e.getServerKickReason().isPresent())
                        e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand there is no fallback server with a matching game version available."))));
                    else
                        e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("There is no fallback server with a matching game version available.")));
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
