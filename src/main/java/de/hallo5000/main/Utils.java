package de.hallo5000.main;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.*;
import java.util.stream.Collectors;

import static de.hallo5000.main.VelocityVersionBouncer.toml;

public class Utils {

    public static List<RegisteredServer> getConfigServerList(){
        //take all backend servers or only the ones provided by the whitelist (if one exists) and remove the ones on the blacklist
        List<RegisteredServer> serverList = (new ArrayList<>(toml.getList("whitelist")))
                .stream().map(name -> VelocityVersionBouncer.getServer.getServer((String) name).orElseGet(() -> {
                    VelocityVersionBouncer.getLogger.info("'" + name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).collect(Collectors.toList());
        if(serverList.isEmpty()) serverList = new ArrayList<>(VelocityVersionBouncer.getServer.getAllServers());
        List<RegisteredServer> blacklist = Optional.ofNullable(toml.getList("blacklist"))
                .orElse(new ArrayList<>(Collections.emptyList()))
                .stream().map(name -> VelocityVersionBouncer.getServer.getServer((String) name).orElseGet(() -> {
                    VelocityVersionBouncer.getLogger.info("'"+ name + "' could not be found!");
                    return null;
                })).filter(Objects::nonNull).toList();
        serverList.removeAll(blacklist);
        return serverList;
    }

}
