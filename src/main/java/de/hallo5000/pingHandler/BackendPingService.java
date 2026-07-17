package de.hallo5000.pingHandler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.event.proxy.server.ServerUnregisteredEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.hallo5000.main.VelocityVersionBouncer;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BackendPingService {

    private final VelocityVersionBouncer plugin;
    private final ProxyServer server;
    private final Map<RegisteredServer, Optional<ServerPing>> pingCache;
    private final PingHandler pingHandler;

    /**
     * Initializes the internal Objects (the pingCache Map is initialized as <code>ConcurrentHashMap</code>
     * @param plugin an instance of the plugins main class
     * @param server the main instance of <code>ProxyServer</code>
     */
    public BackendPingService(VelocityVersionBouncer plugin, ProxyServer server){
        this.plugin = plugin;
        this.server = server;
        this.pingCache = new ConcurrentHashMap<>();
        this.pingHandler = new PingHandler(plugin);
    }

    /**
     * Starts a scheduled task on repeat with <code>pingAll()</code> and registers a listener for the <code>ServerRegisteredEvent</code>.
     * Should only be used once per Plugin initialization.
     */
    public void start(){
        server.getEventManager().register(plugin, this);
        server.getScheduler()
                .buildTask(plugin, this::pingAll)
                .repeat(plugin.getToml().getLong("ping-intervall"), TimeUnit.SECONDS)
                .schedule();
    }

    /**
     * Simply calls <code>ping()</code> on every backend server contained in <code>getAllServers()</code>
     */
    public void pingAll(){
        plugin.getLogger().info(plugin.getMessage("ping-all"));
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for(RegisteredServer s : server.getAllServers()){
            futures.add(ping(s));
        }
        server.getScheduler().buildTask(plugin, () -> { //timeout
            for(CompletableFuture<String> f : futures){
                f.cancel(true);
            }
        }).delay(plugin.getToml().getLong("ping-intervall"), TimeUnit.SECONDS).schedule();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete(
                (v, e) -> plugin.getLogger().info(plugin.getMessage("ping-complete")));
    }

    /**
     * Pings the given server and puts the response in the internal ping cache
     * @param server the server to be pinged
     * @return a CompletableFuture with the ping response
     * @throws NullPointerException if the given <code>RegisteredServer</code> is null
     */
    public CompletableFuture<String> ping(RegisteredServer server){
        return pingHandler.ping(server).handle((json, error) -> {
            if(error != null && !(error instanceof CancellationException)) plugin.getLogger().error(
                    plugin.getMessage("ping-error", error.getMessage()));
            if(json != null){
                pingCache.put(server, Optional.of(plugin.getUtils().getPingFromHandshake(json)));
                if(getProtocol(server).isPresent()){
                    plugin.getLogger().info(plugin.getMessage("ping-successful", server.getServerInfo().getName(), String.valueOf(getProtocol(server).getAsInt())));
                    return json;
                }
            }
            pingCache.remove(server);
            plugin.getLogger().info(plugin.getMessage("ping-failed", server.getServerInfo().getName()));
            return json;
        });
    }

    /**
     * Looks up the ping in the ping cache and returns the Protocol Version Number if present
     * @param server <code>RegisteredServer</code> to get the ping from
     * @return an OptionalInt containing the ping if present in the ping cache or else <code>OptionalInt.empty()</code>
     */
    public OptionalInt getProtocol(RegisteredServer server){
        return getPing(server).map(p -> OptionalInt.of(p.getVersion().getProtocol())).orElse(OptionalInt.empty());
    }

    /**
     * Getter for a <code>ServerPing</code> saved in the internal ping cache
     * @param server the server whose ping to lookup
     * @return an <code>Optional<ServerPing></code> containing the servers ping response or empty if there was none the last time the server was pinged
     */
    public Optional<ServerPing> getPing(RegisteredServer server){
        return pingCache.getOrDefault(server, Optional.empty());
    }

    public Map<RegisteredServer, Optional<ServerPing>> getPingCache() {
        return pingCache;
    }

    public void removePing(RegisteredServer server){
        pingCache.remove(server);
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent e){
        plugin.getLogger().info(plugin.getMessage("ping-new"));
        ping(e.registeredServer());
    }

    @Subscribe
    public void onServerUnregistered(ServerUnregisteredEvent e){
        pingCache.remove(e.unregisteredServer());
    }

}
