package dev.branny.hytale.gamemodes;

import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.match.Match;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Base interface for all gamemodes.
 * Implement this interface to create new gamemodes for the server.
 */
public interface Gamemode {

    /**
     * Gets the unique identifier for this gamemode.
     * Used for commands, queue names, etc.
     *
     * @return the gamemode ID (e.g., "arena", "sniperwars", "ffa")
     */
    @Nonnull
    String getId();

    /**
     * Gets the display name for this gamemode.
     * Shown to players in menus and messages.
     *
     * @return the display name (e.g., "1v1 Arena", "Sniper Wars")
     */
    @Nonnull
    String getDisplayName();

    /**
     * Gets a brief description of this gamemode.
     *
     * @return the description
     */
    @Nonnull
    String getDescription();

    /**
     * Gets the minimum number of players required to start a match.
     *
     * @return minimum players
     */
    int getMinPlayers();

    /**
     * Gets the maximum number of players allowed in a match.
     *
     * @return maximum players
     */
    int getMaxPlayers();

    /**
     * Gets the name of the world used for this gamemode's matches.
     *
     * @return the world name
     */
    @Nonnull
    String getWorldName();

    /**
     * Creates a new match for this gamemode.
     *
     * @param players the players to add to the match
     * @return the created match
     */
    @Nonnull
    Match createMatch(@Nonnull List<PlayerRef> players);

    /**
     * Gets the default loadout for this gamemode.
     *
     * @return the default loadout, or null if no loadout
     */
    @Nullable
    Loadout getDefaultLoadout();

    /**
     * Registers commands for this gamemode.
     * Called during plugin initialization.
     *
     * @param registry the command registry
     */
    default void registerCommands(@Nonnull CommandRegistry registry) {
        // Override to register gamemode-specific commands
    }

    /**
     * Called when this gamemode is registered.
     * Override for initialization logic.
     */
    default void onRegister() {
        // Override for custom initialization
    }

    /**
     * Called when the server is shutting down.
     * Override for cleanup logic.
     */
    default void onShutdown() {
        // Override for custom cleanup
    }

    /**
     * Checks if this gamemode is currently enabled.
     *
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Checks if players can currently queue for this gamemode.
     *
     * @return true if queueing is allowed
     */
    default boolean canQueue() {
        return isEnabled();
    }
}
