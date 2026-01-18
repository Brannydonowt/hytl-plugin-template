package dev.branny.hytale.pvptools.combat;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a player is eliminated (dies) in a match context.
 * Gamemodes listen for this event to handle eliminations appropriately.
 */
public class EliminationEvent {

    private final PlayerRef eliminated;
    private final PlayerRef eliminator; // may be null for environment kills
    private final UUID matchId;
    private final String gamemodeId;
    private final String cause;
    private final Instant timestamp;
    private final float damageAmount;
    private boolean cancelled;

    public EliminationEvent(
            @Nonnull PlayerRef eliminated,
            @Nullable PlayerRef eliminator,
            @Nonnull UUID matchId,
            @Nonnull String gamemodeId,
            @Nonnull String cause,
            float damageAmount) {
        this.eliminated = eliminated;
        this.eliminator = eliminator;
        this.matchId = matchId;
        this.gamemodeId = gamemodeId;
        this.cause = cause;
        this.timestamp = Instant.now();
        this.damageAmount = damageAmount;
        this.cancelled = false;
    }

    // ==================== Getters ====================

    /**
     * Gets the player who was eliminated.
     */
    @Nonnull
    public PlayerRef getEliminated() {
        return eliminated;
    }

    /**
     * Gets the UUID of the eliminated player.
     */
    @Nonnull
    public UUID getEliminatedId() {
        return eliminated.getUuid();
    }

    /**
     * Gets the username of the eliminated player.
     */
    @Nonnull
    public String getEliminatedName() {
        return eliminated.getUsername();
    }

    /**
     * Gets the player who caused the elimination (killer).
     *
     * @return the eliminator, or null for environment/self kills
     */
    @Nullable
    public PlayerRef getEliminator() {
        return eliminator;
    }

    /**
     * Gets the UUID of the eliminator, if any.
     */
    @Nullable
    public UUID getEliminatorId() {
        return eliminator != null ? eliminator.getUuid() : null;
    }

    /**
     * Gets the username of the eliminator, if any.
     */
    @Nullable
    public String getEliminatorName() {
        return eliminator != null ? eliminator.getUsername() : null;
    }

    /**
     * Gets the match ID this elimination occurred in.
     */
    @Nonnull
    public UUID getMatchId() {
        return matchId;
    }

    /**
     * Gets the gamemode ID.
     */
    @Nonnull
    public String getGamemodeId() {
        return gamemodeId;
    }

    /**
     * Gets the cause of elimination.
     */
    @Nonnull
    public String getCause() {
        return cause;
    }

    /**
     * Gets when the elimination occurred.
     */
    @Nonnull
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the damage amount that caused the elimination.
     */
    public float getDamageAmount() {
        return damageAmount;
    }

    // ==================== State Checks ====================

    /**
     * Checks if this was a player-caused elimination.
     */
    public boolean isPlayerElimination() {
        return eliminator != null && !eliminator.getUuid().equals(eliminated.getUuid());
    }

    /**
     * Checks if this was a self-elimination (suicide).
     */
    public boolean isSelfElimination() {
        return eliminator != null && eliminator.getUuid().equals(eliminated.getUuid());
    }

    /**
     * Checks if this was an environmental elimination.
     */
    public boolean isEnvironmentalElimination() {
        return eliminator == null;
    }

    // ==================== Cancellation ====================

    /**
     * Checks if this event has been cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state of this event.
     * If cancelled, the elimination will not be processed by the match system.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // ==================== Conversion ====================

    /**
     * Converts this event to a KillRecord for persistence.
     */
    @Nonnull
    public KillRecord toKillRecord() {
        return new KillRecord(
            eliminated.getUuid(),
            eliminated.getUsername(),
            eliminator != null ? eliminator.getUuid() : null,
            eliminator != null ? eliminator.getUsername() : null,
            cause,
            timestamp,
            matchId,
            gamemodeId
        );
    }

    @Override
    public String toString() {
        if (isPlayerElimination()) {
            return getEliminatedName() + " eliminated by " + getEliminatorName() + 
                   " in " + gamemodeId + " match " + matchId;
        } else if (isSelfElimination()) {
            return getEliminatedName() + " self-eliminated in " + gamemodeId + 
                   " match " + matchId;
        } else {
            return getEliminatedName() + " eliminated by environment in " + 
                   gamemodeId + " match " + matchId;
        }
    }
}
