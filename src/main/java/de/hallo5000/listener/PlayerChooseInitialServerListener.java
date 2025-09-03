package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Main;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.hallo5000.main.Main.toml;

public class PlayerChooseInitialServerListener {

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e){
        RegisteredServer firstMatch = null;
        Main.getLogger.info("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> serverList = new ArrayList<RegisteredServer>(Main.getServer.getAllServers());
        if(toml.getString("order-mode").equalsIgnoreCase("CUSTOM")) serverList = new ArrayList<>(toml.getList("server-list").stream().map(x -> Main.getServer.getServer((String) x).orElse(null)).toList());
        if(serverList.remove(null)) Main.getLogger.info("One or more of the specified servers couldn't be found!");
        for(RegisteredServer s : serverList){
            if(!toml.getList("exclude-servers", Collections.emptyList()).contains(s.getServerInfo().getName())) {
                try {
                    if (e.getPlayer().getProtocolVersion().getProtocol() == s.ping().get().getVersion().getProtocol()) {
                        Main.getLogger.info("> " + s.getServerInfo().getName() + " is compatible with Protocol: " + s.ping().get().getVersion().getProtocol());
                        if(toml.getBoolean("first-match")){
                            if(firstMatch == null) firstMatch = s;
                        }else firstMatch = s;
                    } else
                        Main.getLogger.info("> " + s.getServerInfo().getName() + " is NOT compatible with Protocol: " + s.ping().get().getVersion().getProtocol());
                } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
                    throw new RuntimeException(ex);
                }
            }else {
                Main.getLogger.info("> " + s.getServerInfo().getName() + " is EXCLUDED from checking");
            }
        }
        if(firstMatch == null){
            e.getPlayer().disconnect(Component.text("Diconnected: There is no server with a matching game version available!"));
            Main.getLogger.info("No server found for this client");
        } else{
            Main.getLogger.info("Connects to: " + firstMatch.getServerInfo().getName());
            e.setInitialServer(firstMatch);
        }
    }
}
