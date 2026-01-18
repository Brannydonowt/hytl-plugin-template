package dev.branny.hytale.pvptools.match;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player participating in a match.
 * Wraps PlayerRef with match-specific context and stats.
 */
public class MatchParticipant {

    private final PlayerRef playerRef;
    private final UUID matchId;
    private final Instant joinTime;
    
    // In-match stats
    private int kills;
    private int deaths;
    private int score;
    private boolean eliminated;
    private Instant eliminationTime;
    
    // Team support (for team-based gamemodes)
    @Nullable
    private String teamId;
    
    // Custom metadata
    private final Map<String, Object> metadata;

    public MatchParticipant(@Nonnull PlayerRef playerRef, @Nonnull UUID matchId) {
        this.playerRef = playerRef;
        this.matchId = matchId;
        this.joinTime = Instant.now();
        this.kills = 0;
        this.deaths = 0;
        this.score = 0;
        this.eliminated = false;
        this.metadata = new HashMap<>();
    }

    // ==================== Getters ====================

    @Nonnull
    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Nonnull
    public UUID getPlayerId() {
        return playerRef.getUuid();
    }

    @Nonnull
    public String getUsername() {
        return playerRef.getUsername();
    }

    @Nonnull
    public UUID getMatchId() {
        return matchId;
    }

    @Nonnull
    public Instant getJoinTime() {
        return joinTime;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getScore() {
        return score;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    @Nullable
    public Instant getEliminationTime() {
        return eliminationTime;
    }

    @Nullable
    public String getTeamId() {
        return teamId;
    }

    // ==================== Setters ====================

    public void setTeamId(@Nullable String teamId) {
        this.teamId = teamId;
    }

    // ==================== Stats Updates ====================

    /**
     * Records a kill for this participant.
     *
     * @param scoreValue the score value for the kill
     */
    public void recordKill(int scoreValue) {
        this.kills++;
        this.score += scoreValue;
    }

    /**
     * Records a kill with default score (1).
     */
    public void recordKill() {
        recordKill(1);
    }

    /**
     * Records a death for this participant.
     */
    public void recordDeath() {
        this.deaths++;
    }

    /**
     * Marks this participant as eliminated.
     */
    public void eliminate() {
        this.eliminated = true;
        this.eliminationTime = Instant.now();
    }

    /**
     * Adds score to this participant.
     *
     * @param amount the amount to add
     */
    public void addScore(int amount) {
        this.score += amount;
    }

    /**
     * Sets the score for this participant.
     *
     * @param score the new score
     */
    public void setScore(int score) {
        this.score = score;
    }

    // ==================== Metadata ====================

    /**
     * Gets a metadata value.
     *
     * @param key the key
     * @param <T> the expected type
     * @return the value, or null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMeta(@Nonnull String key) {
        return (T) metadata.get(key);
    }

    /**
     * Gets a metadata value with default.
     *
     * @param key the key
     * @param defaultValue the default
     * @param <T> the expected type
     * @return the value, or default
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T getMeta(@Nonnull String key, @Nonnull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Sets a metadata value.
     *
     * @param key the key
     * @param value the value
     */
    public void setMeta(@Nonnull String key, @Nullable Object value) {
        if (value != null) {
            metadata.put(key, value);
        } else {
            metadata.remove(key);
        }
    }

    // ==================== Utility ====================

    /**
     * Gets the kill/death ratio.
     *
     * @return K/D ratio, or kills if no deaths
     */
    public double getKDRatio() {
        return deaths > 0 ? (double) kills / deaths : kills;
    }

    /**
     * Checks if this participant is still active (not eliminated).
     */
    public boolean isActive() {
        return !eliminated;
    }

    /**
     * Checks if this participant is on the same team as another.
     *
     * @param other the other participant
     * @return true if same team (or both have no team)
     */
    public boolean isSameTeam(@Nonnull MatchParticipant other) {
        if (teamId == null && other.teamId == null) {
            return false; // No teams = not teammates
        }
        return teamId != null && teamId.equals(other.teamId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchParticipant that = (MatchParticipant) o;
        return getPlayerId().equals(that.getPlayerId()) && matchId.equals(that.matchId);
    }

    @Override
    public int hashCode() {
        return getPlayerId().hashCode() * 31 + matchId.hashCode();
    }

    @Override
    public String toString() {
        return "MatchParticipant{" +
                "player=" + getUsername() +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", score=" + score +
                ", eliminated=" + eliminated +
                '}';
    }
}
