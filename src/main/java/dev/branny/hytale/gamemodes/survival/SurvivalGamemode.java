package dev.branny.hytale.gamemodes.survival;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.match.Match;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Survival gamemode - Open world exploration and building.
 * 
 * Unlike PvP gamemodes, Survival is not match-based. Players join directly
 * and persist in the world. No queuing, no loadouts, no match lifecycle.
 */
public class SurvivalGamemode implements Gamemode {

    public static final String ID = "survival";
    public static final String DISPLAY_NAME = "Survival";
    public static final String DESCRIPTION = "Explore, gather resources, and survive in the open world!";

    @Override
    @Nonnull
    public String getId() {
        return ID;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    @Nonnull
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public int getMinPlayers() {
        return 1; // Single player allowed
    }

    @Override
    public int getMaxPlayers() {
        return 100; // No real limit for open world
    }

    @Override
    @Nonnull
    public String getWorldName() {
        return SurvivalConfig.WORLD_NAME;
    }

    /**
     * Survival does not use matches - returns null.
     * Players join the persistent world directly.
     */
    @Override
    @Nullable
    public Match createMatch(@Nonnull List<PlayerRef> players) {
        // Survival is not match-based - this should not be called
        return null;
    }

    /**
     * Survival does not apply loadouts - players keep their inventory.
     */
    @Override
    @Nullable
    public Loadout getDefaultLoadout() {
        return null;
    }

    @Override
    public void registerCommands(@Nonnull CommandRegistry registry) {
        registry.registerCommand(new SurvivalCommands());
    }

    /**
     * Survival uses Hytale's Adventure GameMode.
     * Adventure mode in Hytale includes survival mechanics like damage and resource gathering.
     * (Hytale only has Adventure and Creative modes - no separate Survival mode.)
     */
    @Override
    @Nonnull
    public GameMode getHytaleGameMode() {
        return GameMode.Adventure;
    }

    /**
     * Survival mode persists player inventory between sessions.
     * Players keep their gathered items when leaving and returning.
     */
    @Override
    public boolean hasPersistentInventory() {
        return true;
    }

    /**
     * Survival is always enabled.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Survival is NOT queueable - players join directly.
     * This prevents it from appearing in matchmaking queues.
     */
    @Override
    public boolean canQueue() {
        return false;
    }

    // ==================== Lifecycle Hooks ====================

    @Override
    public void onPlayerJoin(@Nonnull PlayerRef player) {
        // Could add survival-specific welcome logic here
        // For example: restore last position, send tips, etc.
    }

    @Override
    public void onPlayerLeave(@Nonnull PlayerRef player) {
        // Could add survival-specific cleanup here
        // For example: save position, log playtime, etc.
    }

    // ==================== Spawn Configuration ====================

    @Override
    @Nonnull
    public Transform getSpawnTransform() {
        return SurvivalConfig.getSpawn();
    }

    @Override
    @Nonnull
    public String getJoinTitle() {
        return DISPLAY_NAME;
    }

    @Override
    @Nonnull
    public String getJoinSubtitle() {
        return "Explore the wilderness!";
    }
}
