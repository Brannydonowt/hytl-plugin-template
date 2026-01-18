package dev.branny.hytale.servercore.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Centralized service for transferring players between worlds.
 * Handles all the complexity of world transfers including:
 * - Thread safety
 * - Session state management
 * - Title/subtitle display
 * - Error handling
 */
public final class WorldTransferService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private WorldTransferService() {
        // Utility class
    }

    // ==================== Core Transfer Methods ====================

    /**
     * Transfers a player to a target world with full options.
     *
     * @param playerRef the player to transfer
     * @param worldName the target world name
     * @param transform the spawn transform (null for world default)
     * @param fadeInOut whether to show fade transition
     * @param onComplete callback after transfer completes (may be null)
     * @return CompletableFuture that completes when transfer is done
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transfer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform,
            boolean fadeInOut,
            @Nullable Consumer<PlayerRef> onComplete) {

        // Update session state
        PlayerSession session = PlayerSession.get(playerRef.getUuid());
        if (session != null) {
            session.setTransferring(true);
        }

        // Early connection check
        if (!playerRef.getPacketHandler().stillActive()) {
            if (session != null) {
                session.setTransferring(false);
            }
            return CompletableFuture.failedFuture(
                new IllegalStateException("Player connection is no longer active"));
        }

        // Get current world safely
        World currentWorld = getCurrentWorld(playerRef);

        return ensureWorldLoaded(worldName)
            .thenCompose(targetWorld -> {
                // Already in target world?
                if (targetWorld.equals(currentWorld)) {
                    if (session != null) {
                        session.setTransferring(false);
                    }
                    return CompletableFuture.completedFuture(playerRef);
                }

                CompletableFuture<PlayerRef> result = new CompletableFuture<>();

                if (currentWorld != null) {
                    // Remove from current world on its thread, then add to target
                    currentWorld.execute(() -> {
                        try {
                            playerRef.removeFromStore();
                            
                            CompletableFuture<PlayerRef> addFuture = 
                                targetWorld.addPlayer(playerRef, transform, true, fadeInOut);
                            
                            if (addFuture == null) {
                                result.completeExceptionally(
                                    new IllegalStateException("Failed to add player - connection may have closed"));
                                return;
                            }
                            
                            addFuture.whenComplete((transferred, error) -> {
                                if (error != null) {
                                    result.completeExceptionally(error);
                                } else {
                                    result.complete(transferred);
                                }
                            });
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }
                    });
                } else {
                    // Player not in any world, add directly
                    CompletableFuture<PlayerRef> addFuture = 
                        targetWorld.addPlayer(playerRef, transform, true, fadeInOut);
                    
                    if (addFuture == null) {
                        return CompletableFuture.failedFuture(
                            new IllegalStateException("Failed to add player - connection may have closed"));
                    }
                    
                    addFuture.whenComplete((transferred, error) -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                        } else {
                            result.complete(transferred);
                        }
                    });
                }

                return result;
            })
            .whenComplete((transferred, error) -> {
                if (session != null) {
                    session.setTransferring(false);
                }
                
                if (error != null) {
                    LOGGER.atWarning().log("Transfer failed for " + playerRef.getUsername() + ": " + error.getMessage());
                } else if (onComplete != null && transferred != null) {
                    onComplete.accept(transferred);
                }
            });
    }

    /**
     * Simple transfer to a world with default options.
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transfer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName) {
        return transfer(playerRef, worldName, null, true, null);
    }

    /**
     * Transfer with spawn point.
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transfer(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform) {
        return transfer(playerRef, worldName, transform, true, null);
    }

    // ==================== Transfer with Title Display ====================

    /**
     * Transfers a player and shows an EventTitle upon arrival.
     *
     * @param playerRef the player to transfer
     * @param worldName the target world name
     * @param transform the spawn transform (null for world default)
     * @param title the title to display
     * @param subtitle the subtitle to display
     * @return CompletableFuture that completes when transfer is done
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferWithTitle(
            @Nonnull PlayerRef playerRef,
            @Nonnull String worldName,
            @Nullable Transform transform,
            @Nonnull String title,
            @Nonnull String subtitle) {

        return transfer(playerRef, worldName, transform, true, transferred -> {
            if (transferred != null) {
                World world = getCurrentWorld(transferred);
                if (world != null) {
                    world.execute(() -> {
                        EventTitleUtil.showEventTitleToPlayer(
                            transferred,
                            Message.raw(title),
                            Message.raw(subtitle),
                            true
                        );
                    });
                }
            }
        });
    }

    // ==================== Teleport (Same World) ====================

    /**
     * Teleports a player within their current world.
     * Must be called from the world thread or will be scheduled.
     *
     * @param playerRef the player to teleport
     * @param transform the target position
     * @return CompletableFuture that completes when teleport is done
     */
    @Nonnull
    public static CompletableFuture<Void> teleport(
            @Nonnull PlayerRef playerRef,
            @Nonnull Transform transform) {

        World world = getCurrentWorld(playerRef);
        if (world == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Player is not in a world"));
        }

        CompletableFuture<Void> result = new CompletableFuture<>();

        world.execute(() -> {
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        player.getTransformComponent().teleportPosition(transform.getPosition());
                        result.complete(null);
                        return;
                    }
                }
                result.completeExceptionally(new IllegalStateException("Player entity not found"));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the world a player is currently in (thread-safe).
     *
     * @param playerRef the player
     * @return the world, or null if not in any world
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
     * Ensures a world is loaded and returns it.
     *
     * @param worldName the world name
     * @return CompletableFuture with the world
     */
    @Nonnull
    public static CompletableFuture<World> ensureWorldLoaded(@Nonnull String worldName) {
        Universe universe = Universe.get();

        World existingWorld = universe.getWorld(worldName);
        if (existingWorld != null) {
            return CompletableFuture.completedFuture(existingWorld);
        }

        if (universe.isWorldLoadable(worldName)) {
            return universe.loadWorld(worldName);
        }

        return CompletableFuture.failedFuture(
            new IllegalStateException("World '" + worldName + "' does not exist or cannot be loaded"));
    }

    /**
     * Checks if a player is in a specific world.
     *
     * @param playerRef the player
     * @param worldName the world name to check
     * @return true if the player is in that world
     */
    public static boolean isInWorld(@Nonnull PlayerRef playerRef, @Nonnull String worldName) {
        World currentWorld = getCurrentWorld(playerRef);
        return currentWorld != null && worldName.equals(currentWorld.getName());
    }
}
