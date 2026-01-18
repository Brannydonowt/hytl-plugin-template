package dev.branny.hytale.servercore.gamemode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.gamemodes.GamemodeRegistry;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.servercore.lobby.LobbyConfig;
import dev.branny.hytale.servercore.lobby.LobbyManager;
import dev.branny.hytale.servercore.persistence.GamemodeInventoryManager;
import dev.branny.hytale.servercore.player.PlayerSession;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Centralized service for handling all gamemode transitions.
 * 
 * This service encapsulates the entire transition lifecycle:
 * - Validation (can player transition?)
 * - Session state management
 * - Inventory save/clear/restore
 * - World transfer
 * - Hytale GameMode application
 * - Lifecycle hooks
 * 
 * Commands should use this service instead of manually orchestrating transitions.
 */
public final class GamemodeTransitionService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GamemodeTransitionService() {
        // Utility class
    }

    // ==================== Public API ====================

    /**
     * Transitions a player to a gamemode.
     * Handles all state management: inventory save/restore, session updates, world transfer.
     *
     * @param playerRef the player to transition
     * @param gamemodeId the target gamemode ID
     * @return CompletableFuture with the transition result
     */
    @Nonnull
    public static CompletableFuture<TransitionResult> joinGamemode(
            @Nonnull PlayerRef playerRef,
            @Nonnull String gamemodeId) {

        // Validate the transition
        TransitionResult validation = validateTransition(playerRef, gamemodeId);
        if (!validation.success()) {
            return CompletableFuture.completedFuture(validation);
        }

        // Get target gamemode
        Gamemode targetGamemode = GamemodeRegistry.get(gamemodeId);
        if (targetGamemode == null) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Gamemode '" + gamemodeId + "' not found"));
        }

        if (!targetGamemode.isEnabled()) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Gamemode '" + targetGamemode.getDisplayName() + "' is not available"));
        }

        // Get session and current gamemode
        PlayerSession session = PlayerSession.getOrCreate(playerRef);
        String currentGamemodeId = session.getCurrentGamemode();
        Gamemode currentGamemode = currentGamemodeId != null ? GamemodeRegistry.get(currentGamemodeId) : null;

        // Leave queue if in one
        if (LobbyManager.isInQueue(playerRef.getUuid())) {
            LobbyManager.leaveQueue(playerRef.getUuid());
        }

        // Call leave hook on current gamemode
        if (currentGamemode != null) {
            currentGamemode.onPlayerLeave(playerRef);
        }

        // Mark as transitioning
        session.setTransferring(true);

        // Execute inventory preparation on world thread, then transfer
        return prepareInventoryForGamemode(playerRef, targetGamemode, currentGamemodeId)
            .thenCompose(prepared -> {
                if (!prepared) {
                    session.setTransferring(false);
                    return CompletableFuture.completedFuture(
                        TransitionResult.failure("Failed to prepare inventory", currentGamemodeId, gamemodeId));
                }

                // Transfer to target world
                Transform spawn = targetGamemode.getSpawnTransform();
                String title = targetGamemode.getJoinTitle();
                String subtitle = targetGamemode.getJoinSubtitle();

                return WorldTransferService.transferWithTitle(
                    playerRef,
                    targetGamemode.getWorldName(),
                    spawn,
                    title != null ? title : targetGamemode.getDisplayName(),
                    subtitle != null ? subtitle : ""
                ).thenCompose(transferred -> {
                    // After transfer, apply gamemode settings on the new world thread
                    return applyGamemodeSettings(playerRef, targetGamemode)
                        .thenApply(applied -> {
                            // Update session
                            session.setCurrentGamemode(gamemodeId);
                            session.setTransferring(false);

                            // Call join hook
                            targetGamemode.onPlayerJoin(playerRef);

                            LOGGER.atInfo().log("[Transition] " + playerRef.getUsername() + 
                                " successfully joined " + gamemodeId);

                            return TransitionResult.success(currentGamemodeId, gamemodeId);
                        });
                });
            })
            .exceptionally(error -> {
                LOGGER.atWarning().log("[Transition] Failed for " + playerRef.getUsername() + 
                    ": " + error.getMessage());
                session.setTransferring(false);
                return TransitionResult.failure("Transition failed: " + error.getMessage(), 
                    currentGamemodeId, gamemodeId);
            });
    }

    /**
     * Returns a player to the lobby from their current gamemode.
     * Saves inventory if in a persistent gamemode, clears inventory, transfers to lobby.
     *
     * @param playerRef the player to return to lobby
     * @return CompletableFuture with the transition result
     */
    @Nonnull
    public static CompletableFuture<TransitionResult> returnToLobby(@Nonnull PlayerRef playerRef) {

        // Basic validation
        if (!playerRef.getPacketHandler().stillActive()) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Player is no longer connected"));
        }

        // Check if in match
        if (MatchManager.isInMatch(playerRef.getUuid())) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Cannot return to lobby while in a match. Use forfeit to leave."));
        }

        // Get session and current gamemode
        PlayerSession session = PlayerSession.getOrCreate(playerRef);
        
        if (session.isTransferring()) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Already transitioning"));
        }

        String currentGamemodeId = session.getCurrentGamemode();
        Gamemode currentGamemode = currentGamemodeId != null ? GamemodeRegistry.get(currentGamemodeId) : null;

        // Already in lobby?
        if (session.isInLobby() && currentGamemodeId == null) {
            return CompletableFuture.completedFuture(
                TransitionResult.failure("Already in lobby"));
        }

        // Call leave hook on current gamemode
        if (currentGamemode != null) {
            currentGamemode.onPlayerLeave(playerRef);
        }

        // Mark as transitioning
        session.setTransferring(true);

        // Execute inventory preparation on world thread, then transfer
        return prepareInventoryForLobby(playerRef, currentGamemodeId)
            .thenCompose(prepared -> {
                // Transfer to lobby
                return WorldTransferService.transferWithTitle(
                    playerRef,
                    LobbyConfig.LOBBY_WORLD_NAME,
                    LobbyConfig.getLobbySpawn(),
                    "Lobby",
                    "Welcome back!"
                ).thenApply(transferred -> {
                    // Update session
                    session.clearCurrentGamemode();
                    session.setTransferring(false);

                    LOGGER.atInfo().log("[Transition] " + playerRef.getUsername() + 
                        " returned to lobby from " + currentGamemodeId);

                    return TransitionResult.success(currentGamemodeId, null);
                });
            })
            .exceptionally(error -> {
                LOGGER.atWarning().log("[Transition] Return to lobby failed for " + 
                    playerRef.getUsername() + ": " + error.getMessage());
                session.setTransferring(false);
                return TransitionResult.failure("Return to lobby failed: " + error.getMessage(), 
                    currentGamemodeId, null);
            });
    }

    /**
     * Checks if a player can currently transition to another gamemode.
     *
     * @param playerRef the player to check
     * @return true if the player can transition
     */
    public static boolean canTransition(@Nonnull PlayerRef playerRef) {
        if (!playerRef.getPacketHandler().stillActive()) {
            return false;
        }

        if (MatchManager.isInMatch(playerRef.getUuid())) {
            return false;
        }

        PlayerSession session = PlayerSession.get(playerRef.getUuid());
        if (session != null && session.isTransferring()) {
            return false;
        }

        return true;
    }

    // ==================== Internal Methods ====================

    /**
     * Validates that a transition to the target gamemode is allowed.
     */
    @Nonnull
    private static TransitionResult validateTransition(
            @Nonnull PlayerRef playerRef,
            @Nonnull String targetGamemodeId) {

        if (!playerRef.getPacketHandler().stillActive()) {
            return TransitionResult.failure("Player is no longer connected");
        }

        if (MatchManager.isInMatch(playerRef.getUuid())) {
            return TransitionResult.failure(
                "Cannot join gamemode while in a match. Use forfeit to leave first.");
        }

        PlayerSession session = PlayerSession.get(playerRef.getUuid());
        if (session != null && session.isTransferring()) {
            return TransitionResult.failure("Already transitioning between gamemodes");
        }

        // Check if already in target gamemode
        if (session != null && targetGamemodeId.equals(session.getCurrentGamemode())) {
            return TransitionResult.failure("Already in this gamemode");
        }

        return TransitionResult.success(null, null); // Validation passed
    }

    /**
     * Prepares inventory for entering a new gamemode (on world thread).
     */
    @Nonnull
    private static CompletableFuture<Boolean> prepareInventoryForGamemode(
            @Nonnull PlayerRef playerRef,
            @Nonnull Gamemode targetGamemode,
            @Nullable String previousGamemodeId) {

        // Capture the ref ONCE before scheduling - this prevents race conditions
        Ref<EntityStore> capturedRef = playerRef.getReference();
        if (capturedRef == null || !capturedRef.isValid()) {
            // Player not in a world yet, nothing to prepare
            return CompletableFuture.completedFuture(true);
        }

        World world = ((EntityStore) capturedRef.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> {
            try {
                // Save inventory from previous gamemode if applicable
                if (previousGamemodeId != null) {
                    Gamemode prevGamemode = GamemodeRegistry.get(previousGamemodeId);
                    if (prevGamemode != null && prevGamemode.hasPersistentInventory()) {
                        GamemodeInventoryManager.saveCurrentInventory(playerRef, previousGamemodeId, capturedRef);
                    }
                }

                // Clear inventory
                GamemodeInventoryManager.clearInventory(playerRef);

                result.complete(true);
            } catch (Exception e) {
                LOGGER.atWarning().log("[Transition] Inventory prep failed: " + e.getMessage());
                result.complete(false);
            }
        });
        return result;
    }

    /**
     * Prepares inventory for returning to lobby (on world thread).
     */
    @Nonnull
    private static CompletableFuture<Boolean> prepareInventoryForLobby(
            @Nonnull PlayerRef playerRef,
            @Nullable String currentGamemodeId) {

        // Capture the ref ONCE before scheduling - this prevents race conditions
        Ref<EntityStore> capturedRef = playerRef.getReference();
        if (capturedRef == null || !capturedRef.isValid()) {
            return CompletableFuture.completedFuture(true);
        }

        World world = ((EntityStore) capturedRef.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> {
            try {
                // Save inventory if leaving a persistent gamemode
                if (currentGamemodeId != null) {
                    Gamemode gamemode = GamemodeRegistry.get(currentGamemodeId);
                    if (gamemode != null && gamemode.hasPersistentInventory()) {
                        GamemodeInventoryManager.saveCurrentInventory(playerRef, currentGamemodeId, capturedRef);
                    }
                }

                // Clear inventory for lobby
                GamemodeInventoryManager.clearInventory(playerRef);

                result.complete(true);
            } catch (Exception e) {
                LOGGER.atWarning().log("[Transition] Lobby prep failed: " + e.getMessage());
                result.complete(false);
            }
        });
        return result;
    }

    /**
     * Applies gamemode settings after transfer (Hytale GameMode, restore inventory if applicable).
     */
    @Nonnull
    private static CompletableFuture<Boolean> applyGamemodeSettings(
            @Nonnull PlayerRef playerRef,
            @Nonnull Gamemode gamemode) {

        // Wait a bit for the player to fully load into the new world
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        // Schedule on a slight delay to ensure player is fully in the world
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100); // Small delay for world sync
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).thenRun(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                result.complete(true);
                return;
            }

            World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
            if (world == null) {
                result.complete(true);
                return;
            }

            world.execute(() -> {
                try {
                    // Apply Hytale GameMode
                    Ref<EntityStore> currentRef = playerRef.getReference();
                    if (currentRef != null && currentRef.isValid()) {
                        GameMode hytaleMode = gamemode.getHytaleGameMode();
                        if (hytaleMode != null) {
                            Player.setGameMode(currentRef, hytaleMode, currentRef.getStore());
                        }

                        // Restore inventory if persistent gamemode
                        if (gamemode.hasPersistentInventory()) {
                            GamemodeInventoryManager.restoreInventory(playerRef, gamemode.getId());
                        }
                    }

                    result.complete(true);
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Transition] Apply settings failed: " + e.getMessage());
                    result.complete(false);
                }
            });
        });

        return result;
    }
}
