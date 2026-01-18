package dev.branny.hytale.pvptools.loadout;

import javax.annotation.Nonnull;

/**
 * Represents a single item in a loadout.
 * 
 * @param itemId the item's registry ID
 * @param quantity the quantity of the item
 * @param slot optional slot index (-1 for auto-place)
 */
public record LoadoutItem(
    @Nonnull String itemId,
    int quantity,
    int slot
) {
    /**
     * Creates a loadout item with auto-placement.
     *
     * @param itemId the item's registry ID
     * @param quantity the quantity
     */
    public LoadoutItem(@Nonnull String itemId, int quantity) {
        this(itemId, quantity, -1);
    }

    /**
     * Creates a single item with auto-placement.
     *
     * @param itemId the item's registry ID
     */
    public LoadoutItem(@Nonnull String itemId) {
        this(itemId, 1, -1);
    }

    /**
     * Validates this loadout item.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return itemId != null && !itemId.isEmpty() && quantity > 0;
    }

    @Override
    public String toString() {
        return "LoadoutItem{" + itemId + " x" + quantity + (slot >= 0 ? " @slot" + slot : "") + "}";
    }
}
