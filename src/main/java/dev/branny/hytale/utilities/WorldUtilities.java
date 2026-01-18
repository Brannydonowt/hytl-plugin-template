package dev.branny.hytale.utilities;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import dev.branny.hytale.servercore.lobby.LobbyConfig;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration and registry for world settings.
 * 
 * This class manages world-specific configuration such as:
 * - Spawn coordinates per world
 * - Subtitles shown on world entry
 * - GameMode settings per world
 * 
 * For player transfers between worlds, use {@link WorldTransferService}.
 */
public final class WorldUtilities {

    // ==================== World Name Constants ====================
    
    /**
     * The name of the lobby world folder under run/universe/worlds/
     * @see LobbyConfig#LOBBY_WORLD_NAME
     */
    public static final String LOBBY_WORLD_NAME = LobbyConfig.LOBBY_WORLD_NAME;
    
    /**
     * The name of the default/survival world folder under run/universe/worlds/
     */
    public static final String SURVIVAL_WORLD_NAME = "default";
    
    /**
     * The name of the arena world folder under run/universe/worlds/
     * Currently uses the lobby world for arena battles.
     */
    public static final String ARENA_WORLD_NAME = "lobby_world";

    // ==================== Survival Spawn Coordinates ====================

    /**
     * Survival world spawn coordinates.
     */
    public static final double SURVIVAL_SPAWN_X = 243;
    public static final double SURVIVAL_SPAWN_Y = 122.0;
    public static final double SURVIVAL_SPAWN_Z = 167.0;

    // ==================== Arena Spawn Coordinates ====================
    
    /**
     * Arena Player 1 spawn coordinates.
     */
    public static final double ARENA_P1_SPAWN_X = 10.0;
    public static final double ARENA_P1_SPAWN_Y = 108.0;
    public static final double ARENA_P1_SPAWN_Z = 10.0;

    /**
     * Arena Player 2 spawn coordinates.
     */
    public static final double ARENA_P2_SPAWN_X = -10.0;
    public static final double ARENA_P2_SPAWN_Y = 108.0;
    public static final double ARENA_P2_SPAWN_Z = -10.0;

    // ==================== Registries ====================

    /**
     * World subtitle mappings (world folder name -> subtitle text).
     * These are shown to players when they enter a world.
     */
    private static final Map<String, String> WORLD_SUBTITLES = new ConcurrentHashMap<>();

    /**
     * World spawn coordinate mappings (world folder name -> spawn transform).
     * These define where players spawn when transferred to a world.
     */
    private static final Map<String, Transform> WORLD_SPAWNS = new ConcurrentHashMap<>();

    /**
     * World gamemode mappings (world folder name -> gamemode).
     * When players are transferred into a world, their gamemode is set accordingly.
     * Default is Adventure if not registered.
     */
    private static final Map<String, GameMode> WORLD_GAME_MODES = new ConcurrentHashMap<>();

    static {
        // Register default subtitles
        WORLD_SUBTITLES.put(LOBBY_WORLD_NAME, "Prepare for battle!");
        WORLD_SUBTITLES.put(SURVIVAL_WORLD_NAME, "Explore the wilderness");

        // Register default spawn coordinates
        WORLD_SPAWNS.put(LOBBY_WORLD_NAME, LobbyConfig.getLobbySpawn());
        WORLD_SPAWNS.put(SURVIVAL_WORLD_NAME, new Transform(
            SURVIVAL_SPAWN_X, SURVIVAL_SPAWN_Y, SURVIVAL_SPAWN_Z, 0.0f, 0.0f, 0.0f));

        // Register default gamemodes
        WORLD_GAME_MODES.put(LOBBY_WORLD_NAME, GameMode.Adventure);
        WORLD_GAME_MODES.put(SURVIVAL_WORLD_NAME, GameMode.Adventure);
    }

    private WorldUtilities() {
        // Utility class; do not instantiate.
    }

    // ==================== World Subtitle Registry ====================

    /**
     * Registers a subtitle for a world. This subtitle is shown to players
     * when they enter the world.
     *
     * @param worldName the world folder name
     * @param subtitle the subtitle text to display
     */
    public static void registerWorldSubtitle(@Nonnull String worldName, @Nonnull String subtitle) {
        WORLD_SUBTITLES.put(worldName.toLowerCase(), subtitle);
    }

    /**
     * Gets the registered subtitle for a world.
     *
     * @param worldName the world folder name
     * @return the subtitle, or a default message if not registered
     */
    @Nonnull
    public static String getWorldSubtitle(@Nonnull String worldName) {
        String subtitle = WORLD_SUBTITLES.get(worldName.toLowerCase());
        return subtitle != null ? subtitle : "Welcome!";
    }

    // ==================== World Spawn Registry ====================

    /**
     * Registers a spawn location for a world.
     *
     * @param worldName the world folder name
     * @param spawn the spawn transform (position and rotation)
     */
    public static void registerWorldSpawn(@Nonnull String worldName, @Nonnull Transform spawn) {
        WORLD_SPAWNS.put(worldName.toLowerCase(), spawn);
    }

    /**
     * Registers a spawn location for a world using coordinates.
     *
     * @param worldName the world folder name
     * @param x spawn X coordinate
     * @param y spawn Y coordinate
     * @param z spawn Z coordinate
     */
    public static void registerWorldSpawn(@Nonnull String worldName, double x, double y, double z) {
        WORLD_SPAWNS.put(worldName.toLowerCase(), new Transform(x, y, z, 0.0f, 0.0f, 0.0f));
    }

    /**
     * Gets the registered spawn transform for a world.
     *
     * @param worldName the world folder name
     * @return the spawn transform, or null if not registered
     */
    @Nullable
    public static Transform getWorldSpawn(@Nonnull String worldName) {
        return WORLD_SPAWNS.get(worldName.toLowerCase());
    }

    // ==================== World GameMode Registry ====================

    /**
     * Registers a gamemode for a world.
     *
     * @param worldName the world folder name
     * @param gameMode the gamemode to apply on entry
     */
    public static void registerWorldGameMode(@Nonnull String worldName, @Nonnull GameMode gameMode) {
        WORLD_GAME_MODES.put(worldName.toLowerCase(), gameMode);
    }

    /**
     * Gets the gamemode configured for a world.
     *
     * @param worldName the world folder name
     * @return the configured gamemode, or Adventure if not registered
     */
    @Nonnull
    public static GameMode getWorldGameMode(@Nonnull String worldName) {
        GameMode mode = WORLD_GAME_MODES.get(worldName.toLowerCase());
        return mode != null ? mode : GameMode.Adventure;
    }

    // ==================== Arena Spawn Helpers ====================

    /**
     * Gets the arena spawn transform for Player 1.
     *
     * @return the spawn transform for arena player 1
     */
    @Nonnull
    public static Transform getArenaSpawnP1() {
        return new Transform(ARENA_P1_SPAWN_X, ARENA_P1_SPAWN_Y, ARENA_P1_SPAWN_Z, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Gets the arena spawn transform for Player 2.
     *
     * @return the spawn transform for arena player 2
     */
    @Nonnull
    public static Transform getArenaSpawnP2() {
        return new Transform(ARENA_P2_SPAWN_X, ARENA_P2_SPAWN_Y, ARENA_P2_SPAWN_Z, 0.0f, 180.0f, 0.0f);
    }

    /**
     * Checks if a player is currently in the arena world.
     *
     * @param playerRef the player to check
     * @return true if the player is in the arena world, false otherwise
     */
    public static boolean isInArenaWorld(@Nonnull PlayerRef playerRef) {
        World currentWorld = WorldTransferService.getCurrentWorld(playerRef);
        if (currentWorld == null) {
            return false;
        }
        return ARENA_WORLD_NAME.equals(currentWorld.getName());
    }

    // ==================== World Lookup Helpers ====================

    /**
     * Gets a world by name if it's already loaded.
     *
     * @param worldName the name of the world
     * @return the World if loaded, null otherwise
     */
    @Nullable
    public static World getWorld(@Nonnull String worldName) {
        return Universe.get().getWorld(worldName);
    }

    /**
     * Gets the lobby world if it's already loaded.
     *
     * @return the lobby World if loaded, null otherwise
     */
    @Nullable
    public static World getLobbyWorld() {
        return getWorld(LOBBY_WORLD_NAME);
    }

    /**
     * Gets the survival/default world if it's already loaded.
     *
     * @return the survival World if loaded, null otherwise
     */
    @Nullable
    public static World getSurvivalWorld() {
        return getWorld(SURVIVAL_WORLD_NAME);
    }
}
