package de.hallo5000.commands;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.velocitypowered.api.command.SimpleCommand;
import de.hallo5000.main.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class setModpackCommand implements SimpleCommand {
    @Override
    public void execute(final Invocation invocation) {
        if(invocation.arguments().length==1){
            String selected = null;
            for(String s : Main.getServer.getAllServers().stream().map(x->x.getServerInfo().getName()).toList()){
                if(invocation.arguments()[0].equals(s)) selected = s;
            }
            if(selected != null){
                File modFolder = Main.getDataDirectory.resolve(selected).toFile();
                if(modFolder.exists() && modFolder.isDirectory()){ // modFolder exists and should contain the modpack
                    if(modFolder.listFiles() == null) {
                        invocation.source().sendMessage(Component.text("There are no mods in this mod folder!", TextColor.fromHexString("FF5555")));
                        return;
                    }
                    List<Toml> modList = new ArrayList<>();
                    for(File jar : modFolder.listFiles()){
                        if(jar.isFile() && jar.getName().endsWith(".jar")){
                            // | Loader            | File                          | Format    | Contains `modId`? |
                            // | ----------------- | ----------------------------- | --------- | ----------------- |
                            // | Forge             | `META-INF/mods.toml`          | TOML      |  `modId=...`      |
                            // | NeoForge          | `META-INF/neoforge.mods.toml` | TOML      | hopefully `modID=...`
                            // | Fabric            | `fabric.mod.json`             | JSON      |  `"id": "..."`    |
                            // | Quilt             | `quilt.mod.json`              | JSON      |  `"id": "..."`    |
                            // | Old (Forge <1.13) | `mcmod.info`       optional   | JSON      |  (deprecated)     |

                            Toml modConfigs = null;
                            try(InputStream forgeModern = Main.readFromJar(jar, "mods.toml");
                                InputStream fabric = Main.readFromJar(jar, "fabric.mod.json");
                                InputStream quilt = Main.readFromJar(jar, "quilt.mod.json");
                                InputStream forgeDeprecated = Main.readFromJar(jar, "mcmod.info")){
                                    if(forgeModern != null){
                                        modConfigs = new Toml().read(forgeModern);
                                        modList.addAll(modConfigs.getTables("mods"));
                                    }//Neoforge einfügen probably vor forgeModern wegen Endung gleich
                                    else if(fabric != null){
                                        modConfigs = new Toml().read(fabric);
                                    }
                                    else if(quilt != null){
                                        modConfigs = new Toml().read(quilt);
                                    }
                                    else if(forgeDeprecated != null){
                                        modConfigs = new Toml().read(forgeDeprecated);
                                    }
                            }catch(IOException | SecurityException e){
                                invocation.source().sendMessage(Component.text("The mod: " + jar.getName() + " couldn't be opened (the jar file may be corrupted or secured)", TextColor.fromHexString("FF5555")));
                            }
                            if(modConfigs == null){
                                invocation.source().sendMessage(Component.text("Couldn't find a mod config in: " + jar.getName(), TextColor.fromHexString("FF5555")));
                            }
                        }
                    }
                    //Only Forge
                    TomlWriter tomlWriter = new TomlWriter.Builder().build();
                    Map<String, Object> map = new HashMap<>(Main.modlistToml.toMap());
                    String[] modIDs = modList.stream().map(x -> x.getString("modId")).toArray(String[]::new);
                    map.put("modIDs-"+selected, modIDs);
                    try {
                        tomlWriter.write(map, Main.modlistFile);
                    } catch (IOException e) {
                        invocation.source().sendMessage(Component.text("Failed to write the Modlist to modlists.toml!", TextColor.fromHexString("FF5555")));
                        throw new RuntimeException(e);
                    }
                    //DELETE MODFOLDER
                    invocation.source().sendMessage(Component.text("Successfully read the modIDs and wrote them to modlists.toml", TextColor.fromHexString("55FF55")));
                }else invocation.source().sendMessage(Component.text("The folder containing the modpack should be in the plugins folder, named after the server to set it on!", TextColor.fromHexString("FF5555")));
            }else invocation.source().sendMessage(Component.text("No server with this name found! (case-sensitive)", TextColor.fromHexString("FF5555")));
        }else invocation.source().sendMessage(Component.text("Syntax: /setModpack <server-name>", TextColor.fromHexString("FF5555")));
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().equals(Main.getServer.getConsoleCommandSource());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFuture.completedFuture(Main.getServer.getAllServers().stream().map(x -> x.getServerInfo().getName()).toList());
    }
}
