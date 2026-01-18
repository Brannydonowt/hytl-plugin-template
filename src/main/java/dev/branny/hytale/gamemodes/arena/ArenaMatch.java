package dev.branny.hytale.gamemodes.arena;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.pvptools.combat.EliminationEvent;
import dev.branny.hytale.pvptools.loadout.LoadoutRegistry;
import dev.branny.hytale.pvptools.loadout.LoadoutService;
import dev.branny.hytale.pvptools.match.Match;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.pvptools.match.MatchParticipant;
import dev.branny.hytale.pvptools.match.MatchResult;
import dev.branny.hytale.servercore.gamemode.GamemodeTransitionService;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Arena match implementation.
 * A 1v1 battle where the first death ends the match.
 */
public class ArenaMatch extends Match {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ArenaMatch(@Nonnull List<PlayerRef> players) {
        super("arena", ArenaConfig.WORLD_NAME, ArenaConfig.MIN_PLAYERS, ArenaConfig.MAX_PLAYERS);
        
        // Set the default loadout
        this.defaultLoadout = LoadoutRegistry.get(ArenaConfig.LOADOUT_ID);
        
        // Add participants
        for (PlayerRef player : players) {
            addParticipant(player);
        }
    }

    @Override
    protected void onStart() {
        LOGGER.atInfo().log("Arena match " + getMatchId() + " started!");
        
        // Transfer players to arena and apply loadouts
        int index = 0;
        for (MatchParticipant participant : getParticipants()) {
            final int spawnIndex = index;
            Transform spawn = ArenaConfig.getSpawnForIndex(spawnIndex);
            
            WorldTransferService.transfer(
                participant.getPlayerRef(),
                ArenaConfig.WORLD_NAME,
                spawn
            ).thenAccept(transferred -> {
                if (transferred != null && defaultLoadout != null) {
                    LoadoutService.applyLoadoutAsync(transferred, defaultLoadout);
                }
            });
            
            index++;
        }
        
        // Announce match start
        broadcastTitle("Arena Battle!", "Fight!");
    }

    @Override
    protected void onEnd(@Nonnull MatchResult result) {
        LOGGER.atInfo().log("Arena match " + getMatchId() + " ended: " + result);
        
        // Announce result
        if (result.getWinner() != null) {
            broadcastTitle("Victory!", result.getWinner().getUsername() + " wins!");
        } else {
            broadcastTitle("Match Over", "No winner");
        }
        
        // Return players to lobby after delay
        CompletableFuture.delayedExecutor(ArenaConfig.END_DELAY_SECONDS, TimeUnit.SECONDS)
            .execute(() -> {
                for (MatchParticipant participant : result.getParticipants()) {
                    GamemodeTransitionService.returnToLobby(participant.getPlayerRef());
                }
                
                // Unregister match
                MatchManager.unregisterMatch(getMatchId());
            });
    }

    @Override
    protected void onElimination(@Nonnull EliminationEvent event) {
        LOGGER.atInfo().log("Arena elimination: " + event.getEliminatedName());
        
        // In arena, one death = match over
        // Find the winner (the other player)
        MatchParticipant winner = null;
        for (MatchParticipant participant : getParticipants()) {
            if (!participant.getPlayerId().equals(event.getEliminatedId())) {
                winner = participant;
                break;
            }
        }
        
        // End the match
        end(winner);
    }

    @Override
    @Nonnull
    protected Transform getSpawnPosition(@Nonnull MatchParticipant participant, int index) {
        return ArenaConfig.getSpawnForIndex(index);
    }
}
