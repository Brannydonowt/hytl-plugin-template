package dev.branny.hytale.gamemodes.sniperwars;

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
import dev.branny.hytale.servercore.lobby.LobbyManager;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sniper Wars match implementation.
 * Free-for-all sniper battle - first to reach the kill target wins!
 */
public class SniperWarsMatch extends Match {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private CompletableFuture<?> timeLimitFuture;

    public SniperWarsMatch(@Nonnull List<PlayerRef> players) {
        super("sniperwars", SniperWarsConfig.WORLD_NAME, 
              SniperWarsConfig.MIN_PLAYERS, SniperWarsConfig.MAX_PLAYERS);
        
        // Set the default loadout
        this.defaultLoadout = LoadoutRegistry.get(SniperWarsConfig.LOADOUT_ID);
        
        // Add participants
        for (PlayerRef player : players) {
            addParticipant(player);
        }
    }

    @Override
    protected void onStart() {
        LOGGER.atInfo().log("Sniper Wars match " + getMatchId() + " started with " + 
            getParticipantCount() + " players!");
        
        // Transfer players to arena and apply loadouts
        int index = 0;
        for (MatchParticipant participant : getParticipants()) {
            final int spawnIndex = index;
            Transform spawn = SniperWarsConfig.getSpawnForIndex(spawnIndex);
            
            WorldTransferService.transfer(
                participant.getPlayerRef(),
                SniperWarsConfig.WORLD_NAME,
                spawn
            ).thenAccept(transferred -> {
                if (transferred != null && defaultLoadout != null) {
                    LoadoutService.applyLoadoutAsync(transferred, defaultLoadout);
                }
            });
            
            index++;
        }
        
        // Announce match start
        broadcastTitle("Sniper Wars!", "First to " + SniperWarsConfig.KILLS_TO_WIN + " kills wins!");
        broadcast("Match started! Get " + SniperWarsConfig.KILLS_TO_WIN + " kills to win!");
        
        // Start time limit
        timeLimitFuture = CompletableFuture.runAsync(
            this::onTimeLimit,
            CompletableFuture.delayedExecutor(SniperWarsConfig.MATCH_TIME_LIMIT_SECONDS, TimeUnit.SECONDS)
        );
    }

    @Override
    protected void onEnd(@Nonnull MatchResult result) {
        LOGGER.atInfo().log("Sniper Wars match " + getMatchId() + " ended: " + result);
        
        // Cancel time limit future
        if (timeLimitFuture != null) {
            timeLimitFuture.cancel(true);
        }
        
        // Announce result
        MatchParticipant winner = result.getWinner();
        if (winner != null) {
            broadcastTitle("Victory!", winner.getUsername() + " wins with " + 
                winner.getKills() + " kills!");
        } else {
            broadcastTitle("Time's Up!", "Match ended");
        }
        
        // Show final scores
        broadcast("=== Final Scores ===");
        List<MatchParticipant> sorted = result.getParticipants().stream()
            .sorted(Comparator.comparingInt(MatchParticipant::getKills).reversed())
            .toList();
        int rank = 1;
        for (MatchParticipant p : sorted) {
            broadcast("#" + rank + " " + p.getUsername() + ": " + p.getKills() + " kills, " + 
                p.getDeaths() + " deaths");
            rank++;
        }
        
        // Return players to lobby after delay
        CompletableFuture.delayedExecutor(SniperWarsConfig.END_DELAY_SECONDS, TimeUnit.SECONDS)
            .execute(() -> {
                for (MatchParticipant participant : result.getParticipants()) {
                    LoadoutService.clearLoadoutAsync(participant.getPlayerRef());
                    LobbyManager.transferToLobby(participant.getPlayerRef())
                        .thenAccept(p -> {
                            if (p != null) {
                                LobbyManager.onPlayerReturnToLobby(p);
                            }
                        });
                }
                MatchManager.unregisterMatch(getMatchId());
            });
    }

    @Override
    protected void onElimination(@Nonnull EliminationEvent event) {
        LOGGER.atInfo().log("Sniper Wars elimination: " + event.getEliminatedName() + 
            (event.isPlayerElimination() ? " by " + event.getEliminatorName() : ""));
        
        // Announce the kill
        if (event.isPlayerElimination()) {
            broadcast(event.getEliminatorName() + " sniped " + event.getEliminatedName() + "!");
            
            // Check if killer reached kill target
            UUID eliminatorId = event.getEliminatorId();
            if (eliminatorId != null) {
                MatchParticipant killer = getParticipant(eliminatorId);
                if (killer != null && killer.getKills() >= SniperWarsConfig.KILLS_TO_WIN) {
                    // Winner!
                    end(killer);
                    return;
                }
            }
        } else {
            broadcast(event.getEliminatedName() + " was eliminated!");
        }
        
        // Respawn the eliminated player
        respawnPlayer(event.getEliminated());
        
        // Broadcast kill update
        broadcastScores();
    }

    @Override
    @Nonnull
    protected Transform getSpawnPosition(@Nonnull MatchParticipant participant, int index) {
        return SniperWarsConfig.getSpawnForIndex(index);
    }

    /**
     * Respawns a player at a random spawn point.
     */
    private void respawnPlayer(@Nonnull PlayerRef playerRef) {
        // Pick a random spawn point
        int spawnIndex = (int) (Math.random() * SniperWarsConfig.getSpawnPointCount());
        Transform spawn = SniperWarsConfig.getSpawnForIndex(spawnIndex);
        
        // Teleport and reapply loadout
        WorldTransferService.teleport(playerRef, spawn)
            .thenAccept(v -> {
                if (defaultLoadout != null) {
                    LoadoutService.applyLoadoutAsync(playerRef, defaultLoadout);
                }
            });
        
        // Un-eliminate the participant
        MatchParticipant participant = getParticipant(playerRef.getUuid());
        if (participant != null) {
            // Reset elimination status for respawn games
            // Note: The participant tracks deaths but isn't "eliminated" in FFA
        }
    }

    /**
     * Called when the match time limit is reached.
     */
    private void onTimeLimit() {
        if (!isActive()) {
            return;
        }
        
        LOGGER.atInfo().log("Sniper Wars match " + getMatchId() + " reached time limit");
        
        // Find player with most kills
        MatchParticipant winner = getParticipants().stream()
            .max(Comparator.comparingInt(MatchParticipant::getKills))
            .orElse(null);
        
        broadcast("Time's up!");
        end(winner);
    }

    /**
     * Broadcasts current scores to all players.
     */
    private void broadcastScores() {
        StringBuilder sb = new StringBuilder("Scores: ");
        List<MatchParticipant> sorted = getParticipants().stream()
            .sorted(Comparator.comparingInt(MatchParticipant::getKills).reversed())
            .limit(3)
            .toList();
        
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(", ");
            MatchParticipant p = sorted.get(i);
            sb.append(p.getUsername()).append(": ").append(p.getKills());
        }
        
        broadcast(sb.toString());
    }
}
