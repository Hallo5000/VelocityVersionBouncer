package de.hallo5000.pingHandler;

import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BackendPingService {

    private final Logger logger;
    private final VelocityVersionBouncer plugin;
    private final ProxyServer server;
    private final Map<RegisteredServer, BackendPingResult> pingCache;

    public BackendPingService(Logger logger, VelocityVersionBouncer plugin, ProxyServer server){
        this.logger = logger;
        this.plugin = plugin;
        this.server = server;
        this.pingCache = new ConcurrentHashMap<>();
    }

    public void start(){
        pingAll();
        server.getScheduler()
                .buildTask(plugin, this::pingAll)
                .delay(30, TimeUnit.SECONDS)
                .schedule();
        server.getEventManager().register(plugin, this);
    }

    public void pingAll(){
        logger.info("Pinging every Backend Server...");
        for(RegisteredServer s : server.getAllServers()){
            ping(s);
        }
        logger.info("Pings complete!");
    }

    public void ping(RegisteredServer s){
        s.ping().whenComplete((result, error) -> {
            if (error != null) {
                logger.warn(
                        "Ping FAILED for " + s.getServerInfo().getName(),
                        error
                );
            }else{
                pingCache.put(s, new BackendPingResult(result));
                logger.info(result.toString());
            }
        });
    }

    public void onServerRegistered(ServerRegisteredEvent e){
        e.registeredServer().ping().whenComplete((ping, error) -> {
            if(error != null) return; //possible error handling on ping fail
            pingCache.put(e.registeredServer(), new BackendPingResult(ping));
        });
    }

    public Map<RegisteredServer, BackendPingResult> getPingCache(){
        return Collections.unmodifiableMap(this.pingCache);
    }

}
