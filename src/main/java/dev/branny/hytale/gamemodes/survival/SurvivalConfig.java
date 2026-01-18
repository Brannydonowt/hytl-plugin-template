package dev.branny.hytale.gamemodes.survival;

import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;

/**
 * Configuration for the Survival gamemode.
 */
public final class SurvivalConfig {

    // World settings
    public static final String WORLD_NAME = "default";
    
    // Spawn coordinates
    public static final double SPAWN_X = 243.0;
    public static final double SPAWN_Y = 122.0;
    public static final double SPAWN_Z = 167.0;

    private SurvivalConfig() {
        // Configuration class
    }

    /**
     * Gets the spawn transform for the survival world.
     */
    @Nonnull
    public static Transform getSpawn() {
        return new Transform(SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0f, 0.0f, 0.0f);
    }
}
