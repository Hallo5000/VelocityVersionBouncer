package de.hallo5000.main;

import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VVBCommand implements SimpleCommand {

    private final VelocityVersionBouncer plugin;
    public VVBCommand(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        String[] args = invocation.arguments();

        if(args.length > 0){
            if(args[0].equalsIgnoreCase("reloadConfig")){
                if(invocation.source().equals(plugin.getServer().getConsoleCommandSource())){
                    //reload
                }
            }
        }
    }

    // This method allows you to control who can execute the command.
    // If the executor does not have the required permission,
    // the execution of the command and the control of its autocompletion
    // will be sent directly to the server on which the sender is located
    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("command.test");
    }

    // With this method you can control the suggestions to send
    // to the CommandSource according to the arguments
    // it has already written or other requirements you need
    @Override
    public List<String> suggest(final Invocation invocation) {
        return List.of();
    }

    // Here you can offer argument suggestions in the same way as the previous method,
    // but asynchronously. It is recommended to use this method instead of the previous one
    // especially in cases where you make a more extensive logic to provide the suggestions
    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}
