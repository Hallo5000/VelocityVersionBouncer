package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;

public class PlayerModInfoListener {

    /*
    modId = "pcf"
    version = "1.1.7"
    displayName = "ProxyCompatibleForge"
     */

    @Subscribe
    public void onModInfo(PlayerModInfoEvent e){
        /*if(e.getModInfo().getType().equals("FML2") || e.getModInfo().getType().equals("FML")){
            Main.getServer.sendMessage(Component.text("Start checking for compatibilities (Clientprotocol: " + e.getPlayer().getProtocolVersion().getProtocol()));
            for(RegisteredServer s : Main.getServer.getAllServers()){
                try {
                    if(e.getPlayer().getProtocolVersion().getProtocol() == s.ping().get().getVersion().getProtocol()){
                        Main.getServer.sendMessage(Component.text(s.getServerInfo().getName() + " is compatible"));

                    }else Main.getServer.sendMessage(Component.text(s.getServerInfo().getName() + " is NOT compatible"));
                } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }*/
    }

}
