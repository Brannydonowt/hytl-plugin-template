package dev.branny.hytale.servercore.lobby;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player waiting in a queue for a gamemode.
 */
public class QueueEntry implements Comparable<QueueEntry> {

    private final PlayerRef playerRef;
    private final String gamemodeId;
    private final Instant joinTime;

    public QueueEntry(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        this.playerRef = playerRef;
        this.gamemodeId = gamemodeId;
        this.joinTime = Instant.now();
    }

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
    public String getGamemodeId() {
        return gamemodeId;
    }

    @Nonnull
    public Instant getJoinTime() {
        return joinTime;
    }

    /**
     * Gets how long this player has been in queue in seconds.
     */
    public long getWaitTimeSeconds() {
        return java.time.Duration.between(joinTime, Instant.now()).toSeconds();
    }

    /**
     * Checks if the player is still connected.
     */
    public boolean isStillConnected() {
        return playerRef.getPacketHandler().stillActive();
    }

    @Override
    public int compareTo(@Nonnull QueueEntry other) {
        // Earlier join time = higher priority
        return this.joinTime.compareTo(other.joinTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueEntry that = (QueueEntry) o;
        return getPlayerId().equals(that.getPlayerId());
    }

    @Override
    public int hashCode() {
        return getPlayerId().hashCode();
    }

    @Override
    public String toString() {
        return "QueueEntry{" +
                "player=" + getUsername() +
                ", gamemode=" + gamemodeId +
                ", waitTime=" + getWaitTimeSeconds() + "s" +
                '}';
    }
}
