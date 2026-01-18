package dev.branny.hytale.gamemodes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all gamemodes.
 * Gamemodes must be registered here to be available for queuing and matching.
 */
public final class GamemodeRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<String, Gamemode> gamemodes = new ConcurrentHashMap<>();

    private GamemodeRegistry() {
        // Utility class
    }

    // ==================== Registration ====================

    /**
     * Registers a gamemode.
     *
     * @param gamemode the gamemode to register
     */
    public static void register(@Nonnull Gamemode gamemode) {
        String id = gamemode.getId().toLowerCase();
        
        if (gamemodes.containsKey(id)) {
            LOGGER.atWarning().log("Overwriting existing gamemode: " + id);
        }
        
        gamemodes.put(id, gamemode);
        gamemode.onRegister();
        
        LOGGER.atInfo().log("Registered gamemode: " + gamemode.getDisplayName() + 
            " (" + id + ") [" + gamemode.getMinPlayers() + "-" + gamemode.getMaxPlayers() + " players]");
    }

    /**
     * Registers a gamemode and its commands.
     *
     * @param gamemode the gamemode to register
     * @param commandRegistry the command registry for registering commands
     */
    public static void register(@Nonnull Gamemode gamemode, @Nonnull CommandRegistry commandRegistry) {
        register(gamemode);
        gamemode.registerCommands(commandRegistry);
    }

    /**
     * Unregisters a gamemode.
     *
     * @param id the gamemode ID
     * @return the removed gamemode, or null if not found
     */
    @Nullable
    public static Gamemode unregister(@Nonnull String id) {
        Gamemode gamemode = gamemodes.remove(id.toLowerCase());
        if (gamemode != null) {
            gamemode.onShutdown();
            LOGGER.atInfo().log("Unregistered gamemode: " + id);
        }
        return gamemode;
    }

    // ==================== Lookup ====================

    /**
     * Gets a gamemode by ID.
     *
     * @param id the gamemode ID
     * @return the gamemode, or null if not found
     */
    @Nullable
    public static Gamemode get(@Nonnull String id) {
        return gamemodes.get(id.toLowerCase());
    }

    /**
     * Checks if a gamemode is registered.
     *
     * @param id the gamemode ID
     * @return true if registered
     */
    public static boolean exists(@Nonnull String id) {
        return gamemodes.containsKey(id.toLowerCase());
    }

    /**
     * Gets all registered gamemode IDs.
     *
     * @return collection of gamemode IDs
     */
    @Nonnull
    public static Collection<String> getRegisteredIds() {
        return Collections.unmodifiableSet(gamemodes.keySet());
    }

    /**
     * Gets all registered gamemodes.
     *
     * @return collection of gamemodes
     */
    @Nonnull
    public static Collection<Gamemode> getAll() {
        return Collections.unmodifiableCollection(gamemodes.values());
    }

    /**
     * Gets all enabled gamemodes.
     *
     * @return list of enabled gamemodes
     */
    @Nonnull
    public static List<Gamemode> getEnabled() {
        return gamemodes.values().stream()
            .filter(Gamemode::isEnabled)
            .toList();
    }

    /**
     * Gets all gamemodes that can be queued for.
     *
     * @return list of queueable gamemodes
     */
    @Nonnull
    public static List<Gamemode> getQueueable() {
        return gamemodes.values().stream()
            .filter(Gamemode::canQueue)
            .toList();
    }

    /**
     * Gets the count of registered gamemodes.
     *
     * @return count
     */
    public static int getCount() {
        return gamemodes.size();
    }

    /**
     * Finds a gamemode by its world name.
     *
     * @param worldName the world name to search for
     * @return the gamemode that uses this world, or null if none found
     */
    @Nullable
    public static Gamemode getByWorldName(@Nonnull String worldName) {
        for (Gamemode gamemode : gamemodes.values()) {
            if (worldName.equals(gamemode.getWorldName())) {
                return gamemode;
            }
        }
        return null;
    }

    // ==================== Lifecycle ====================

    /**
     * Shuts down all gamemodes.
     * Called during server shutdown.
     */
    public static void shutdown() {
        for (Gamemode gamemode : gamemodes.values()) {
            try {
                gamemode.onShutdown();
            } catch (Exception e) {
                LOGGER.atWarning().log("Error shutting down gamemode " + gamemode.getId() + ": " + e.getMessage());
            }
        }
        gamemodes.clear();
        LOGGER.atInfo().log("All gamemodes shut down");
    }
}
