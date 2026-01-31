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
- override versions on HangarMC
- modlists
- ViaVersion detect
- language files (+color code support)
- config-version in config
- ServerListPing override with bouncer (ProxyPingEvent)
- my own logger so that the plugins name will be show in the console instead of the id
*/


@Plugin(id = "velocityversionbouncer", name = "VelocityVersionBouncer", version = "1.3.0-release",
        url = "https://github.com/Hallo5000/VelocityVersionBouncer",
        description = "This plugin redirects players to server depending on there game version",
        authors = {"Hallo5000"})
public class VelocityVersionBouncer {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Utils utils;
    private final BackendPingService backendPingService;
    private Toml toml;

    @Inject
    public VelocityVersionBouncer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory; //.getParent().resolve(this.getClass().getAnnotation(Plugin.class).name());

        this.utils = new Utils(this);
        this.backendPingService = new BackendPingService(logger, this, server);

        logger.info("Successfully loaded!");
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
        toml = loadConfig();

        backendPingService.start();

        server.getEventManager().register(this, new PlayerChooseInitialServerListener(this));
        server.getEventManager().register(this, new KickedFromServerListener(this));
    }

    private Toml loadConfig() {
        File dataFolder = dataDirectory.toFile();
        if(!dataFolder.exists() && !dataFolder.mkdirs()) logger.error("Couldn't create plugins folder, probably caused by a missing permission.");
        File file = new File(dataFolder, "config.toml");
        try(InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.toml")){
            if(!file.exists() && defaultConfig != null) Files.copy(defaultConfig, file.toPath());
            return new Toml(new Toml().read(defaultConfig)).read(file);
        }catch(IOException ex){
            logger.error("Could not load config.toml file - Please check for errors", ex);
            return null;
        }
    }

    public ProxyServer getServer(){
        return server;
    }

    public Logger getLogger(){
        return logger;
    }

    public Utils getUtils(){
        return utils;
    }

    public BackendPingService getBackendPingService(){
        return backendPingService;
    }

    public Toml getToml(){
        return toml;
    }

}
