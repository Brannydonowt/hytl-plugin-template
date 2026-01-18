package dev.branny.hytale.gamemodes.arena;

import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;

/**
 * Configuration for the Arena gamemode.
 */
public final class ArenaConfig {

    // World settings
    public static final String WORLD_NAME = "lobby_world"; // For now, arena runs in lobby world
    
    // Player limits
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 2;
    
    // Spawn coordinates
    public static final double P1_SPAWN_X = 10.0;
    public static final double P1_SPAWN_Y = 108.0;
    public static final double P1_SPAWN_Z = 10.0;
    
    public static final double P2_SPAWN_X = -10.0;
    public static final double P2_SPAWN_Y = 108.0;
    public static final double P2_SPAWN_Z = -10.0;
    
    // Loadout ID
    public static final String LOADOUT_ID = "arena_default";
    
    // Timing
    public static final int START_COUNTDOWN_SECONDS = 3;
    public static final int END_DELAY_SECONDS = 3;

    private ArenaConfig() {
        // Configuration class
    }

    /**
     * Gets the spawn transform for player 1.
     */
    @Nonnull
    public static Transform getPlayer1Spawn() {
        return new Transform(P1_SPAWN_X, P1_SPAWN_Y, P1_SPAWN_Z, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Gets the spawn transform for player 2.
     */
    @Nonnull
    public static Transform getPlayer2Spawn() {
        return new Transform(P2_SPAWN_X, P2_SPAWN_Y, P2_SPAWN_Z, 0.0f, 180.0f, 0.0f);
    }

    /**
     * Gets the spawn transform for a participant by index.
     *
     * @param index the participant index (0 or 1)
     */
    @Nonnull
    public static Transform getSpawnForIndex(int index) {
        return index == 0 ? getPlayer1Spawn() : getPlayer2Spawn();
    }
}
