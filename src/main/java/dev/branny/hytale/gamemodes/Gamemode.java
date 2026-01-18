package dev.branny.hytale.gamemodes;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
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
     * Gets the Hytale GameMode (Adventure/Creative) for this gamemode.
     * This determines player abilities like block breaking, flying, etc.
     * 
     * Available modes:
     * - Adventure (0): Standard mode with damage, survival mechanics
     * - Creative (1): Unlimited resources, flying, no damage
     * 
     * Default is Adventure mode, which works well for PvP minigames
     * where players should take damage but not access creative abilities.
     *
     * @return the Hytale GameMode to apply when players join this gamemode
     */
    @Nonnull
    default GameMode getHytaleGameMode() {
        return GameMode.Adventure;
    }

    /**
     * Checks if this gamemode persists player inventory between sessions.
     * When true, the inventory system will save/restore player inventory
     * when entering and leaving this gamemode.
     * 
     * Default is false (minigames typically use loadouts instead).
     * Override to return true for persistent modes like Survival.
     *
     * @return true if inventory should be persisted
     */
    default boolean hasPersistentInventory() {
        return false;
    }

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

    // ==================== Lifecycle Hooks ====================

    /**
     * Called after a player successfully joins this gamemode.
     * Override to perform custom logic when a player enters.
     *
     * @param player the player who joined
     */
    default void onPlayerJoin(@Nonnull PlayerRef player) {
        // Override for custom join logic
    }

    /**
     * Called before a player leaves this gamemode.
     * Override to perform custom cleanup when a player exits.
     *
     * @param player the player who is leaving
     */
    default void onPlayerLeave(@Nonnull PlayerRef player) {
        // Override for custom leave logic
    }

    // ==================== Spawn Configuration ====================

    /**
     * Gets the spawn location for this gamemode.
     * Override to specify a custom spawn point.
     *
     * @return the spawn transform, or null to use world default
     */
    @Nullable
    default Transform getSpawnTransform() {
        return null;
    }

    /**
     * Gets the title shown when a player joins this gamemode.
     * Displayed as an EventTitle after transfer.
     *
     * @return the join title, or null to use display name
     */
    @Nullable
    default String getJoinTitle() {
        return getDisplayName();
    }

    /**
     * Gets the subtitle shown when a player joins this gamemode.
     * Displayed below the title after transfer.
     *
     * @return the join subtitle, or null for no subtitle
     */
    @Nullable
    default String getJoinSubtitle() {
        return null;
    }
}
