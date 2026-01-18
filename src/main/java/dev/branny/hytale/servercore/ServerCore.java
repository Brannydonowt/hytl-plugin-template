package dev.branny.hytale.servercore;

import com.hypixel.hytale.logger.HytaleLogger;

import dev.branny.hytale.servercore.lobby.LobbyConfig;
import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Central coordination point for server core systems.
 * Provides initialization and access to core services.
 */
public final class ServerCore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Configuration constants - delegate to LobbyConfig as single source of truth
    public static final String LOBBY_WORLD_NAME = LobbyConfig.LOBBY_WORLD_NAME;
    public static final String DEFAULT_WORLD_NAME = "default";

    private static boolean initialized = false;

    private ServerCore() {
        // Utility class
    }

    /**
     * Initializes the server core systems.
     * Should be called during plugin setup.
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.atWarning().log("ServerCore already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing ServerCore...");
        initialized = true;
        LOGGER.atInfo().log("ServerCore initialized successfully");
    }

    /**
     * Cleans up server core systems.
     * Should be called during plugin shutdown.
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        LOGGER.atInfo().log("Shutting down ServerCore...");
        // Clear all sessions
        PlayerSession.getAllSessions().keySet().forEach(PlayerSession::remove);
        initialized = false;
        LOGGER.atInfo().log("ServerCore shutdown complete");
    }

    /**
     * Called when a player connects to the server.
     *
     * @param playerId the player's UUID
     * @param username the player's username
     * @return the created session
     */
    @Nonnull
    public static PlayerSession onPlayerConnect(@Nonnull UUID playerId, @Nonnull String username) {
        PlayerSession session = new PlayerSession(playerId, username);
        return session;
    }

    /**
     * Called when a player disconnects from the server.
     *
     * @param playerId the player's UUID
     */
    public static void onPlayerDisconnect(@Nonnull UUID playerId) {
        PlayerSession session = PlayerSession.get(playerId);
        if (session != null) {
            // Clean up any queue or match state
            if (session.isQueued()) {
                LOGGER.atInfo().log("Player " + session.getUsername() + " disconnected while in queue");
            }
            if (session.isInMatch()) {
                LOGGER.atInfo().log("Player " + session.getUsername() + " disconnected while in match");
                // Match cleanup will be handled by the match system
            }
        }
        PlayerSession.remove(playerId);
    }

    /**
     * Checks if the server core is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
