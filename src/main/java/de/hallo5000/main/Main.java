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
import de.hallo5000.listener.PlayerModInfoListener;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


@Plugin(id = "velocityversionbouncer", name = "VelocityVersionBouncer", version = "1.1.0-release",
        url = "https://github.com/hallo5000",
        description = "This plugin redirects players to server depending on there game version",
        authors = {"Hallo5000"})
public class Main {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public static ProxyServer getServer;
    public static Logger getLogger;
    public static Toml toml;

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        getServer = server;
        getLogger = logger;

        logger.info("Successfully loaded!");
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
        toml = loadConfig();
        server.getEventManager().register(this, new PlayerModInfoListener());
        server.getEventManager().register(this, new PlayerChooseInitialServerListener());
        server.getEventManager().register(this, new KickedFromServerListener());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
