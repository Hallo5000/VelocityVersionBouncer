package de.hallo5000.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.HandshakeIntent;
import de.hallo5000.main.VelocityVersionBouncer;

public class ProxyPingListener {

    private final VelocityVersionBouncer plugin;
    public ProxyPingListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent e, Continuation continuation){
        if(e.getConnection().getHandshakeIntent() != HandshakeIntent.STATUS){
            continuation.resume();
            return;
        }
        if(!plugin.getToml().getBoolean("server-list-ping")){
            continuation.resume();
            return;
        }

        if(!plugin.getToml().getBoolean("default-to-ping-passthrough")) e.setResult(ResultedEvent.GenericResult.denied());
        if(e.getConnection().getProtocolVersion().getProtocol() == -2){ //legacy ping (most likely because the client got no response in a previous attempt)
            continuation.resume();
            return;
        }

        plugin.getLogger().info("Server List Ping incoming...");
        plugin.getUtils().findMatchingServer(e.getConnection(), null)
                .whenComplete((s, t) -> {
                    if(s != null){
                        plugin.getBackendPingService().getPing(s).ifPresentOrElse((ping) ->{
                            plugin.getLogger().info("Send ping response: " + s.getServerInfo().getName());
                            e.setPing(ping);
                            e.setResult(ResultedEvent.GenericResult.allowed());
                        },() -> plugin.getLogger().info("No server ping found for " + s.getServerInfo().getName()));
                    }
                    if(t != null) continuation.resumeWithException(t);
                    else continuation.resume();
                });
    }

}
