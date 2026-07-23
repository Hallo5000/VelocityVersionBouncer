package de.hallo5000.main;

import com.velocitypowered.api.command.SimpleCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VVBCommand implements SimpleCommand {

    private final VelocityVersionBouncer plugin;
    private final Runnable reload;
    public VVBCommand(VelocityVersionBouncer plugin, Runnable reload){
        this.plugin = plugin;
        this.reload = reload;
    }

    @Override
    public void execute(final Invocation invocation) {
        String[] args = invocation.arguments();

        if(args.length > 0){
            if(args[0].equalsIgnoreCase("reloadConfig")){
                if(invocation.source().equals(plugin.getServer().getConsoleCommandSource())){
                    reload.run();
                }
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        if(invocation.arguments().length < 2) suggestions.add("reloadConfig");
        return CompletableFuture.completedFuture(suggestions);
    }
}
