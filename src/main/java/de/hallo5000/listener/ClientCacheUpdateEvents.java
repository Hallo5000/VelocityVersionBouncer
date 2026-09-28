package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import de.hallo5000.main.VelocityVersionBouncer;

public class ClientCacheUpdateEvents {

    private final VelocityVersionBouncer plugin;

    public ClientCacheUpdateEvents(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent e){
        plugin.clientCache.remove(e.getPlayer().getSessionId());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent e){
        plugin.clientCache.remove(e.getPlayer().getSessionId());
    }
}
