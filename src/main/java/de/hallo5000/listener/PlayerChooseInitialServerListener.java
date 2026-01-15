package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Main;
import net.kyori.adventure.text.Component;

import java.util.*;

import static de.hallo5000.main.Main.toml;

public class PlayerChooseInitialServerListener {

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e){
        /* for testing!!
        if(e.getPlayer().getProtocolVersion().getProtocol() == 767){
            e.setInitialServer(Main.getServer.getServer("Stoneblock-4").get());
            return;
        }*/
        List<RegisteredServer> matches = new ArrayList<>();
        Main.getLogger.info("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> serverList = new ArrayList<>(Main.getServer.getAllServers());
        if(toml.getString("order-mode").equalsIgnoreCase("CUSTOM")) serverList = new ArrayList<>(toml.getList("server-list").stream().map(x -> Main.getServer.getServer((String) x).orElse(null)).toList());
        if(serverList.remove(null)) Main.getLogger.info("One or more of the specified servers couldn't be found!");
        for(RegisteredServer s : serverList){
            if(!toml.getList("exclude-servers", Collections.emptyList()).contains(s.getServerInfo().getName())) {
                if(e.getPlayer().getProtocolVersion().getProtocol() == Main.pingMap.get(s).getVersion().getProtocol()) {
                    Main.getLogger.info("> " + s.getServerInfo().getName() + " is compatible with Protocol: " + Main.pingMap.get(s).getVersion().getProtocol());
                    matches.add(s);
                }else
                    Main.getLogger.info("> " + s.getServerInfo().getName() + " is NOT compatible with Protocol: " + Main.pingMap.get(s).getVersion().getProtocol());
            }else {
                Main.getLogger.info("> " + s.getServerInfo().getName() + " is EXCLUDED from checking");
            }
        }
        Main.getLogger.info("JA SAFE");
        if(matches.isEmpty()){
            e.getPlayer().disconnect(Component.text("Disconnected: There is no server with a matching game version available!"));
            Main.getLogger.info("No server found for this client");
        }else{
            RegisteredServer finalServer = matches.getFirst();
            if(toml.getString("distribution").equalsIgnoreCase("BALANCED")){
                for(RegisteredServer s : matches){
                    if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                }
            }
            Main.getLogger.info("Connects to: " + finalServer.getServerInfo().getName());
            e.setInitialServer(finalServer);
        }
    }
}
