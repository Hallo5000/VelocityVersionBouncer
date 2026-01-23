package de.hallo5000.main;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.hallo5000.listener.KickedFromServerListener;
import de.hallo5000.listener.PlayerChooseInitialServerListener;
import de.hallo5000.pingHandler.BackendPingService;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


/*
TODO:
- Modrinth
- explicit mapping ('v'/version prefix not yet implemented and need to find out the syntax for version prefix)
- override versions on HangarMC
- modlists
- ViaVersion detect
- language files
- config-version in config
*/



@Plugin(id = "velocityversionbouncer", name = "VelocityVersionBouncer", version = "1.2.1-SNAPSHOT",
        url = "https://github.com/Hallo5000/VelocityVersionBouncer",
        description = "This plugin redirects players to server depending on there game version",
        authors = {"Hallo5000"})
public class VelocityVersionBouncer {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public static ProxyServer getServer;
    public static Logger getLogger;
    public static Toml toml;
    public static Path getDataDirectory;
    public static BackendPingService ps;

    @Inject
    public VelocityVersionBouncer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        getServer = server;
        getLogger = logger;
        getDataDirectory = dataDirectory;


        logger.info("Successfully loaded!");
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
        toml = loadConfig();

        ps = new BackendPingService(logger, this, server);
        ps.start();

        server.getEventManager().register(this, new PlayerChooseInitialServerListener());
        server.getEventManager().register(this, new KickedFromServerListener());
    }

    private Toml loadConfig() {
        File dataFolder = dataDirectory.toFile();
        try {
            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File file = new File(dataFolder, "config.toml");
            if(!file.exists()) {
                Files.copy(getClass().getClassLoader().getResourceAsStream("config.toml"), file.toPath());
            }
            return new Toml(new Toml().read(getClass().getClassLoader().getResourceAsStream("config.toml"))).read(file);
        } catch (IOException ex) {
            logger.error("Could not load config.toml file - Please check for errors", ex);
            return null;
        }
    }
}
