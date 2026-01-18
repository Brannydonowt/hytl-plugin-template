package dev.branny.hytale.gamemodes.sniperwars;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.pvptools.match.Match;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.pvptools.match.MatchParticipant;
import dev.branny.hytale.servercore.lobby.LobbyManager;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

/**
 * Commands for the Sniper Wars gamemode.
 */
public class SniperWarsCommands extends AbstractCommandCollection {

    public SniperWarsCommands() {
        super("sniperwars", "Sniper Wars gamemode commands");
        this.setPermissionGroup(GameMode.Adventure);

        this.addSubCommand(new QueueCommand());
        this.addSubCommand(new LeaveCommand());
        this.addSubCommand(new ScoresCommand());
    }
}

/**
 * Join the sniper wars queue.
 */
class QueueCommand extends CommandBase {

    QueueCommand() {
        super("queue", "Join the Sniper Wars queue");
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
            ctx.sendMessage(Message.raw("You are already in a queue! Use /sniperwars leave to leave."));
            return;
        }

        // Join sniper wars queue
        LobbyManager.joinQueue(playerRef, SniperWarsGamemode.ID);
    }
}

/**
 * Leave the sniper wars queue.
 */
class LeaveCommand extends CommandBase {

    LeaveCommand() {
        super("leave", "Leave the Sniper Wars queue");
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
 * View current match scores.
 */
class ScoresCommand extends CommandBase {

    ScoresCommand() {
        super("scores", "View current match scores");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be run by a player."));
            return;
        }

        Player player = (Player) ctx.sender();
        Match match = MatchManager.getMatchForPlayer(player.getUuid());

        if (match == null || !match.isActive()) {
            ctx.sendMessage(Message.raw("You are not in an active match."));
            return;
        }

        ctx.sendMessage(Message.raw("=== Sniper Wars Scores ==="));
        ctx.sendMessage(Message.raw("First to " + SniperWarsConfig.KILLS_TO_WIN + " wins!"));
        ctx.sendMessage(Message.raw(""));
        
        List<MatchParticipant> sorted = match.getParticipants().stream()
            .sorted(Comparator.comparingInt(MatchParticipant::getKills).reversed())
            .toList();
        
        int rank = 1;
        for (MatchParticipant p : sorted) {
            String marker = p.getPlayerId().equals(player.getUuid()) ? " (you)" : "";
            ctx.sendMessage(Message.raw("#" + rank + " " + p.getUsername() + marker + 
                ": " + p.getKills() + " kills, " + p.getDeaths() + " deaths"));
            rank++;
        }
    }
}
