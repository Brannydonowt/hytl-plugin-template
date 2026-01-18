package dev.branny.hytale.pvptools.loadout;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Service for applying and managing player loadouts.
 * Handles the actual inventory manipulation when applying/clearing loadouts.
 */
public final class LoadoutService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private LoadoutService() {
        // Utility class
    }

    // ==================== Apply Loadouts ====================

    /**
     * Applies a loadout to a player by ID.
     * Must be called from the world thread.
     *
     * @param player the player to apply to
     * @param loadoutId the loadout ID
     * @return true if applied successfully
     */
    public static boolean applyLoadout(@Nonnull Player player, @Nonnull String loadoutId) {
        Loadout loadout = LoadoutRegistry.get(loadoutId);
        if (loadout == null) {
            LOGGER.atWarning().log("Loadout not found: " + loadoutId);
            return false;
        }
        return applyLoadout(player, loadout);
    }

    /**
     * Applies a loadout to a player.
     * Must be called from the world thread.
     *
     * @param player the player to apply to
     * @param loadout the loadout to apply
     * @return true if applied successfully
     */
    public static boolean applyLoadout(@Nonnull Player player, @Nonnull Loadout loadout) {
        try {
            Inventory inventory = player.getInventory();

            // Clear existing inventory
            inventory.clear();

            // Apply hotbar items
            ItemContainer hotbar = inventory.getHotbar();
            for (LoadoutItem item : loadout.getHotbarItems()) {
                if (!item.isValid()) continue;
                try {
                    ItemStack stack = new ItemStack(item.itemId(), item.quantity());
                    hotbar.addItemStack(stack);
                } catch (Exception e) {
                    LOGGER.atWarning().log("Failed to add hotbar item '" + item.itemId() + "': " + e.getMessage());
                }
            }

            // Apply armor items
            ItemContainer armor = inventory.getArmor();
            for (LoadoutItem item : loadout.getArmorItems()) {
                if (!item.isValid()) continue;
                try {
                    ItemStack stack = new ItemStack(item.itemId(), item.quantity());
                    armor.addItemStack(stack);
                } catch (Exception e) {
                    LOGGER.atWarning().log("Failed to add armor item '" + item.itemId() + "': " + e.getMessage());
                }
            }

            // Apply utility items
            ItemContainer utility = inventory.getUtility();
            for (LoadoutItem item : loadout.getUtilityItems()) {
                if (!item.isValid()) continue;
                try {
                    ItemStack stack = new ItemStack(item.itemId(), item.quantity());
                    utility.addItemStack(stack);
                } catch (Exception e) {
                    LOGGER.atWarning().log("Failed to add utility item '" + item.itemId() + "': " + e.getMessage());
                }
            }

            LOGGER.atInfo().log("Applied loadout '" + loadout.getId() + "' to player");
            return true;

        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to apply loadout: " + e.getMessage());
            return false;
        }
    }

    /**
     * Applies a loadout to a player asynchronously.
     * Schedules the operation on the world thread.
     *
     * @param playerRef the player reference
     * @param loadoutId the loadout ID
     * @return CompletableFuture that completes when applied
     */
    @Nonnull
    public static CompletableFuture<Boolean> applyLoadoutAsync(
            @Nonnull PlayerRef playerRef, 
            @Nonnull String loadoutId) {
        
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        world.execute(() -> {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) {
                result.complete(applyLoadout(player, loadoutId));
            } else {
                result.complete(false);
            }
        });

        return result;
    }

    /**
     * Applies a loadout to a player asynchronously.
     *
     * @param playerRef the player reference
     * @param loadout the loadout to apply
     * @return CompletableFuture that completes when applied
     */
    @Nonnull
    public static CompletableFuture<Boolean> applyLoadoutAsync(
            @Nonnull PlayerRef playerRef, 
            @Nonnull Loadout loadout) {
        
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        world.execute(() -> {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) {
                result.complete(applyLoadout(player, loadout));
            } else {
                result.complete(false);
            }
        });

        return result;
    }

    // ==================== Clear Loadouts ====================

    /**
     * Clears a player's inventory.
     * Must be called from the world thread.
     *
     * @param player the player to clear
     */
    public static void clearLoadout(@Nonnull Player player) {
        player.getInventory().clear();
        LOGGER.atInfo().log("Cleared loadout from player");
    }

    /**
     * Clears a player's inventory asynchronously.
     *
     * @param playerRef the player reference
     * @return CompletableFuture that completes when cleared
     */
    @Nonnull
    public static CompletableFuture<Void> clearLoadoutAsync(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> result = new CompletableFuture<>();

        world.execute(() -> {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) {
                clearLoadout(player);
            }
            result.complete(null);
        });

        return result;
    }

    // ==================== Utility Methods ====================

    /**
     * Gets a player's current loadout as a Loadout object.
     * Useful for saving/restoring player states.
     *
     * @param player the player
     * @param id the ID for the new loadout
     * @return a Loadout representing current inventory
     */
    @Nonnull
    public static Loadout captureCurrentLoadout(@Nonnull Player player, @Nonnull String id) {
        Inventory inventory = player.getInventory();
        Loadout.Builder builder = Loadout.builder(id);

        // Capture hotbar
        ItemContainer hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.getCapacity(); i++) {
            ItemStack stack = hotbar.getItemStack((short) i);
            if (stack != null && !stack.isEmpty()) {
                builder.hotbar(stack.getItemId(), stack.getQuantity(), i);
            }
        }

        // Capture armor
        ItemContainer armor = inventory.getArmor();
        for (int i = 0; i < armor.getCapacity(); i++) {
            ItemStack stack = armor.getItemStack((short) i);
            if (stack != null && !stack.isEmpty()) {
                builder.armor(stack.getItemId(), i);
            }
        }

        // Capture utility
        ItemContainer utility = inventory.getUtility();
        for (int i = 0; i < utility.getCapacity(); i++) {
            ItemStack stack = utility.getItemStack((short) i);
            if (stack != null && !stack.isEmpty()) {
                builder.utility(stack.getItemId(), stack.getQuantity(), i);
            }
        }

        return builder.build();
    }
}
