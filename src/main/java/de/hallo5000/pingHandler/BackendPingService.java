package de.hallo5000.pingHandler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.event.proxy.server.ServerUnregisteredEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
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
        server.getScheduler()
                .buildTask(plugin, this::pingAll)
                .repeat(60, TimeUnit.SECONDS)
                .schedule();
        server.getEventManager().register(plugin, this);
    }

    public void pingAll(){
        logger.info("Pinging every Backend Server...");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for(RegisteredServer s : server.getAllServers()){
            futures.add(ping(s));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, e) -> logger.info("Pings complete!"));
    }

    public CompletableFuture<?> ping(RegisteredServer s){
        return s.ping().whenComplete((result, error) -> {
            if (error != null) {
                if(error instanceof java.net.ConnectException
                        || error instanceof java.net.NoRouteToHostException
                        || error instanceof java.net.SocketTimeoutException) logger.info("Ping FAILED for " + s.getServerInfo().getName() + " (server is offline)");
                if(error instanceof io.netty.handler.codec.CorruptedFrameException //may be irrelevant because the CorruptedFrameException is handled by the Decoder Exception
                        || error instanceof io.netty.handler.codec.DecoderException) logger.info("Ping FAILED for " + s.getServerInfo().getName() + " (ping response is corrupted)");
            }else{
                pingCache.put(s, new BackendPingResult(result));
                logger.info("Ping SUCCESSFUL for " + s.getServerInfo().getName() + " - protocol version: " + pingCache.get(s).getProtocol());
            }
        });
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent e){
        logger.info("New server registered. Pinging...");
        e.registeredServer().ping().whenComplete((result, error) -> {
            if (error != null) {
                if(error instanceof java.net.ConnectException
                        || error instanceof java.net.NoRouteToHostException
                        || error instanceof java.net.SocketTimeoutException) logger.warn("Ping FAILED for " + e.registeredServer().getServerInfo().getName() + " (server is offline)");
                if(error instanceof io.netty.handler.codec.CorruptedFrameException //may be irrelevant because the CorruptedFrameException is handled by the Decoder Exception
                        || error instanceof io.netty.handler.codec.DecoderException) logger.warn("Ping FAILED for " + e.registeredServer().getServerInfo().getName() + " (ping response is corrupted)");
            }else{
                pingCache.put(e.registeredServer(), new BackendPingResult(result));
                logger.info(e.registeredServer().getServerInfo().getName() + ": " + result.toString());
            }
        });
    }

    /* not safe as this exposes nearly the entire backend
    public Map<RegisteredServer, BackendPingResult> getPingCache(){
        return Collections.unmodifiableMap(this.pingCache);
    }*/

    public OptionalInt getProtocol(RegisteredServer server){
        BackendPingResult result = pingCache.get(server);
        if(result == null) return OptionalInt.empty();
        return OptionalInt.of(result.getProtocol());
    }

    @Subscribe
    public void onServerUnregistered(ServerUnregisteredEvent e){
        pingCache.remove(e.unregisteredServer());
    }

}
