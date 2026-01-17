package dev.branny.hytale.utilities;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility functions for working with worlds.
 * Loading players into different worlds, etc.
 */
public final class WorldUtilities {

    // ==================== Constants ====================

    /**
     * The name of the lobby world folder under run/universe/worlds/
     */
    public static final String LOBBY_WORLD_NAME = "lobby_world";
    
    /**
     * The name of the default world folder under run/universe/worlds/
     */
    public static final String SURVIVAL_WORLD_NAME = "default";
    
    /**
     * Lobby world spawn coordinates.
     */
    public static final double LOBBY_SPAWN_X = 2.5;
    public static final double LOBBY_SPAWN_Y = 108.0;
    public static final double LOBBY_SPAWN_Z = 0.0;

    /**
     * Survival world spawn coordinates.
     */
    public static final double SURVIVAL_SPAWN_X = 243;
    public static final double SURVIVAL_SPAWN_Y = 122.0;
    public static final double SURVIVAL_SPAWN_Z = 167.0;

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
     * When players are transferred into a world via transferPlayerToWorld(), their gamemode is set accordingly.
     *
     * Default is Adventure if not registered.
     */
    private static final Map<String, GameMode> WORLD_GAME_MODES = new ConcurrentHashMap<>();

    static {
        // Register default subtitles (should match config.json Plugin.Subtitle)
        WORLD_SUBTITLES.put(LOBBY_WORLD_NAME, "Prepare for battle!");
        WORLD_SUBTITLES.put(SURVIVAL_WORLD_NAME, "Explore the wilderness");

        // Register default spawn coordinates
        WORLD_SPAWNS.put(LOBBY_WORLD_NAME, new Transform(
            LOBBY_SPAWN_X, LOBBY_SPAWN_Y, LOBBY_SPAWN_Z, 0.0f, 0.0f, 0.0f));
        WORLD_SPAWNS.put(SURVIVAL_WORLD_NAME, new Transform(
            SURVIVAL_SPAWN_X, SURVIVAL_SPAWN_Y, SURVIVAL_SPAWN_Z, 0.0f, 0.0f, 0.0f));

        // Register default gamemodes (Adventure = 0)
        WORLD_GAME_MODES.put(LOBBY_WORLD_NAME, GameMode.Adventure);
        WORLD_GAME_MODES.put(SURVIVAL_WORLD_NAME, GameMode.Adventure);
    }

    private WorldUtilities() {
        // Utility class; do not instantiate.
    }

    // ==================== World Configuration Management ====================

    /**
     * Registers a subtitle for a world. This subtitle is shown to players
     * when they enter the world via transferPlayerToWorld().
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

    /**
     * Registers a spawn location for a world. This is where players will spawn
     * when transferred to the world via transferPlayerToWorld().
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
     * @return the spawn transform, or null if not registered (will use world's spawn provider)
     */
    @Nullable
    public static Transform getWorldSpawn(@Nonnull String worldName) {
        return WORLD_SPAWNS.get(worldName.toLowerCase());
    }

    // ==================== World GameMode Management ====================

    /**
     * Registers a gamemode for a world. When players are transferred into this world via
     * transferPlayerToWorld(), their gamemode will be updated.
     *
     * If a world has no registered gamemode, Adventure is used by default.
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

    // ==================== World Resolution ====================

    /**
     * Ensures the specified world is loaded and returns it.
     * If the world is already loaded, returns it immediately.
     * If the world exists on disk but isn't loaded, loads it.
     * If the world doesn't exist, returns a failed future.
     *
     * @param worldName the name of the world to ensure is loaded
     * @return a CompletableFuture that resolves to the loaded World
     */
    @Nonnull
    public static CompletableFuture<World> ensureWorldLoaded(@Nonnull String worldName) {
        Universe universe = Universe.get();

        // Check if world is already loaded
        World existingWorld = universe.getWorld(worldName);
        if (existingWorld != null) {
            return CompletableFuture.completedFuture(existingWorld);
        }

        // Check if world exists on disk and can be loaded
        if (universe.isWorldLoadable(worldName)) {
            return universe.loadWorld(worldName);
        }

        // World doesn't exist or can't be loaded
        return CompletableFuture.failedFuture(
            new IllegalStateException("World '" + worldName + "' does not exist or is not loadable. " +
                "Ensure the world folder exists under run/universe/worlds/" + worldName + " with a valid config.bson or config.json")
        );
    }

    /**
     * Gets a world by name if it's already loaded, or null if not.
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

    // ==================== Player Transfer ====================

    /**
     * Gets the world a player is currently in, without reading any components off-thread.
     * This is safe to call from any thread.
     *
     * @param playerRef the player to check
     * @return the World the player is in, or null if not in any world
     */
    @Nullable
    public static World getCurrentWorld(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return ((EntityStore) ref.getStore().getExternalData()).getWorld();
    }

    /**
     * Transfers a player to a different world while keeping their in-memory state
     * (inventory, avatar, etc.). This is the recommended way to move players between worlds.
     *
     * This method is thread-safe and handles all threading internally:
     * - Removes the player from their current world on that world's thread
     * - Adds the player to the target world with proper client handshake
     *
     * IMPORTANT: Do NOT read player components (playerRef.getComponent(...), 
     * playerRef.getTransform(), etc.) before calling this method from a command thread.
     * Pass the transform directly or use null for world spawn.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world
     * @param transform the spawn transform in the target world, or null to use world spawn
     * @param fadeInOut whether to show fade transition on client (false for immediate transfers)
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform,
            boolean fadeInOut) {

        // Early check: if connection is dead, don't bother
        if (!playerRef.getPacketHandler().stillActive()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Player connection is no longer active"));
        }

        // Get current world using thread-safe method (no component reads)
        World currentWorld = getCurrentWorld(playerRef);

        return ensureWorldLoaded(worldName)
            .thenCompose(targetWorld -> {
                // Check if already in target world
                if (targetWorld.equals(currentWorld)) {
                    return CompletableFuture.completedFuture(playerRef);
                }

                CompletableFuture<PlayerRef> result = new CompletableFuture<>();

                if (currentWorld != null) {
                    // Remove from current world on its thread, then add to target
                    currentWorld.execute(() -> {
                        try {
                            // Remove player from current world store
                            playerRef.removeFromStore();
                            
                            // Add to target world with clean client state
                            // clearWorld=true ensures client clears old world data
                            CompletableFuture<PlayerRef> addFuture = 
                                targetWorld.addPlayer(playerRef, transform, true, fadeInOut);
                            
                            if (addFuture == null) {
                                result.completeExceptionally(
                                    new IllegalStateException("Failed to add player to target world - connection may have closed"));
                                return;
                            }
                            
                            addFuture.whenComplete((transferredPlayer, error) -> {
                                if (error != null) {
                                    result.completeExceptionally(error);
                                } else {
                                    result.complete(transferredPlayer);
                                }
                            });
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }
                    });
                } else {
                    // Player not in any world, add directly to target
                    CompletableFuture<PlayerRef> addFuture = 
                        targetWorld.addPlayer(playerRef, transform, true, fadeInOut);
                    
                    if (addFuture == null) {
                        return CompletableFuture.failedFuture(
                            new IllegalStateException("Failed to add player to target world - connection may have closed"));
                    }
                    
                    addFuture.whenComplete((transferredPlayer, error) -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                        } else {
                            result.complete(transferredPlayer);
                        }
                    });
                }

                return result;
            })
            .thenApply(transferredPlayer -> {
                // If we actually moved worlds, update gamemode based on the target world (default Adventure).
                if (transferredPlayer != null) {
                    World world = getCurrentWorld(transferredPlayer);
                    if (world != null) {
                        world.execute(() -> applyWorldGameMode(transferredPlayer, worldName, world));
                    }
                }
                return transferredPlayer;
            });
    }

    /**
     * Transfers a player to a different world with fade transition enabled.
     * This is the default behavior for most transfers.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world
     * @param transform the spawn transform in the target world, or null to use world spawn
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform) {
        return transferPlayer(playerRef, worldName, transform, true);
    }

    /**
     * Transfers a player to a different world at the world's default spawn.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName) {
        return transferPlayer(playerRef, worldName, null);
    }

    /**
     * Transfers a player to the lobby world at the configured spawn coordinates.
     * Uses a default rotation (facing north) since we cannot safely read the player's
     * current rotation off-thread.
     *
     * @param playerRef the player to transfer
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToLobby(@Nonnull PlayerRef playerRef) {
        Transform lobbySpawn = new Transform(
            LOBBY_SPAWN_X, LOBBY_SPAWN_Y, LOBBY_SPAWN_Z,
            0.0f, 0.0f, 0.0f
        );
        
        return transferPlayer(playerRef, LOBBY_WORLD_NAME, lobbySpawn);
    }
    
    /**
     * Transfers a player to the default world using the world's spawn provider.
     *
     * @param playerRef the player to transfer
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToDefault(@Nonnull PlayerRef playerRef) {
        // Pass null transform to use the world's spawn provider
        return transferPlayer(playerRef, SURVIVAL_WORLD_NAME, new Transform(SURVIVAL_SPAWN_X, SURVIVAL_SPAWN_Y, SURVIVAL_SPAWN_Z, 
            0.0f, 0.0f, 0.0f));
    }

    // ==================== Transfer with EventTitle ====================

    /**
     * Transfers a player to any world by name and shows an EventTitle upon arrival.
     * Uses the registered spawn coordinates for the world (if any), otherwise falls back
     * to the world's spawn provider.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world folder
     * @param fadeInOut whether to show fade transition (false for immediate transfers after join)
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToWorld(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            boolean fadeInOut) {
        Transform spawn = getWorldSpawn(worldName);
        return transferPlayerToWorld(playerRef, worldName, spawn, fadeInOut);
    }

    /**
     * Transfers a player to any world by name and shows an EventTitle upon arrival.
     * Uses fade transition by default.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world folder
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToWorld(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName) {
        return transferPlayerToWorld(playerRef, worldName, true);
    }

    /**
     * Transfers a player to any world by name with a specific spawn transform,
     * and shows an EventTitle upon arrival.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world folder
     * @param transform the spawn transform, or null to use world's spawn provider
     * @param fadeInOut whether to show fade transition (false for immediate transfers after join)
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToWorld(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform,
            boolean fadeInOut) {

        return transferPlayer(playerRef, worldName, transform, fadeInOut)
            .thenApply(transferredPlayer -> {
                if (transferredPlayer != null) {
                    // Show entry title on the world thread
                    World world = getCurrentWorld(transferredPlayer);
                    if (world != null) {
                        world.execute(() -> showWorldEntryTitle(transferredPlayer, worldName));
                    }
                }
                return transferredPlayer;
            });
    }

    /**
     * Transfers a player to any world by name with a specific spawn transform,
     * and shows an EventTitle upon arrival. Uses fade transition by default.
     *
     * @param playerRef the player to transfer
     * @param worldName the name of the target world folder
     * @param transform the spawn transform, or null to use world's spawn provider
     * @return a CompletableFuture that resolves to the PlayerRef after transfer
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferPlayerToWorld(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform) {
        return transferPlayerToWorld(playerRef, worldName, transform, true);
    }

    /**
     * Shows the world entry EventTitle to a player.
     * Uses the world's DisplayName as the title and the registered subtitle.
     *
     * @param playerRef the player to show the title to
     * @param worldName the world folder name (used to look up subtitle)
     */
    public static void showWorldEntryTitle(@Nonnull PlayerRef playerRef, @Nonnull String worldName) {
        World world = getCurrentWorld(playerRef);
        if (world == null) {
            return;
        }

        // Get display name from WorldConfig, fallback to formatted world name
        WorldConfig config = world.getWorldConfig();
        String displayName = config.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = WorldConfig.formatDisplayName(worldName);
        }

        // Get subtitle from our registry
        String subtitle = getWorldSubtitle(worldName);

        // Show the EventTitle
        EventTitleUtil.showEventTitleToPlayer(
            playerRef,
            Message.raw(displayName),
            Message.raw(subtitle),
            true  // isMajor = true for world entry
        );
    }

    /**
     * Updates a player's gamemode based on the target world name.
     * Must be called on the world's thread.
     */
    private static void applyWorldGameMode(@Nonnull PlayerRef playerRef, @Nonnull String worldName, @Nonnull World world) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        GameMode desired = getWorldGameMode(worldName);
        ComponentAccessor<EntityStore> accessor = EntityStoreUtilities.getAccessor(world);
        Player.setGameMode(ref, desired, accessor);
    }
}
