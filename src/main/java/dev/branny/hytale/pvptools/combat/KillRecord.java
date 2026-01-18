package dev.branny.hytale.pvptools.combat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Record of a single kill event.
 * 
 * @param victimId the UUID of the player who died
 * @param victimName the username of the player who died
 * @param killerId the UUID of the killer (null for environment/self)
 * @param killerName the username of the killer (null for environment/self)
 * @param cause the cause of death (e.g., "combat", "fall", "environment")
 * @param timestamp when the kill occurred
 * @param matchId the match this kill occurred in (null if not in a match)
 * @param gamemodeId the gamemode this kill occurred in (null if not in a match)
 */
public record KillRecord(
    @Nonnull UUID victimId,
    @Nonnull String victimName,
    @Nullable UUID killerId,
    @Nullable String killerName,
    @Nonnull String cause,
    @Nonnull Instant timestamp,
    @Nullable UUID matchId,
    @Nullable String gamemodeId
) {
    /**
     * Checks if this was a player-caused kill.
     *
     * @return true if killed by another player
     */
    public boolean isPlayerKill() {
        return killerId != null && !killerId.equals(victimId);
    }

    /**
     * Checks if this was a self-kill (suicide).
     *
     * @return true if self-inflicted death
     */
    public boolean isSuicide() {
        return killerId != null && killerId.equals(victimId);
    }

    /**
     * Checks if this was an environmental death.
     *
     * @return true if killed by environment
     */
    public boolean isEnvironmentalDeath() {
        return killerId == null;
    }

    /**
     * Checks if this kill occurred during a match.
     *
     * @return true if part of a match
     */
    public boolean isInMatch() {
        return matchId != null;
    }

    @Override
    public String toString() {
        if (isPlayerKill()) {
            return victimName + " was killed by " + killerName + " (" + cause + ")";
        } else if (isSuicide()) {
            return victimName + " killed themselves (" + cause + ")";
        } else {
            return victimName + " died (" + cause + ")";
        }
    }
}
