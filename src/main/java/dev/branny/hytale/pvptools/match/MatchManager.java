package dev.branny.hytale.pvptools.match;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.pvptools.combat.EliminationEvent;
import dev.branny.hytale.pvptools.combat.KillTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active matches across the server.
 * Provides lookup and routing for match-related events.
 */
public final class MatchManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Active matches by match ID
    private static final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    
    // Player to match mapping for quick lookup
    private static final Map<UUID, UUID> playerToMatch = new ConcurrentHashMap<>();

    private MatchManager() {
        // Utility class
    }

    // ==================== Initialization ====================

    /**
     * Initializes the match manager.
     * Registers listeners for elimination events.
     */
    public static void initialize() {
        // Register for elimination events from KillTracker
        KillTracker.onElimination(MatchManager::handleElimination);
        LOGGER.atInfo().log("MatchManager initialized");
    }

    /**
     * Shuts down the match manager.
     * Cancels all active matches.
     */
    public static void shutdown() {
        // Cancel all active matches
        for (Match match : new ArrayList<>(activeMatches.values())) {
            match.cancel();
        }
        activeMatches.clear();
        playerToMatch.clear();
        LOGGER.atInfo().log("MatchManager shutdown");
    }

    // ==================== Match Registration ====================

    /**
     * Registers a match with the manager.
     *
     * @param match the match to register
     */
    public static void registerMatch(@Nonnull Match match) {
        activeMatches.put(match.getMatchId(), match);
        
        // Map all participants
        for (MatchParticipant participant : match.getParticipants()) {
            playerToMatch.put(participant.getPlayerId(), match.getMatchId());
        }
        
        LOGGER.atInfo().log("Registered match " + match.getMatchId() + " (" + match.getGamemodeId() + ")");
    }

    /**
     * Unregisters a match from the manager.
     *
     * @param matchId the match ID
     */
    public static void unregisterMatch(@Nonnull UUID matchId) {
        Match match = activeMatches.remove(matchId);
        if (match != null) {
            // Remove player mappings
            for (MatchParticipant participant : match.getParticipants()) {
                playerToMatch.remove(participant.getPlayerId());
            }
            // Clear kill tracker stats
            KillTracker.clearMatchStats(matchId);
            LOGGER.atInfo().log("Unregistered match " + matchId);
        }
    }

    // ==================== Participant Management ====================

    /**
     * Adds a player to a match.
     *
     * @param matchId the match ID
     * @param playerRef the player to add
     * @return true if added successfully
     */
    public static boolean addPlayerToMatch(@Nonnull UUID matchId, @Nonnull PlayerRef playerRef) {
        Match match = activeMatches.get(matchId);
        if (match == null) {
            LOGGER.atWarning().log("Match not found: " + matchId);
            return false;
        }

        if (match.addParticipant(playerRef)) {
            playerToMatch.put(playerRef.getUuid(), matchId);
            return true;
        }
        return false;
    }

    /**
     * Removes a player from their current match.
     *
     * @param playerId the player's UUID
     * @return the match they were removed from, or null
     */
    @Nullable
    public static Match removePlayerFromMatch(@Nonnull UUID playerId) {
        UUID matchId = playerToMatch.remove(playerId);
        if (matchId == null) {
            return null;
        }

        Match match = activeMatches.get(matchId);
        if (match != null) {
            match.removeParticipant(playerId);
        }
        return match;
    }

    // ==================== Lookup ====================

    /**
     * Gets a match by ID.
     *
     * @param matchId the match ID
     * @return the match, or null if not found
     */
    @Nullable
    public static Match getMatch(@Nonnull UUID matchId) {
        return activeMatches.get(matchId);
    }

    /**
     * Gets the match a player is in.
     *
     * @param playerId the player's UUID
     * @return the match, or null if not in a match
     */
    @Nullable
    public static Match getMatchForPlayer(@Nonnull UUID playerId) {
        UUID matchId = playerToMatch.get(playerId);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    /**
     * Checks if a player is in a match.
     *
     * @param playerId the player's UUID
     * @return true if in a match
     */
    public static boolean isInMatch(@Nonnull UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    /**
     * Checks if a player is in a specific match.
     *
     * @param playerId the player's UUID
     * @param matchId the match ID
     * @return true if in that match
     */
    public static boolean isInMatch(@Nonnull UUID playerId, @Nonnull UUID matchId) {
        UUID currentMatch = playerToMatch.get(playerId);
        return matchId.equals(currentMatch);
    }

    /**
     * Gets all active matches.
     *
     * @return collection of active matches
     */
    @Nonnull
    public static Collection<Match> getActiveMatches() {
        return Collections.unmodifiableCollection(activeMatches.values());
    }

    /**
     * Gets all active matches for a specific gamemode.
     *
     * @param gamemodeId the gamemode ID
     * @return list of matches for that gamemode
     */
    @Nonnull
    public static List<Match> getMatchesForGamemode(@Nonnull String gamemodeId) {
        return activeMatches.values().stream()
            .filter(m -> gamemodeId.equals(m.getGamemodeId()))
            .toList();
    }

    /**
     * Gets the count of active matches.
     *
     * @return match count
     */
    public static int getActiveMatchCount() {
        return activeMatches.size();
    }

    // ==================== Event Handling ====================

    /**
     * Handles elimination events from the KillTracker.
     * Routes to the appropriate match.
     */
    private static void handleElimination(@Nonnull EliminationEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Match match = activeMatches.get(event.getMatchId());
        if (match != null && match.isActive()) {
            // Update participant stats
            MatchParticipant victim = match.getParticipant(event.getEliminatedId());
            if (victim != null) {
                victim.recordDeath();
                victim.eliminate();
            }

            if (event.isPlayerElimination()) {
                MatchParticipant killer = match.getParticipant(event.getEliminatorId());
                if (killer != null) {
                    killer.recordKill();
                }
            }

            // Delegate to match for game-specific handling
            match.onElimination(event);
        }
    }
}
