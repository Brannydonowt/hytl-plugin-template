package dev.branny.hytale.pvptools.match;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import dev.branny.hytale.pvptools.combat.EliminationEvent;
import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.loadout.LoadoutService;
import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all match types.
 * Gamemodes extend this class to implement their specific game logic.
 */
public abstract class Match {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected final UUID matchId;
    protected final String gamemodeId;
    protected final String worldName;
    protected final Instant createdAt;
    protected Instant startedAt;
    protected Instant endedAt;
    
    protected MatchState state;
    protected final Map<UUID, MatchParticipant> participants;
    
    // Configuration
    protected final int minPlayers;
    protected final int maxPlayers;
    @Nullable
    protected Loadout defaultLoadout;

    protected Match(@Nonnull String gamemodeId, @Nonnull String worldName, 
                   int minPlayers, int maxPlayers) {
        this.matchId = UUID.randomUUID();
        this.gamemodeId = gamemodeId;
        this.worldName = worldName;
        this.createdAt = Instant.now();
        this.state = MatchState.WAITING;
        this.participants = new ConcurrentHashMap<>();
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    // ==================== Abstract Methods ====================

    /**
     * Called when the match starts.
     * Implement gamemode-specific start logic.
     */
    protected abstract void onStart();

    /**
     * Called when the match ends.
     * Implement gamemode-specific end logic.
     *
     * @param result the match result
     */
    protected abstract void onEnd(@Nonnull MatchResult result);

    /**
     * Called when a player is eliminated.
     * Implement gamemode-specific elimination logic.
     *
     * @param event the elimination event
     */
    protected abstract void onElimination(@Nonnull EliminationEvent event);

    /**
     * Gets the spawn position for a participant.
     *
     * @param participant the participant
     * @param index the participant's index (for spawn position calculation)
     * @return the spawn transform
     */
    @Nonnull
    protected abstract com.hypixel.hytale.math.vector.Transform getSpawnPosition(
        @Nonnull MatchParticipant participant, int index);

    // ==================== Lifecycle Methods ====================

    /**
     * Starts the match.
     * Transitions state to STARTING, then ACTIVE.
     */
    public void start() {
        if (state != MatchState.WAITING) {
            LOGGER.atWarning().log("Cannot start match in state: " + state);
            return;
        }

        if (participants.size() < minPlayers) {
            LOGGER.atWarning().log("Not enough players to start match");
            return;
        }

        LOGGER.atInfo().log("Starting match " + matchId + " (" + gamemodeId + ")");
        state = MatchState.STARTING;
        startedAt = Instant.now();

        // Apply loadouts to all participants
        if (defaultLoadout != null) {
            for (MatchParticipant participant : participants.values()) {
                LoadoutService.applyLoadoutAsync(participant.getPlayerRef(), defaultLoadout);
            }
        }

        // Update player sessions
        for (MatchParticipant participant : participants.values()) {
            PlayerSession session = PlayerSession.get(participant.getPlayerId());
            if (session != null) {
                session.setCurrentMatch(matchId, gamemodeId);
            }
        }

        // Transition to active
        state = MatchState.ACTIVE;
        onStart();
    }

    /**
     * Ends the match with the given result.
     *
     * @param winner the winning participant (or null for draw/cancelled)
     */
    public void end(@Nullable MatchParticipant winner) {
        if (state.isFinished()) {
            LOGGER.atWarning().log("Match already finished");
            return;
        }

        LOGGER.atInfo().log("Ending match " + matchId);
        state = MatchState.ENDING;
        endedAt = Instant.now();

        // Build result
        MatchResult result = MatchResult.builder(matchId, gamemodeId)
            .startTime(startedAt != null ? startedAt : createdAt)
            .endTime(endedAt)
            .winner(winner)
            .addParticipants(participants.values())
            .build();

        // Clear player sessions
        for (MatchParticipant participant : participants.values()) {
            PlayerSession session = PlayerSession.get(participant.getPlayerId());
            if (session != null) {
                session.clearCurrentMatch();
            }
            // Clear loadouts
            LoadoutService.clearLoadoutAsync(participant.getPlayerRef());
        }

        state = MatchState.COMPLETED;
        onEnd(result);
    }

    /**
     * Cancels the match.
     */
    public void cancel() {
        if (state.isFinished()) {
            return;
        }

        LOGGER.atInfo().log("Cancelling match " + matchId);
        state = MatchState.CANCELLED;
        endedAt = Instant.now();

        // Clear player sessions
        for (MatchParticipant participant : participants.values()) {
            PlayerSession session = PlayerSession.get(participant.getPlayerId());
            if (session != null) {
                session.clearCurrentMatch();
            }
            LoadoutService.clearLoadoutAsync(participant.getPlayerRef());
        }

        MatchResult result = MatchResult.builder(matchId, gamemodeId)
            .startTime(startedAt != null ? startedAt : createdAt)
            .endTime(endedAt)
            .finalState(MatchState.CANCELLED)
            .addParticipants(participants.values())
            .build();

        onEnd(result);
    }

    // ==================== Participant Management ====================

    /**
     * Adds a player to the match.
     *
     * @param playerRef the player to add
     * @return true if added successfully
     */
    public boolean addParticipant(@Nonnull PlayerRef playerRef) {
        if (!state.canJoin()) {
            LOGGER.atWarning().log("Cannot join match in state: " + state);
            return false;
        }

        if (participants.size() >= maxPlayers) {
            LOGGER.atWarning().log("Match is full");
            return false;
        }

        UUID playerId = playerRef.getUuid();
        if (participants.containsKey(playerId)) {
            LOGGER.atWarning().log("Player already in match");
            return false;
        }

        MatchParticipant participant = new MatchParticipant(playerRef, matchId);
        participants.put(playerId, participant);

        LOGGER.atInfo().log("Added " + playerRef.getUsername() + " to match " + matchId);
        return true;
    }

    /**
     * Removes a player from the match.
     *
     * @param playerId the player's UUID
     * @return the removed participant, or null
     */
    @Nullable
    public MatchParticipant removeParticipant(@Nonnull UUID playerId) {
        MatchParticipant participant = participants.remove(playerId);
        if (participant != null) {
            LOGGER.atInfo().log("Removed " + participant.getUsername() + " from match " + matchId);
            
            // Clear their session
            PlayerSession session = PlayerSession.get(playerId);
            if (session != null) {
                session.clearCurrentMatch();
            }
        }
        return participant;
    }

    /**
     * Gets a participant by player ID.
     */
    @Nullable
    public MatchParticipant getParticipant(@Nonnull UUID playerId) {
        return participants.get(playerId);
    }

    /**
     * Checks if a player is in this match.
     */
    public boolean isParticipant(@Nonnull UUID playerId) {
        return participants.containsKey(playerId);
    }

    /**
     * Gets all participants.
     */
    @Nonnull
    public Collection<MatchParticipant> getParticipants() {
        return Collections.unmodifiableCollection(participants.values());
    }

    /**
     * Gets participants that are still active (not eliminated).
     */
    @Nonnull
    public List<MatchParticipant> getActiveParticipants() {
        return participants.values().stream()
            .filter(MatchParticipant::isActive)
            .toList();
    }

    // ==================== Messaging ====================

    /**
     * Broadcasts a message to all participants.
     *
     * @param message the message to send
     */
    public void broadcast(@Nonnull Message message) {
        for (MatchParticipant participant : participants.values()) {
            participant.getPlayerRef().sendMessage(message);
        }
    }

    /**
     * Broadcasts a message string to all participants.
     *
     * @param message the message string
     */
    public void broadcast(@Nonnull String message) {
        broadcast(Message.raw(message));
    }

    /**
     * Shows an EventTitle to all participants.
     *
     * @param title the title
     * @param subtitle the subtitle
     */
    public void broadcastTitle(@Nonnull String title, @Nonnull String subtitle) {
        for (MatchParticipant participant : participants.values()) {
            EventTitleUtil.showEventTitleToPlayer(
                participant.getPlayerRef(),
                Message.raw(title),
                Message.raw(subtitle),
                true
            );
        }
    }

    // ==================== Getters ====================

    @Nonnull
    public UUID getMatchId() {
        return matchId;
    }

    @Nonnull
    public String getGamemodeId() {
        return gamemodeId;
    }

    @Nonnull
    public String getWorldName() {
        return worldName;
    }

    @Nonnull
    public MatchState getState() {
        return state;
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Nullable
    public Loadout getDefaultLoadout() {
        return defaultLoadout;
    }

    public void setDefaultLoadout(@Nullable Loadout loadout) {
        this.defaultLoadout = loadout;
    }

    public boolean isActive() {
        return state.isActive();
    }

    public boolean isFinished() {
        return state.isFinished();
    }

    public boolean canStart() {
        return state == MatchState.WAITING && participants.size() >= minPlayers;
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + matchId +
                ", gamemode=" + gamemodeId +
                ", state=" + state +
                ", participants=" + participants.size() +
                '}';
    }
}
