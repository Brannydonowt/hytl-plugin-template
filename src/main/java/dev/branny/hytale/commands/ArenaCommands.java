package dev.branny.hytale.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.utilities.WorldUtilities;

import javax.annotation.Nonnull;

public class ArenaCommands extends AbstractCommandCollection {

    public ArenaCommands() {
        super("arena", "Arena related commands and utilities");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP

        this.addSubCommand(new ArenaTeleportCommand());
        this.addSubCommand(new ArenaHomeCommand());
        this.addSubCommand(new ArenaVersionCommand());
    }
}

class ArenaTeleportCommand extends CommandBase {

    public ArenaTeleportCommand() {
        super("teleport", "Teleport to the lobby world");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // This command must be run by a player
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        PlayerRef playerRef = player.getPlayerRef();

        ctx.sendMessage(Message.raw("Transferring you to the lobby..."));

        WorldUtilities.transferPlayerToLobby(playerRef)
            .thenAccept(transferredPlayer -> {
                // Send success message after transfer completes
                if (transferredPlayer != null) {
                    transferredPlayer.sendMessage(Message.raw("Welcome to the lobby!"));
                }
            })
            .exceptionally(throwable -> {
                // Handle transfer failure
                String errorMsg = throwable.getCause() != null 
                    ? throwable.getCause().getMessage() 
                    : throwable.getMessage();
                playerRef.sendMessage(Message.raw("Failed to transfer to lobby: " + errorMsg));
                return null;
            });
    }
}

class ArenaHomeCommand extends CommandBase {

    public ArenaHomeCommand() {
        super("home", "Teleport back to the default world");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        PlayerRef playerRef = player.getPlayerRef();

        ctx.sendMessage(Message.raw("Returning to default world..."));

        WorldUtilities.transferPlayerToDefault(playerRef)
            .thenAccept(transferredPlayer -> {
                if (transferredPlayer != null) {
                    transferredPlayer.sendMessage(Message.raw("Welcome back!"));
                }
            })
            .exceptionally(throwable -> {
                String errorMsg = throwable.getCause() != null 
                    ? throwable.getCause().getMessage() 
                    : throwable.getMessage();
                playerRef.sendMessage(Message.raw("Failed to return home: " + errorMsg));
                return null;
            });
    }
}

class ArenaVersionCommand extends CommandBase {

    public ArenaVersionCommand() {
        super("version", "Print the version of the arena mod");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Arena mod version 0.0.1"));
    }
}
