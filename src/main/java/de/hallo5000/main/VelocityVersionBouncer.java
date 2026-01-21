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
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/*
TODO:
- Modrinth
- explicit mapping ('c'/'v' prefixes not yet implemented)
- hangar update resource page
- modlists/forge/fabric (idea: at least check the client for the right modloader so some servers are forge-only for example)
- ViaVersion detect
- language files
- exclude reconnect on /kick (impossible when only acting on the proxy side)
- config-version in config
- lookup which netty version to use (especially for implementing the custom ping)
*/



@Plugin(id = "velocityversionbouncer", name = "VelocityVersionBouncer", version = "1.2.0-release",
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
    public static Toml modlistToml;
    public static File modlistFile;
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
        modlistToml = loadModlists();
        modlistFile = loadModlistsFile();

        ps = new BackendPingService(logger, this, server);
        ps.start();

        /*
        CommandManager commandManager = server.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("setModpack")
                .plugin(this)
                .build();

        commandManager.register(commandMeta, new setModpackCommand());
        */

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

    private File loadModlistsFile() {
        File dataFolder = dataDirectory.toFile();
        try {
            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File file = new File(dataFolder, "modlists.toml");
            if(!file.exists()) {
                Files.copy(getClass().getClassLoader().getResourceAsStream("modlists.toml"), file.toPath());
            }
            return file;
        } catch (IOException ex) {
            logger.error("Could not load modlists.toml file - Please check for errors", ex);
            return null;
        }
    }

    private Toml loadModlists(){
        return new Toml(new Toml().read(getClass().getClassLoader().getResourceAsStream("modlists.toml"))).read(loadModlistsFile());
    }

    public static InputStream readFromJar(File jarFile, String fileToRead) throws IOException {
        JarFile jar =  new JarFile(jarFile);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (e.getName().endsWith(fileToRead)) {
                try {
                    //following variant could be implemented via an overload
                    /*File tempFile = File.createTempFile(jarFile.getName()+"-specificExtract-", ".tmp");
                    tempFile.deleteOnExit();
                    InputStream input = jar.getInputStream(e);
                    try (OutputStream output = new FileOutputStream(tempFile)) {
                        input.transferTo(output);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    return tempFile;*/
                    return jar.getInputStream(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return null;
    }

}
