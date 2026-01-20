package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Utils;
import de.hallo5000.main.VelocityVersionBouncer;
import net.kyori.adventure.text.Component;

import java.util.*;

import static de.hallo5000.main.VelocityVersionBouncer.toml;

public class KickedFromServerListener {

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent e){
        if(toml.getBoolean("enable-fallback-bouncing")){
            if(toml.getString("explicit-fallback-server").equalsIgnoreCase("")){ //there is no explicit fallback server
                /* no explicit routing yet
                List<String> matchingKeys = toml.getTable("explicit-routing").toMap().keySet().stream().filter(x -> x.startsWith("p"+e.getPlayer().getProtocolVersion().getProtocol())).toList();
                if(!matchingKeys.isEmpty()){
                    String matchName = (String) toml.getTable("explicit-routing").toMap().get("p"+e.getPlayer().getProtocolVersion().getProtocol()+"c"+e.getPlayer().getClientBrand());
                    if(matchName == null) matchName = (String) toml.getTable("explicit-routing").toMap().get("p"+e.getPlayer().getProtocolVersion().getProtocol());
                    RegisteredServer match = VelocityVersionBouncer.getServer.getServer(matchName).orElse(null);
                    //if()
                }*/
                List<RegisteredServer> matches = new ArrayList<>();
                VelocityVersionBouncer.getLogger.info("[FALLBACK] Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
                List<RegisteredServer> serverList = Utils.getConfigServerList();
                if(serverList.remove(null)) VelocityVersionBouncer.getLogger.info("One or more of the specified servers couldn't be found!");
                if(toml.getBoolean("exclude-previous-server")) serverList.remove(e.getServer());
                for(RegisteredServer s : serverList){
                    if (e.getPlayer().getProtocolVersion().getProtocol() == VelocityVersionBouncer.ps.getProtocol(s).orElse(-1)) {
                        matches.add(s);
                        VelocityVersionBouncer.getLogger.info("> " + s.getServerInfo().getName() + " is compatible with Protocol: " + VelocityVersionBouncer.ps.getPingCache().get(s).getProtocol());
                    } else
                        VelocityVersionBouncer.getLogger.info("> " + s.getServerInfo().getName() + " is NOT compatible with Protocol: " + VelocityVersionBouncer.ps.getPingCache().get(s).getProtocol());
                }
                if(matches.isEmpty()){
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand there is no fallback server with a matching game version available."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("There is no fallback server with a matching game version available.")));
                    VelocityVersionBouncer.getLogger.info("No fallback server found for this client");
                }else{
                    RegisteredServer finalServer = matches.getFirst();
                    if(toml.getString("distribution").equalsIgnoreCase("BALANCED")){
                        for(RegisteredServer s : matches){
                            if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                        }
                    }
                    VelocityVersionBouncer.getLogger.info("Connects to: " + finalServer.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(finalServer));
                }
            }else{
                Optional<RegisteredServer> fallback = VelocityVersionBouncer.getServer.getServer(toml.getString("explicit-fallback-server"));
                if(fallback.isPresent()) e.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get()));
                else{
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand the fallback server is currently unavailable."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("The fallback server is currently unavailable.")));
                    VelocityVersionBouncer.getLogger.info("It seems like the fallback server is offline and therefore " + e.getPlayer().getGameProfile().getName() + " was kicked from the server!");
                }
            }
        }
    }
}
