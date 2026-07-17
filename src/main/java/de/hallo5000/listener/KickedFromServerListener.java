package de.hallo5000.listener;

import com.velocitypowered.api.event.Continuation;
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
    public void onPlayerKick(KickedFromServerEvent e, Continuation continuation){
        plugin.getLogger().info(plugin.getMessage("fallback-bouncing"));
        if(plugin.getToml().getBoolean("enable-fallback-bouncing")){
            if(plugin.getToml().getString("explicit-fallback-server").equalsIgnoreCase("")){ //there is no explicit fallback server
                RegisteredServer serverToExclude = plugin.getToml().getBoolean("exclude-previous-server") ? e.getServer() : null;
                plugin.getUtils().findMatchingServer(e.getPlayer(), serverToExclude)
                        .whenComplete((s, t) -> {
                            if(s != null){
                                plugin.getLogger().info(plugin.getMessage("connecting", s.getServerInfo().getName()));
                                e.setResult(KickedFromServerEvent.RedirectPlayer.create(s));
                            }else{
                                if(e.getServerKickReason().isPresent())
                                    e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(plugin.getMessage(
                                            "no-matching-server-args", e.getServerKickReason().get().toString()))));
                                else
                                    e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(plugin.getMessage("no-matching-server"))));
                            }
                            if(t != null) continuation.resumeWithException(t);
                            else continuation.resume();
                        });
                return;
            }else{
                Optional<RegisteredServer> fallback = plugin.getServer().getServer(plugin.getToml().getString("explicit-fallback-server"));
                if(fallback.map(rs -> plugin.getBackendPingService().ping(rs)).isPresent())
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get()));
                else{
                    if(e.getServerKickReason().isPresent())
                        e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text(plugin.getMessage("fallback-server-unavailable-kick")))));
                    else
                        e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(plugin.getMessage("fallback-server-unavailable"))));
                    plugin.getLogger().info(plugin.getMessage("fallback-server-unavailable-console", e.getPlayer().getGameProfile().getName()));
                }
            }
        }
        continuation.resume();
    }
}
