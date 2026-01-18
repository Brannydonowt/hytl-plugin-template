package dev.branny.hytale.servercore.lobby;

import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;

/**
 * Configuration for the lobby system.
 */
public class LobbyConfig {

    // Lobby world settings
    public static final String LOBBY_WORLD_NAME = "lobby_world";
    
    // Spawn coordinates
    public static final double SPAWN_X = 2.5;
    public static final double SPAWN_Y = 108.0;
    public static final double SPAWN_Z = 0.0;

    // Queue settings
    public static final int MAX_QUEUE_TIME_SECONDS = 300; // 5 minutes
    public static final int QUEUE_CHECK_INTERVAL_MS = 1000; // 1 second

    private LobbyConfig() {
        // Configuration class
    }

    /**
     * Gets the lobby spawn transform.
     */
    @Nonnull
    public static Transform getLobbySpawn() {
        return new Transform(SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0f, 0.0f, 0.0f);
    }
}
