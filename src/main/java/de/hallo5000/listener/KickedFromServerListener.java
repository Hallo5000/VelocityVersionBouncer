package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Main;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static de.hallo5000.main.Main.toml;

public class KickedFromServerListener {

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent e){
        if(toml.getBoolean("enable-fallback-bouncing")){
            if(toml.getString("explicit-fallback-server").equalsIgnoreCase("")){
                RegisteredServer firstMatch = null;
                Main.getLogger.info("[FALLBACK] Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol() + ")");
                Collection<RegisteredServer> serverList = Main.getServer.getAllServers();
                if(toml.getString("order-mode").equalsIgnoreCase("CUSTOM")) serverList = toml.getList("server-list").stream().map(x -> Main.getServer.getServer((String) x).get()).toList();
                if(toml.getBoolean("exclude-previous-server")) serverList.remove(e.getServer());
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
                }
                else{
                    Main.getLogger.info("Connects to: " + firstMatch.getServerInfo().getName());
                    e.setResult(KickedFromServerEvent.RedirectPlayer.create(firstMatch));

                }
            }else{
                Optional<RegisteredServer> fallback = Main.getServer.getServer(toml.getString("explicit-fallback-server"));
                if(fallback.isPresent()) e.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback.get()));
                else{
                    e.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(e.getServerKickReason()+"\nThe fallback server was not found.")));
                    Main.getLogger.info("It seems like the fallback server is offline and therefore " + e.getPlayer().getGameProfile().getName() + " was kicked from the server!");
                }
            }
        }
    }
}
