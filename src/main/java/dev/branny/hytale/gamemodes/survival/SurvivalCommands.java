package dev.branny.hytale.gamemodes.survival;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.servercore.gamemode.GamemodeTransitionService;

import javax.annotation.Nonnull;

/**
 * Commands for the Survival gamemode.
 */
public class SurvivalCommands extends AbstractCommandCollection {

    public SurvivalCommands() {
        super("survival", "Survival gamemode commands");
        this.setPermissionGroup(GameMode.Adventure);

        this.addSubCommand(new JoinCommand());
        this.addSubCommand(new LeaveCommand());
    }
}

/**
 * Join the survival world.
 * Usage: /survival join
 */
class JoinCommand extends CommandBase {

    JoinCommand() {
        super("join", "Join the survival world");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        PlayerRef playerRef = player.getPlayerRef();

        ctx.sendMessage(Message.raw("Joining survival world..."));

        GamemodeTransitionService.joinGamemode(playerRef, SurvivalGamemode.ID)
            .whenComplete((result, error) -> {
                if (error != null) {
                    playerRef.sendMessage(Message.raw("Failed to join survival: " + error.getMessage()));
                } else if (!result.success()) {
                    playerRef.sendMessage(Message.raw(result.message()));
                }
            });
    }
}

/**
 * Leave survival and return to the lobby.
 * Usage: /survival leave
 */
class LeaveCommand extends CommandBase {

    LeaveCommand() {
        super("leave", "Leave survival and return to the lobby");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        PlayerRef playerRef = player.getPlayerRef();

        ctx.sendMessage(Message.raw("Returning to lobby..."));

        GamemodeTransitionService.returnToLobby(playerRef)
            .whenComplete((result, error) -> {
                if (error != null) {
                    playerRef.sendMessage(Message.raw("Failed to return to lobby: " + error.getMessage()));
                } else if (!result.success()) {
                    playerRef.sendMessage(Message.raw(result.message()));
                }
            });
    }
}
