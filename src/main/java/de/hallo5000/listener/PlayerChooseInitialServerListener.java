package de.hallo5000.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

public class PlayerChooseInitialServerListener {

    private final VelocityVersionBouncer plugin;

    public PlayerChooseInitialServerListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e, Continuation continuation){
        plugin.getLogger().info(plugin.getMessage("initial-join"));
        plugin.getUtils().findMatchingServer(e.getPlayer(), null)
                .whenComplete((s, t) -> {
                    if(s != null){
                        plugin.getLogger().info(plugin.getMessage("connecting", s.getServerInfo().getName()));
                        e.setInitialServer(s);
                    }else e.getPlayer().disconnect(Component.text(plugin.getMessage("no-matching-server-player")));
                    if(t != null) continuation.resumeWithException(t);
                    else continuation.resume();
                });
    }
}
