package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Main;
import net.kyori.adventure.text.Component;

import java.util.*;

import static de.hallo5000.main.Main.toml;

public class KickedFromServerListener {

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent e){
        if(toml.getBoolean("enable-fallback-bouncing")){
            if(toml.getString("explicit-fallback-server").equalsIgnoreCase("")){
                List<String> matchingKeys = toml.getTable("explicit-routing").toMap().keySet().stream().filter(x -> x.startsWith("p"+e.getPlayer().getProtocolVersion().getProtocol())).toList();
                if(!matchingKeys.isEmpty()){
                    String matchName = (String) toml.getTable("explicit-routing").toMap().get("p"+e.getPlayer().getProtocolVersion().getProtocol()+"c"+e.getPlayer().getClientBrand());
                    if(matchName == null) matchName = (String) toml.getTable("explicit-routing").toMap().get("p"+e.getPlayer().getProtocolVersion().getProtocol());
                    RegisteredServer match = Main.getServer.getServer(matchName).orElse(null);
                    //if()
                }
                List<RegisteredServer> matches = new ArrayList<>();
                Main.getLogger.info("[FALLBACK] Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
                List<RegisteredServer> serverList = new ArrayList<>(Main.getServer.getAllServers());
                if(toml.getString("order-mode").equalsIgnoreCase("CUSTOM")) serverList = new ArrayList<>(toml.getList("server-list").stream().map(x -> Main.getServer.getServer((String) x).orElse(null)).toList());
                if(serverList.remove(null)) Main.getLogger.info("One or more of the specified servers couldn't be found!");
                if(toml.getBoolean("exclude-previous-server")) serverList.remove(e.getServer());
                for(RegisteredServer s : serverList){
                    if(!toml.getList("exclude-servers", Collections.emptyList()).contains(s.getServerInfo().getName())) {
                        if (e.getPlayer().getProtocolVersion().getProtocol() == Main.pingMap.get(s).getVersion().getProtocol()) {
                            matches.add(s);
                            Main.getLogger.info("> " + s.getServerInfo().getName() + " is compatible with Protocol: " + Main.pingMap.get(s).getVersion().getProtocol());
                        } else
                            Main.getLogger.info("> " + s.getServerInfo().getName() + " is NOT compatible with Protocol: " + Main.pingMap.get(s).getVersion().getProtocol());
                    }else {
                        Main.getLogger.info("> " + s.getServerInfo().getName() + " is EXCLUDED from checking");
                    }
                }
                if(matches.isEmpty()){
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand there is no fallback server with a matching game version available."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("There is no fallback server with a matching game version available.")));
                    Main.getLogger.info("No fallback server found for this client");
                }else{
                    RegisteredServer finalServer = matches.getFirst();
                    if(toml.getString("distribution").equalsIgnoreCase("BALANCED")){
                        for(RegisteredServer s : matches){
                            if(s.getPlayersConnected().size() < finalServer.getPlayersConnected().size()) finalServer = s;
                        }
                    }
                    Main.getLogger.info("Connects to: " + finalServer.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(finalServer));
                }
            }else{
                Optional<RegisteredServer> fallback = Main.getServer.getServer(toml.getString("explicit-fallback-server"));
                if(fallback.isPresent()) e.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get()));
                else{
                    if(e.getServerKickReason().isPresent()) e.setResult(KickedFromServerEvent.DisconnectPlayer.create(e.getServerKickReason().get().append(Component.text("\nand the fallback server is currently unavailable."))));
                    else e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("The fallback server is currently unavailable.")));
                    Main.getLogger.info("It seems like the fallback server is offline and therefore " + e.getPlayer().getGameProfile().getName() + " was kicked from the server!");
                }
            }
        }
    }
}
