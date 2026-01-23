package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Utils;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

import static de.hallo5000.main.VelocityVersionBouncer.toml;

public class PlayerChooseInitialServerListener {

    @Subscribe
    private void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent e){
        RegisteredServer match = Utils.checkForExplicitRouting(e.getPlayer());
        if (match != null) {
            VelocityVersionBouncer.getLogger.info("Connects to explicitly declared server: " + match.getServerInfo().getName());
            e.setInitialServer(match);
            return;
        }

        //Toml Vars
        String distribution = Optional.ofNullable(toml.getString("distribution")).orElse("FIRST-MATCH");

        //start checking
        VelocityVersionBouncer.getLogger.info("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
        List<RegisteredServer> matches = new ArrayList<>(); //every server with matching protocol version
        VelocityVersionBouncer.getLogger.info(Utils.getConfigServerList().toString());
        for(RegisteredServer s : Utils.getConfigServerList()){
            VelocityVersionBouncer.getLogger.info("Check " + s.getServerInfo().getName() + " with protocol " + VelocityVersionBouncer.ps.getProtocol(s));
            if(e.getPlayer().getProtocolVersion().getProtocol() == VelocityVersionBouncer.ps.getProtocol(s).orElse(-1)){
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
