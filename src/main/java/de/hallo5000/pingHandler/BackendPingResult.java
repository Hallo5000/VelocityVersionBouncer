package de.hallo5000.pingHandler;

import com.velocitypowered.api.proxy.server.ServerPing;

public class BackendPingResult {

    private final ServerPing velocityPing; //can be replaced in the future with only possibly modifying getProtocol()

    public BackendPingResult(ServerPing ping){
        this.velocityPing = ping;
    }

    public int getProtocol(){
        return velocityPing.getVersion().getProtocol();
    }

}
