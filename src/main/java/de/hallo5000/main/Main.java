package de.hallo5000.main;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.hallo5000.listener.onPlayerModInfo;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "versionbouncer", name = "VersionBouncer", version = "0.1.0-SNAPSHOT",
        url = "https://github.com/hallo5000",
        description = "This plugin redirects players to server depending on there game version",
        authors = {"Hallo5000"})
public class Main {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public static ProxyServer getServer;

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        getServer = server;

        logger.info("Successfully loaded!");
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        if(!dataDirectory.toFile().exists()){

        }
        server.getEventManager().register(this, new onPlayerModInfo());
    }
}
