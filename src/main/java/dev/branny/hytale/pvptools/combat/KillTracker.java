package dev.branny.hytale.pvptools.combat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tracks kills and deaths across the server.
 * Maintains per-player and per-match statistics.
 */
public final class KillTracker {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Per-match kill counts (matchId -> playerId -> kills)
    private static final Map<UUID, Map<UUID, Integer>> matchKills = new ConcurrentHashMap<>();
    
    // Per-match death counts (matchId -> playerId -> deaths)
    private static final Map<UUID, Map<UUID, Integer>> matchDeaths = new ConcurrentHashMap<>();
    
    // Recent kills (for kill feed, limited size)
    private static final List<KillRecord> recentKills = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_KILLS = 100;
    
    // Elimination event listeners
    private static final List<Consumer<EliminationEvent>> eliminationListeners = new CopyOnWriteArrayList<>();

    private KillTracker() {
        // Utility class
    }

    // ==================== Event Registration ====================

    /**
     * Registers a listener for elimination events.
     *
     * @param listener the listener to register
     */
    public static void onElimination(@Nonnull Consumer<EliminationEvent> listener) {
        eliminationListeners.add(listener);
    }

    /**
     * Unregisters an elimination listener.
     *
     * @param listener the listener to remove
     */
    public static void removeEliminationListener(@Nonnull Consumer<EliminationEvent> listener) {
        eliminationListeners.remove(listener);
    }

    // ==================== Recording Kills ====================

    /**
     * Records a kill/death event.
     * Called by the CombatListener when a player dies.
     *
     * @param victim the player who died
     * @param killer the player who killed them (null for environment)
     * @param cause the cause of death
     * @param matchId the match ID (null if not in match)
     * @param gamemodeId the gamemode ID (null if not in match)
     */
    public static void recordKill(
            @Nonnull PlayerRef victim,
            @Nullable PlayerRef killer,
            @Nonnull String cause,
            @Nullable UUID matchId,
            @Nullable String gamemodeId) {

        UUID victimId = victim.getUuid();
        UUID killerId = killer != null ? killer.getUuid() : null;

        // Create kill record
        KillRecord record = new KillRecord(
            victimId,
            victim.getUsername(),
            killerId,
            killer != null ? killer.getUsername() : null,
            cause,
            Instant.now(),
            matchId,
            gamemodeId
        );

        // Add to recent kills
        recentKills.add(0, record);
        while (recentKills.size() > MAX_RECENT_KILLS) {
            recentKills.remove(recentKills.size() - 1);
        }

        // Update match stats if in a match
        if (matchId != null) {
            // Record death for victim
            matchDeaths.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>())
                .merge(victimId, 1, Integer::sum);

            // Record kill for killer (if player kill)
            if (killerId != null && !killerId.equals(victimId)) {
                matchKills.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>())
                    .merge(killerId, 1, Integer::sum);
            }

            // Fire elimination event
            EliminationEvent event = new EliminationEvent(
                victim, killer, matchId, gamemodeId, cause, 0
            );
            fireEliminationEvent(event);
        }

        LOGGER.atInfo().log("Recorded kill: " + record);
    }

    /**
     * Fires an elimination event to all listeners.
     */
    private static void fireEliminationEvent(@Nonnull EliminationEvent event) {
        for (Consumer<EliminationEvent> listener : eliminationListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in elimination listener: " + e.getMessage());
            }
        }
    }

    // ==================== Match Stats ====================

    /**
     * Gets the kill count for a player in a match.
     *
     * @param matchId the match ID
     * @param playerId the player's UUID
     * @return the kill count
     */
    public static int getMatchKills(@Nonnull UUID matchId, @Nonnull UUID playerId) {
        Map<UUID, Integer> kills = matchKills.get(matchId);
        return kills != null ? kills.getOrDefault(playerId, 0) : 0;
    }

    /**
     * Gets the death count for a player in a match.
     *
     * @param matchId the match ID
     * @param playerId the player's UUID
     * @return the death count
     */
    public static int getMatchDeaths(@Nonnull UUID matchId, @Nonnull UUID playerId) {
        Map<UUID, Integer> deaths = matchDeaths.get(matchId);
        return deaths != null ? deaths.getOrDefault(playerId, 0) : 0;
    }

    /**
     * Gets all kills for a match.
     *
     * @param matchId the match ID
     * @return map of player UUIDs to kill counts
     */
    @Nonnull
    public static Map<UUID, Integer> getAllMatchKills(@Nonnull UUID matchId) {
        Map<UUID, Integer> kills = matchKills.get(matchId);
        return kills != null ? new HashMap<>(kills) : Collections.emptyMap();
    }

    /**
     * Gets all deaths for a match.
     *
     * @param matchId the match ID
     * @return map of player UUIDs to death counts
     */
    @Nonnull
    public static Map<UUID, Integer> getAllMatchDeaths(@Nonnull UUID matchId) {
        Map<UUID, Integer> deaths = matchDeaths.get(matchId);
        return deaths != null ? new HashMap<>(deaths) : Collections.emptyMap();
    }

    /**
     * Gets the player with the most kills in a match.
     *
     * @param matchId the match ID
     * @return the player UUID with most kills, or null if no kills
     */
    @Nullable
    public static UUID getMatchTopKiller(@Nonnull UUID matchId) {
        Map<UUID, Integer> kills = matchKills.get(matchId);
        if (kills == null || kills.isEmpty()) {
            return null;
        }
        return kills.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Clears all stats for a match (called when match ends).
     *
     * @param matchId the match ID
     */
    public static void clearMatchStats(@Nonnull UUID matchId) {
        matchKills.remove(matchId);
        matchDeaths.remove(matchId);
        LOGGER.atInfo().log("Cleared stats for match " + matchId);
    }

    // ==================== Recent Kills ====================

    /**
     * Gets recent kills.
     *
     * @param count the maximum number to return
     * @return list of recent kill records
     */
    @Nonnull
    public static List<KillRecord> getRecentKills(int count) {
        int size = Math.min(count, recentKills.size());
        return new ArrayList<>(recentKills.subList(0, size));
    }

    /**
     * Gets recent kills for a specific player.
     *
     * @param playerId the player's UUID
     * @param count the maximum number to return
     * @return list of recent kill records involving this player
     */
    @Nonnull
    public static List<KillRecord> getRecentKillsForPlayer(@Nonnull UUID playerId, int count) {
        return recentKills.stream()
            .filter(r -> playerId.equals(r.victimId()) || playerId.equals(r.killerId()))
            .limit(count)
            .toList();
    }

    /**
     * Gets recent kills by a specific player (as killer).
     *
     * @param playerId the player's UUID
     * @param count the maximum number to return
     * @return list of recent kills by this player
     */
    @Nonnull
    public static List<KillRecord> getRecentKillsByPlayer(@Nonnull UUID playerId, int count) {
        return recentKills.stream()
            .filter(r -> playerId.equals(r.killerId()))
            .limit(count)
            .toList();
    }

    // ==================== Cleanup ====================

    /**
     * Clears all tracking data.
     */
    public static void clearAll() {
        matchKills.clear();
        matchDeaths.clear();
        recentKills.clear();
        LOGGER.atInfo().log("Cleared all kill tracker data");
    }
}
