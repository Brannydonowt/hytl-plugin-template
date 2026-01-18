package dev.branny.hytale.gamemodes.arena;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.branny.hytale.pvptools.match.Match;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.pvptools.match.MatchParticipant;
import dev.branny.hytale.servercore.lobby.LobbyManager;
import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;

/**
 * Commands for the Arena gamemode.
 */
public class ArenaCommands extends AbstractCommandCollection {

    public ArenaCommands() {
        super("arena", "Arena gamemode commands");
        this.setPermissionGroup(GameMode.Adventure);

        this.addSubCommand(new QueueCommand());
        this.addSubCommand(new LeaveCommand());
        this.addSubCommand(new StatsCommand());
        this.addSubCommand(new ForfeitCommand());
    }
}

/**
 * Join the arena queue.
 */
class QueueCommand extends CommandBase {

    QueueCommand() {
        super("queue", "Join the arena queue");
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

        // Check if already in match
        if (MatchManager.isInMatch(player.getUuid())) {
            ctx.sendMessage(Message.raw("You are already in a match!"));
            return;
        }

        // Check if already in queue
        if (LobbyManager.isInQueue(player.getUuid())) {
            ctx.sendMessage(Message.raw("You are already in a queue! Use /arena leave to leave."));
            return;
        }

        // Join arena queue
        LobbyManager.joinQueue(playerRef, ArenaGamemode.ID);
    }
}

/**
 * Leave the arena queue.
 */
class LeaveCommand extends CommandBase {

    LeaveCommand() {
        super("leave", "Leave the arena queue");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();

        if (!LobbyManager.isInQueue(player.getUuid())) {
            ctx.sendMessage(Message.raw("You are not in a queue."));
            return;
        }

        LobbyManager.leaveQueue(player.getUuid());
    }
}

/**
 * View arena stats.
 */
class StatsCommand extends CommandBase {

    StatsCommand() {
        super("stats", "View your arena statistics");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        PlayerSession session = PlayerSession.get(player.getUuid());

        ctx.sendMessage(Message.raw("=== Arena Stats ==="));
        
        if (session != null) {
            ctx.sendMessage(Message.raw("Current Status: " + 
                (session.isInMatch() ? "In Match" : 
                 session.isQueued() ? "In Queue" : "In Lobby")));
        }
        
        // TODO: Add persistent stats from GlobalPlayerStats component
        ctx.sendMessage(Message.raw("(Full stats tracking coming soon)"));
    }
}

/**
 * Forfeit the current arena match.
 */
class ForfeitCommand extends CommandBase {

    ForfeitCommand() {
        super("forfeit", "Forfeit your current arena match");
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

        Match match = MatchManager.getMatchForPlayer(player.getUuid());
        if (match == null || !match.isActive()) {
            ctx.sendMessage(Message.raw("You are not in an active arena match."));
            return;
        }

        // Find the opponent as winner
        MatchParticipant winner = null;
        for (MatchParticipant participant : match.getParticipants()) {
            if (!participant.getPlayerId().equals(player.getUuid())) {
                winner = participant;
                break;
            }
        }

        ctx.sendMessage(Message.raw("You have forfeited the match."));
        match.end(winner);
    }
}
