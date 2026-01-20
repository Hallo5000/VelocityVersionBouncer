package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

import static de.hallo5000.main.VelocityVersionBouncer.toml;

public class PlayerChooseInitialServerListener {

    @Subscribe
    private void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e){
        //only for testing!!
        if(e.getPlayer().getProtocolVersion().getProtocol() == 767){
            e.setInitialServer(VelocityVersionBouncer.getServer.getServer("Stoneblock-4").get());
            return;
        }


        //Toml Vars
        String distribution = Optional.ofNullable(toml.getString("distribution")).orElse("FIRST-MATCH");

        //take all backend servers or only the ones provided by the whitelist (if one exists) and remove the ones on the blacklist
        List<RegisteredServer> serverList = new ArrayList<>(Optional.ofNullable(toml.getList("whitelist", (new ArrayList<>(VelocityVersionBouncer.getServer.getAllServers())).stream().map(s -> s.getServerInfo().getName()).toList()))
                .orElse((new ArrayList<>(VelocityVersionBouncer.getServer.getAllServers())).stream().map(s -> s.getServerInfo().getName()).toList())
                .stream().map(name -> VelocityVersionBouncer.getServer.getServer(name).orElseGet(() -> {
                    VelocityVersionBouncer.getLogger.info("'" + name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).toList());

        List<RegisteredServer> blacklist = Optional.ofNullable(toml.getList("blacklist", Collections.emptyList()))
                .orElse(Collections.emptyList())
                .stream().map(name -> VelocityVersionBouncer.getServer.getServer((String) name).orElseGet(() -> {
                    VelocityVersionBouncer.getLogger.info("'"+ name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).toList();
        serverList.removeAll(blacklist);

        //start checking
        VelocityVersionBouncer.getLogger.info("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version

        for(RegisteredServer s : serverList){
            if(e.getPlayer().getProtocolVersion().getProtocol() == VelocityVersionBouncer.ps.getPingCache().get(s).getProtocol()){
                matches.add(s);
            }
        }
        if(matches.isEmpty()){
            e.getPlayer().disconnect(Component.text("Disconnected: There is no server with a matching game version available!"));
            VelocityVersionBouncer.getLogger.info("No server found for this client");
        }else{ //needs to be changed if more than two distribution modes exist
            RegisteredServer finalServer = matches.getFirst();
            if(distribution.equalsIgnoreCase("BALANCED")){
                for(RegisteredServer s : matches){
                    if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                }
            }
            VelocityVersionBouncer.getLogger.info("Connects to: " + finalServer.getServerInfo().getName());
            e.setInitialServer(finalServer);
        }
    }
}
