package dev.branny.hytale.servercore.player;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime session data for a connected player.
 * Tracks the player's current state within the minigame server (queue status, current match, etc.).
 * This data is NOT persisted - it only lives while the player is connected.
 */
public class PlayerSession {

    private static final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    private final UUID playerId;
    private final String username;
    private final Instant joinTime;
    
    // Queue state
    @Nullable
    private String queuedGamemode;
    @Nullable
    private Instant queueJoinTime;
    
    // Match state
    @Nullable
    private UUID currentMatchId;
    @Nullable
    private String currentGamemode;
    
    // Transient state
    private boolean isTransferring;
    private boolean isInLobby;

    public PlayerSession(@Nonnull UUID playerId, @Nonnull String username) {
        this.playerId = playerId;
        this.username = username;
        this.joinTime = Instant.now();
        this.isInLobby = true;
        this.isTransferring = false;
    }

    // ==================== Static Session Management ====================

    /**
     * Creates or gets an existing session for a player.
     *
     * @param playerRef the player reference
     * @return the player's session
     */
    @Nonnull
    public static PlayerSession getOrCreate(@Nonnull PlayerRef playerRef) {
        return sessions.computeIfAbsent(playerRef.getUuid(), 
            uuid -> new PlayerSession(uuid, playerRef.getUsername()));
    }

    /**
     * Gets an existing session for a player.
     *
     * @param playerId the player's UUID
     * @return the session, or null if not found
     */
    @Nullable
    public static PlayerSession get(@Nonnull UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Removes a player's session (called on disconnect).
     *
     * @param playerId the player's UUID
     */
    public static void remove(@Nonnull UUID playerId) {
        sessions.remove(playerId);
    }

    /**
     * Checks if a player has an active session.
     *
     * @param playerId the player's UUID
     * @return true if session exists
     */
    public static boolean exists(@Nonnull UUID playerId) {
        return sessions.containsKey(playerId);
    }

    /**
     * Gets all active sessions.
     *
     * @return map of player UUIDs to sessions
     */
    @Nonnull
    public static Map<UUID, PlayerSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }

    // ==================== Queue Management ====================

    /**
     * Marks this player as queued for a gamemode.
     *
     * @param gamemodeId the gamemode to queue for
     */
    public void joinQueue(@Nonnull String gamemodeId) {
        this.queuedGamemode = gamemodeId;
        this.queueJoinTime = Instant.now();
    }

    /**
     * Removes this player from any queue.
     */
    public void leaveQueue() {
        this.queuedGamemode = null;
        this.queueJoinTime = null;
    }

    /**
     * Checks if the player is in a queue.
     *
     * @return true if queued
     */
    public boolean isQueued() {
        return queuedGamemode != null;
    }

    /**
     * Checks if the player is queued for a specific gamemode.
     *
     * @param gamemodeId the gamemode to check
     * @return true if queued for that gamemode
     */
    public boolean isQueuedFor(@Nonnull String gamemodeId) {
        return gamemodeId.equals(queuedGamemode);
    }

    // ==================== Match Management ====================

    /**
     * Sets the player's current match.
     *
     * @param matchId the match UUID
     * @param gamemodeId the gamemode ID
     */
    public void setCurrentMatch(@Nonnull UUID matchId, @Nonnull String gamemodeId) {
        this.currentMatchId = matchId;
        this.currentGamemode = gamemodeId;
        this.queuedGamemode = null;
        this.queueJoinTime = null;
        this.isInLobby = false;
    }

    /**
     * Clears the player's current match (returned to lobby).
     */
    public void clearCurrentMatch() {
        this.currentMatchId = null;
        this.currentGamemode = null;
        this.isInLobby = true;
    }

    /**
     * Sets the player's current gamemode (for non-match gamemodes like Survival).
     *
     * @param gamemodeId the gamemode ID
     */
    public void setCurrentGamemode(@Nonnull String gamemodeId) {
        this.currentGamemode = gamemodeId;
        this.isInLobby = false;
    }

    /**
     * Clears the player's current gamemode (returned to lobby).
     */
    public void clearCurrentGamemode() {
        this.currentGamemode = null;
        this.isInLobby = true;
    }

    /**
     * Checks if the player is in a match.
     *
     * @return true if in a match
     */
    public boolean isInMatch() {
        return currentMatchId != null;
    }

    // ==================== Getters ====================

    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }

    @Nonnull
    public String getUsername() {
        return username;
    }

    @Nonnull
    public Instant getJoinTime() {
        return joinTime;
    }

    @Nullable
    public String getQueuedGamemode() {
        return queuedGamemode;
    }

    @Nullable
    public Instant getQueueJoinTime() {
        return queueJoinTime;
    }

    @Nullable
    public UUID getCurrentMatchId() {
        return currentMatchId;
    }

    @Nullable
    public String getCurrentGamemode() {
        return currentGamemode;
    }

    public boolean isTransferring() {
        return isTransferring;
    }

    public void setTransferring(boolean transferring) {
        this.isTransferring = transferring;
    }

    public boolean isInLobby() {
        return isInLobby;
    }

    public void setInLobby(boolean inLobby) {
        this.isInLobby = inLobby;
    }

    @Override
    public String toString() {
        return "PlayerSession{" +
                "playerId=" + playerId +
                ", username='" + username + '\'' +
                ", queuedGamemode='" + queuedGamemode + '\'' +
                ", currentMatchId=" + currentMatchId +
                ", isInLobby=" + isInLobby +
                '}';
    }
}
