package de.hallo5000.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.Main;

public class PlayerModInfoListener {

    /*
    modId = "pcf"
    version = "1.1.7"
    displayName = "ProxyCompatibleForge"
     */

    @Subscribe
    public void onModInfo(PlayerModInfoEvent e){
        if(e.getModInfo().getType().equals("FML2") || e.getModInfo().getType().equals("FML")){
            for(RegisteredServer s : Main.getServer.getAllServers()){
                //MODLIST CAN ONLY BE USED FOR FORGE (NOT FABRIC)
                // e.getModInfo(); for clients modlist (may be incomplete)
                // proxy can't access servers modlist (s.ping().get().getModinfo(); is always empty)
            }
        }
    }

}
