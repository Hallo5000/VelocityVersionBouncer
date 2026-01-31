package de.hallo5000.pingHandler;

import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;

/**
 * Represents a ping result from one of the backend servers
 * Currently just a wrapper for <code>ServerPing</code>
 */
public class BackendPingResult {

    private final ServerPing velocityPing; //can be replaced in the future with only possibly modifying getProtocol()

    public BackendPingResult(ServerPing ping){
        this.velocityPing = ping;
    }

    /**
     * Internally creates a <code>ServerPing</code> object with empty fields except for the protocol version
     * @param protocolVersionNumber the protocol version as int wrapped in this class
     */
    public BackendPingResult(int protocolVersionNumber){
        this.velocityPing = new ServerPing(new ServerPing.Version(protocolVersionNumber, ""), null, Component.empty(), null);
    }

    /**
     * Gets the protocol version number from the ping
     */
    public int getProtocol(){
        return velocityPing.getVersion().getProtocol();
    }

}
