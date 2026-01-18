package dev.branny.hytale.servercore.persistence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.loadout.LoadoutItem;
import dev.branny.hytale.pvptools.loadout.LoadoutService;
import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages per-gamemode inventory saving and restoring.
 * 
 * This service handles the inventory lifecycle when players switch between gamemodes:
 * - Saves current inventory to the previous gamemode's persistent data
 * - Restores saved inventory when returning to a gamemode
 * - Clears inventory for gamemodes that use loadouts
 */
public final class GamemodeInventoryManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GamemodeInventoryManager() {
        // Utility class
    }

    // ==================== Core Operations ====================

    /**
     * Saves the player's current inventory to the specified gamemode's persistent data.
     * Call this before switching to a different gamemode to preserve progress.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to save inventory for
     * @return true if save was successful
     */
    public static boolean saveCurrentInventory(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            LOGGER.atWarning().log("Cannot save inventory - player not in world: " + playerRef.getUsername());
            return false;
        }
        return saveCurrentInventory(playerRef, gamemodeId, ref);
    }

    /**
     * Saves the player's current inventory using a pre-captured Ref.
     * Use this overload when you've already validated the Ref to avoid race conditions.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to save inventory for
     * @param ref the pre-captured entity reference (must be valid)
     * @return true if save was successful
     */
    public static boolean saveCurrentInventory(
            @Nonnull PlayerRef playerRef, 
            @Nonnull String gamemodeId,
            @Nonnull Ref<EntityStore> ref) {
        
        if (!ref.isValid()) {
            LOGGER.atWarning().log("Cannot save inventory - ref is no longer valid: " + playerRef.getUsername());
            return false;
        }

        // Get component from cache (most reliable) with ref as fallback
        PlayerDataComponent component = PlayerDataService.getFromRef(ref, playerRef.getUuid());
        if (component == null) {
            LOGGER.atWarning().log("Cannot save inventory - no component for player: " + playerRef.getUsername());
            return false;
        }
        GamemodePlayerData gamemodeData = component.getGamemodeData(gamemodeId);

        try {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) {
                LOGGER.atWarning().log("Cannot save inventory - Player entity not found (component exists) for: " + playerRef.getUsername());
                return false;
            }

            // Capture current inventory as a loadout (hotbar/armor/utility)
            Loadout capturedLoadout = LoadoutService.captureCurrentLoadout(player, gamemodeId);
            
            // Also capture storage items
            List<LoadoutItem> storageItems = captureStorageItems(player);

            // Save to gamemode data
            gamemodeData.saveInventory(capturedLoadout, storageItems);
            gamemodeData.updateLastPlayed();
            component.markGamemodeDataChanged();

            LOGGER.atInfo().log("Saved inventory for " + playerRef.getUsername() + " in gamemode: " + gamemodeId);
            return true;

        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to save inventory for " + playerRef.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves the player's current inventory asynchronously.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to save inventory for
     * @return CompletableFuture that completes when save is done
     */
    @Nonnull
    public static CompletableFuture<Boolean> saveCurrentInventoryAsync(
            @Nonnull PlayerRef playerRef,
            @Nonnull String gamemodeId) {

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> result.complete(saveCurrentInventory(playerRef, gamemodeId)));
        return result;
    }

    /**
     * Restores a player's saved inventory from the specified gamemode's persistent data.
     * Call this when a player joins a gamemode that has persistent inventory.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to restore inventory from
     * @return true if restore was successful (false if no saved data exists)
     */
    public static boolean restoreInventory(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            LOGGER.atWarning().log("Cannot restore inventory - player not in world: " + playerRef.getUsername());
            return false;
        }

        // Get gamemode data (don't create if not present)
        GamemodePlayerData gamemodeData = PlayerDataService.getGamemodeDataIfPresent(playerRef, gamemodeId);
        if (gamemodeData == null || !gamemodeData.hasInventory()) {
            LOGGER.atInfo().log("No saved inventory to restore for " + playerRef.getUsername() + " in gamemode: " + gamemodeId);
            return false;
        }

        try {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) {
                return false;
            }

            // Clear current inventory first
            player.getInventory().clear();

            // Restore hotbar/armor/utility from saved loadout
            Loadout savedLoadout = gamemodeData.toLoadout(gamemodeId);
            if (savedLoadout != null) {
                LoadoutService.applyLoadout(player, savedLoadout);
            }

            // Restore storage items
            restoreStorageItems(player, gamemodeData.getStorageItems());

            gamemodeData.updateLastPlayed();
            PlayerDataComponent component = PlayerDataService.get(playerRef);
            if (component != null) {
                component.markGamemodeDataChanged();
            }
            LOGGER.atInfo().log("Restored inventory for " + playerRef.getUsername() + " from gamemode: " + gamemodeId);
            return true;

        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to restore inventory for " + playerRef.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Restores a player's saved inventory asynchronously.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to restore inventory from
     * @return CompletableFuture that completes when restore is done
     */
    @Nonnull
    public static CompletableFuture<Boolean> restoreInventoryAsync(
            @Nonnull PlayerRef playerRef,
            @Nonnull String gamemodeId) {

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> result.complete(restoreInventory(playerRef, gamemodeId)));
        return result;
    }

    /**
     * Clears a player's inventory.
     *
     * @param playerRef the player reference
     * @return true if clear was successful
     */
    public static boolean clearInventory(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        try {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getInventory().clear();
                LOGGER.atInfo().log("Cleared inventory for " + playerRef.getUsername());
                return true;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to clear inventory: " + e.getMessage());
        }
        return false;
    }

    /**
     * Clears a player's inventory asynchronously.
     *
     * @param playerRef the player reference
     * @return CompletableFuture that completes when clear is done
     */
    @Nonnull
    public static CompletableFuture<Void> clearInventoryAsync(@Nonnull PlayerRef playerRef) {
        return LoadoutService.clearLoadoutAsync(playerRef);
    }

    // ==================== Gamemode Transition Helpers ====================

    /**
     * Prepares a player for entering a new gamemode.
     * Handles saving previous inventory, clearing, and optionally applying loadout/restoring.
     *
     * @param playerRef the player reference
     * @param newGamemode the gamemode being entered
     * @param previousGamemodeId the gamemode being left (null if from lobby)
     * @return true if preparation was successful
     */
    public static boolean prepareForGamemode(
            @Nonnull PlayerRef playerRef,
            @Nonnull Gamemode newGamemode,
            @Nullable String previousGamemodeId) {

        // Step 1: Save current inventory to previous gamemode if applicable
        if (previousGamemodeId != null) {
            PlayerSession session = PlayerSession.get(playerRef.getUuid());
            if (session != null && session.getCurrentGamemode() != null) {
                saveCurrentInventory(playerRef, previousGamemodeId);
            }
        }

        // Step 2: Clear current inventory
        clearInventory(playerRef);

        // Step 3: Set up inventory for new gamemode
        Loadout defaultLoadout = newGamemode.getDefaultLoadout();
        if (defaultLoadout != null) {
            // PvP mode: Apply the gamemode's loadout
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                if (player != null) {
                    LoadoutService.applyLoadout(player, defaultLoadout);
                }
            }
        } else if (newGamemode.hasPersistentInventory()) {
            // Persistent mode: Restore saved inventory
            restoreInventory(playerRef, newGamemode.getId());
        }
        // Otherwise: Leave inventory empty (e.g., lobby)

        LOGGER.atInfo().log("Prepared " + playerRef.getUsername() + " for gamemode: " + newGamemode.getId());
        return true;
    }

    /**
     * Prepares a player for entering a new gamemode asynchronously.
     *
     * @param playerRef the player reference
     * @param newGamemode the gamemode being entered
     * @param previousGamemodeId the gamemode being left (null if from lobby)
     * @return CompletableFuture that completes when preparation is done
     */
    @Nonnull
    public static CompletableFuture<Boolean> prepareForGamemodeAsync(
            @Nonnull PlayerRef playerRef,
            @Nonnull Gamemode newGamemode,
            @Nullable String previousGamemodeId) {

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> result.complete(prepareForGamemode(playerRef, newGamemode, previousGamemodeId)));
        return result;
    }

    /**
     * Prepares a player for returning to the lobby.
     * Saves their current gamemode inventory and clears it.
     *
     * @param playerRef the player reference
     * @param currentGamemodeId the gamemode being left
     * @return true if preparation was successful
     */
    public static boolean prepareForLobby(@Nonnull PlayerRef playerRef, @Nullable String currentGamemodeId) {
        // Save current inventory if leaving a persistent gamemode
        if (currentGamemodeId != null) {
            saveCurrentInventory(playerRef, currentGamemodeId);
        }

        // Clear inventory for lobby
        clearInventory(playerRef);

        LOGGER.atInfo().log("Prepared " + playerRef.getUsername() + " for lobby return");
        return true;
    }

    /**
     * Prepares a player for returning to the lobby asynchronously.
     *
     * @param playerRef the player reference
     * @param currentGamemodeId the gamemode being left
     * @return CompletableFuture that completes when preparation is done
     */
    @Nonnull
    public static CompletableFuture<Boolean> prepareForLobbyAsync(
            @Nonnull PlayerRef playerRef,
            @Nullable String currentGamemodeId) {

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        world.execute(() -> result.complete(prepareForLobby(playerRef, currentGamemodeId)));
        return result;
    }

    // ==================== Storage Item Helpers ====================

    /**
     * Captures storage items from a player's inventory.
     */
    @Nonnull
    private static List<LoadoutItem> captureStorageItems(@Nonnull Player player) {
        List<LoadoutItem> items = new ArrayList<>();
        Inventory inventory = player.getInventory();
        ItemContainer storage = inventory.getStorage();

        for (int i = 0; i < storage.getCapacity(); i++) {
            ItemStack stack = storage.getItemStack((short) i);
            if (stack != null && !stack.isEmpty()) {
                items.add(new LoadoutItem(stack.getItemId(), stack.getQuantity(), i));
            }
        }

        return items;
    }

    /**
     * Restores storage items to a player's inventory.
     */
    private static void restoreStorageItems(@Nonnull Player player, @Nonnull List<LoadoutItem> items) {
        if (items.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemContainer storage = inventory.getStorage();

        for (LoadoutItem item : items) {
            if (!item.isValid()) continue;
            try {
                ItemStack stack = new ItemStack(item.itemId(), item.quantity());
                if (item.slot() >= 0 && item.slot() < storage.getCapacity()) {
                    storage.setItemStackForSlot((short) item.slot(), stack);
                } else {
                    storage.addItemStack(stack);
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to restore storage item '" + item.itemId() + "': " + e.getMessage());
            }
        }
    }

    // ==================== Query Methods ====================

    /**
     * Checks if a player has saved inventory data for a gamemode.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode to check
     * @return true if saved inventory exists
     */
    public static boolean hasSavedInventory(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        GamemodePlayerData data = PlayerDataService.getGamemodeDataIfPresent(playerRef, gamemodeId);
        return data != null && data.hasInventory();
    }
}
