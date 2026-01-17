package dev.branny.hytale.utilities;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;

public final class ItemUtilities {

    private ItemUtilities() {
        // Utility class; do not instantiate.
    }

    public static void giveItemToHotbar(@Nonnull Player player, @Nonnull ItemStack itemStack) {
        Inventory inventory = player.getInventory();
        inventory.getHotbar().addItemStack(itemStack);
    }

     public static void clearInventory(@Nonnull Player player) {
        Inventory inventory = player.getInventory();
        inventory.clear();
     }
}

