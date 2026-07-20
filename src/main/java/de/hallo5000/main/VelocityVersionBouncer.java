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
import de.hallo5000.listener.ProxyPingListener;
import de.hallo5000.pingHandler.BackendPingService;
import de.themoep.utils.lang.LangLogger;
import de.themoep.utils.lang.velocity.LanguageManager;
import de.themoep.utils.lang.velocity.Languaged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;


/*
TODO:
- fix changelog in workflows
- ViaVersion detect
- set different log levels for each logging call (log-level in config)
- check compatability with forced-hosts
- command to reload config without restarting the proxy
*/


@Plugin(id = "velocityversionbouncer", name = "VelocityVersionBouncer", version = "1.6.0-release",
        url = "https://github.com/Hallo5000/VelocityVersionBouncer",
        description = "This plugin redirects players to servers depending on their game version",
        authors = {"Hallo5000"})
public class VelocityVersionBouncer implements Languaged {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final LangLogger langLogger;
    private LanguageManager lang;
    private final Utils utils;
    private final JsonReader jsonReader;
    private final BackendPingService backendPingService;
    private Toml toml;

    @Inject
    public VelocityVersionBouncer(ProxyServer server, @SuppressWarnings("unused") Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = LoggerFactory.getLogger(this.getClass().getAnnotation(Plugin.class).name());
        this.dataDirectory = dataDirectory; //.getParent().resolve(this.getClass().getAnnotation(Plugin.class).name());

        this.langLogger = new LangLogger() {
            @Override
            public void log(Level level, String msg) {
                if (level == Level.SEVERE) logger.error(msg);
                else if (level == Level.WARNING) logger.warn(msg);
                else if (level == Level.INFO) logger.info(msg);
                else logger.debug(msg);
            }

            @Override
            public void log(Level level, String msg, Throwable thrown) {
                if (level == Level.SEVERE) logger.error(msg, thrown);
                else if (level == Level.WARNING) logger.warn(msg, thrown);
                else if (level == Level.INFO) logger.info(msg, thrown);
                else logger.debug(msg, thrown);
            }
        };

        this.utils = new Utils(this);
        this.jsonReader = new JsonReader(this);
        this.backendPingService = new BackendPingService(this, server);
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
        toml = loadConfig();
        lang = new LanguageManager(this, "languages", this.getToml().getString("language", "en_US"));

        server.getEventManager().register(this, new PlayerChooseInitialServerListener(this));
        server.getEventManager().register(this, new KickedFromServerListener(this));
        server.getEventManager().register(this, new ProxyPingListener(this));

        logger.info(this.getMessage("successfully-loaded"));

        backendPingService.start();
    }

    private Toml loadConfig() {
        File dataFolder = dataDirectory.toFile();
        if(!dataFolder.exists() && !dataFolder.mkdirs()) logger.error(this.getMessage("cant-create-folder"));
        File file = new File(dataFolder, "config.toml");
        try(InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.toml")){
            if(!file.exists() && defaultConfig != null) Files.copy(defaultConfig, file.toPath());
            return new Toml(new Toml().read(defaultConfig)).read(file);
        }catch(IOException ex){
            logger.error(this.getMessage("config-load-error", ex.getMessage()));
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

    public JsonReader getJsonReader(){
        return jsonReader;
    }

    public BackendPingService getBackendPingService(){
        return backendPingService;
    }

    public Toml getToml(){
        return toml;
    }

    @Override
    public String getName() {
        return getClass().getAnnotation(Plugin.class).name();
    }

    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public LangLogger getLangLogger() {
        return langLogger;
    }

    public String getMessage(String key) {
        return lang.getDefaultConfig().get(key);
    }

    public String getMessage(String key, String... replacements){
        String msg = getMessage(key);
        for(int i = 0; i < replacements.length; i++){
            msg = msg.replace("{"+i+"}", replacements[i]);
        }
        return msg;
    }
}
