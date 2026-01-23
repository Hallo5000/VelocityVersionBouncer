package de.hallo5000.pingHandler;

import com.velocitypowered.api.proxy.server.ServerPing;

/**
 * Represents a ping result from one of the backend servers
 * Currently just a wrapper for <code>ServerPing</code>
 */
public class BackendPingResult {

    private final ServerPing velocityPing; //can be replaced in the future with only possibly modifying getProtocol()
    //private final ServerPing.Version version;

    public BackendPingResult(ServerPing ping){
        this.velocityPing = ping;
    }

    /**
     * Gets the protocol version number from the ping
     */
    public int getProtocol(){
        return velocityPing.getVersion().getProtocol();
    }

}
