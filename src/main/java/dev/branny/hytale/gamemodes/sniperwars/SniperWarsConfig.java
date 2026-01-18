package dev.branny.hytale.gamemodes.sniperwars;

import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Configuration for the Sniper Wars gamemode.
 */
public final class SniperWarsConfig {

    // World settings
    public static final String WORLD_NAME = "lobby_world";
    
    // Player limits
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 8;
    
    // Spawn coordinates (spread around the arena)
    private static final List<Transform> SPAWN_POINTS = List.of(
        new Transform(20.0, 108.0, 20.0, 0.0f, 225.0f, 0.0f),
        new Transform(-20.0, 108.0, 20.0, 0.0f, 315.0f, 0.0f),
        new Transform(-20.0, 108.0, -20.0, 0.0f, 45.0f, 0.0f),
        new Transform(20.0, 108.0, -20.0, 0.0f, 135.0f, 0.0f),
        new Transform(0.0, 108.0, 25.0, 0.0f, 180.0f, 0.0f),
        new Transform(0.0, 108.0, -25.0, 0.0f, 0.0f, 0.0f),
        new Transform(25.0, 108.0, 0.0, 0.0f, 270.0f, 0.0f),
        new Transform(-25.0, 108.0, 0.0, 0.0f, 90.0f, 0.0f)
    );
    
    // Loadout ID
    public static final String LOADOUT_ID = "sniper_wars";
    
    // Game rules
    public static final int KILLS_TO_WIN = 5;
    public static final int MATCH_TIME_LIMIT_SECONDS = 300; // 5 minutes
    
    // Timing
    public static final int START_COUNTDOWN_SECONDS = 5;
    public static final int END_DELAY_SECONDS = 5;

    private SniperWarsConfig() {
        // Configuration class
    }

    /**
     * Gets a spawn transform for a player by index.
     */
    @Nonnull
    public static Transform getSpawnForIndex(int index) {
        return SPAWN_POINTS.get(index % SPAWN_POINTS.size());
    }

    /**
     * Gets the number of available spawn points.
     */
    public static int getSpawnPointCount() {
        return SPAWN_POINTS.size();
    }
}
