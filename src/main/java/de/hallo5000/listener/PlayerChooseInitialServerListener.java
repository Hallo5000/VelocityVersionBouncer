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
        plugin.getLogger().info("[Initial join - VersionBouncing");
        RegisteredServer s = plugin.getUtils().findMatchingServer(e.getPlayer());
        if(s != null){
            plugin.getLogger().info("Connects to: " + s.getServerInfo().getName());
            e.setInitialServer(s);
        }else e.getPlayer().disconnect(Component.text("Disconnected: There is no server with a matching game version available!"));
    }
}
