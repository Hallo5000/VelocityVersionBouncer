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

    /**
     * Starts a scheduled task on repeat with <code>pingAll()</code> and registers a listener for the <code>ServerRegisteredEvent</code>.
     * Should only be used once per Plugin initialization.
     */
    public void start(){
        server.getScheduler()
                .buildTask(plugin, this::pingAll)
                .repeat(60, TimeUnit.SECONDS)
                .schedule();
        server.getEventManager().register(plugin, this);
    }

    /**
     * Simply calls <code>ping()</code> on every backend server contained in <code>getAllServers()</code>
     */
    public void pingAll(){
        logger.info("Pinging every Backend Server...");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for(RegisteredServer s : server.getAllServers()){
            futures.add(ping(s));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, e) -> logger.info("Pings complete!"));
    }

    /**
     * Pings the given server and puts the response in the internal ping cache
     * @param server the server to be pinged
     * @return a CompletableFuture with the ping response
     * @throws NullPointerException if the given <code>RegisteredServer</code> is null
     */
    public CompletableFuture<?> ping(RegisteredServer server){
        return server.ping().whenComplete((result, error) -> {
            if (error != null) {
                if(error instanceof java.net.ConnectException
                        || error instanceof java.net.NoRouteToHostException
                        || error instanceof java.net.SocketTimeoutException) logger.info("Ping FAILED for " + server.getServerInfo().getName() + " (server is offline)");
                if(error instanceof io.netty.handler.codec.CorruptedFrameException //may be irrelevant because the CorruptedFrameException is handled by the Decoder Exception
                        || error instanceof io.netty.handler.codec.DecoderException) logger.info("Ping FAILED for " + server.getServerInfo().getName() + " (ping response is corrupted)");
            }else{
                pingCache.put(server, new BackendPingResult(result));
                logger.info("Ping SUCCESSFUL for " + server.getServerInfo().getName() + " - protocol version: " + pingCache.get(server).getProtocol());
            }
        });
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent e){
        logger.info("New server registered. Pinging...");
        ping(e.registeredServer());
    }

    /**
     * Looks up the ping in the ping cache and returns the Protocol Version Number if present
     * @param server <code>RegisteredServer</code> to get the ping from
     * @return an OptionalInt containing the ping if present in the ping cache or else <code>OptionalInt.empty()</code>
     */
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
