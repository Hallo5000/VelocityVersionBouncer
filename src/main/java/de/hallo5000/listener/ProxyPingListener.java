package de.hallo5000.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;

public class ProxyPingListener {

    private final VelocityVersionBouncer plugin;
    public ProxyPingListener(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent e){
        if(!plugin.getToml().getBoolean("server-list-ping")) return;

        plugin.getLogger().info("Server List Ping incoming...");

        RegisteredServer s = plugin.getUtils().findMatchingServer(e.getConnection());
        if(s != null){
            if(plugin.getBackendPingService().getPing(s).isPresent()){
                plugin.getLogger().info("Send ping response: " + s.getServerInfo().getName());
                e.setPing(plugin.getBackendPingService().getPing(s).get());
                e.setResult(ResultedEvent.GenericResult.allowed());
                return;
            }
        }
        if(!plugin.getToml().getBoolean("default-to-ping-passthrough")) e.setResult(ResultedEvent.GenericResult.denied());
    }

}
