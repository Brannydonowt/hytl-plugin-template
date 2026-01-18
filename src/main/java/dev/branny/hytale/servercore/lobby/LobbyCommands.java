package dev.branny.hytale.servercore.lobby;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.gamemodes.GamemodeRegistry;
import dev.branny.hytale.pvptools.match.MatchManager;

import javax.annotation.Nonnull;

/**
 * Global lobby commands.
 * Provides commands for returning to the lobby and listing available gamemodes.
 */
public class LobbyCommands extends AbstractCommandCollection {

    public LobbyCommands() {
        super("lobby", "Lobby commands");
        setPermissionGroup(GameMode.Adventure);
        
        addSubCommand(new ReturnToLobbyCommand());
        addSubCommand(new GamesCommand());
    }
}

/**
 * Command to return to the lobby.
 * Usage: /lobby return
 */
class ReturnToLobbyCommand extends CommandBase {

    ReturnToLobbyCommand() {
        super("return", "Return to the lobby");
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

        // Check if in a match
        if (MatchManager.isInMatch(player.getUuid())) {
            ctx.sendMessage(Message.raw(
                "You cannot return to lobby while in a match. Use the forfeit command to leave."));
            return;
        }

        ctx.sendMessage(Message.raw("Returning to lobby..."));
        LobbyManager.transferToLobby(playerRef);
    }
}

/**
 * Command to list available gamemodes.
 * Usage: /lobby games
 */
class GamesCommand extends CommandBase {

    GamesCommand() {
        super("games", "List available gamemodes");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("=== Available Gamemodes ==="));
        
        for (Gamemode gamemode : GamemodeRegistry.getQueueable()) {
            ctx.sendMessage(Message.raw(
                "- " + gamemode.getDisplayName() + " (/" + gamemode.getId() + " queue)"));
            ctx.sendMessage(Message.raw(
                "  " + gamemode.getDescription()));
            ctx.sendMessage(Message.raw(
                "  Players: " + gamemode.getMinPlayers() + "-" + gamemode.getMaxPlayers()));
        }
    }
}
