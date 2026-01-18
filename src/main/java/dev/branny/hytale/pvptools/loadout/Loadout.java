package dev.branny.hytale.pvptools.loadout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable definition of a player loadout.
 * Contains items for hotbar, armor, and utility slots.
 */
public final class Loadout {

    private final String id;
    private final String displayName;
    private final List<LoadoutItem> hotbarItems;
    private final List<LoadoutItem> armorItems;
    private final List<LoadoutItem> utilityItems;
    private final Map<String, Object> metadata;

    private Loadout(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName != null ? builder.displayName : builder.id;
        this.hotbarItems = Collections.unmodifiableList(new ArrayList<>(builder.hotbarItems));
        this.armorItems = Collections.unmodifiableList(new ArrayList<>(builder.armorItems));
        this.utilityItems = Collections.unmodifiableList(new ArrayList<>(builder.utilityItems));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    // ==================== Getters ====================

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public List<LoadoutItem> getHotbarItems() {
        return hotbarItems;
    }

    @Nonnull
    public List<LoadoutItem> getArmorItems() {
        return armorItems;
    }

    @Nonnull
    public List<LoadoutItem> getUtilityItems() {
        return utilityItems;
    }

    @Nonnull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @param <T> the expected type
     * @return the value, or null if not present
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMeta(@Nonnull String key) {
        return (T) metadata.get(key);
    }

    /**
     * Gets a metadata value with default.
     *
     * @param key the metadata key
     * @param defaultValue the default value
     * @param <T> the expected type
     * @return the value, or default if not present
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T getMeta(@Nonnull String key, @Nonnull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Checks if this loadout is empty.
     *
     * @return true if no items in any slot
     */
    public boolean isEmpty() {
        return hotbarItems.isEmpty() && armorItems.isEmpty() && utilityItems.isEmpty();
    }

    /**
     * Gets the total number of items in this loadout.
     *
     * @return item count
     */
    public int getItemCount() {
        return hotbarItems.size() + armorItems.size() + utilityItems.size();
    }

    @Override
    public String toString() {
        return "Loadout{" +
                "id='" + id + '\'' +
                ", hotbar=" + hotbarItems.size() +
                ", armor=" + armorItems.size() +
                ", utility=" + utilityItems.size() +
                '}';
    }

    // ==================== Builder ====================

    /**
     * Creates a new loadout builder.
     *
     * @param id the loadout ID
     * @return a new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull String id) {
        return new Builder(id);
    }

    /**
     * Builder for creating Loadout instances.
     */
    public static final class Builder {
        private final String id;
        private String displayName;
        private final List<LoadoutItem> hotbarItems = new ArrayList<>();
        private final List<LoadoutItem> armorItems = new ArrayList<>();
        private final List<LoadoutItem> utilityItems = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();

        private Builder(@Nonnull String id) {
            this.id = id;
        }

        /**
         * Sets the display name.
         */
        @Nonnull
        public Builder displayName(@Nonnull String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Adds an item to the hotbar.
         */
        @Nonnull
        public Builder hotbar(@Nonnull String itemId, int quantity) {
            hotbarItems.add(new LoadoutItem(itemId, quantity));
            return this;
        }

        /**
         * Adds an item to the hotbar at a specific slot.
         */
        @Nonnull
        public Builder hotbar(@Nonnull String itemId, int quantity, int slot) {
            hotbarItems.add(new LoadoutItem(itemId, quantity, slot));
            return this;
        }

        /**
         * Adds a single item to the hotbar.
         */
        @Nonnull
        public Builder hotbar(@Nonnull String itemId) {
            hotbarItems.add(new LoadoutItem(itemId, 1));
            return this;
        }

        /**
         * Adds an armor piece.
         */
        @Nonnull
        public Builder armor(@Nonnull String itemId) {
            armorItems.add(new LoadoutItem(itemId, 1));
            return this;
        }

        /**
         * Adds an armor piece at a specific slot.
         */
        @Nonnull
        public Builder armor(@Nonnull String itemId, int slot) {
            armorItems.add(new LoadoutItem(itemId, 1, slot));
            return this;
        }

        /**
         * Adds a utility item.
         */
        @Nonnull
        public Builder utility(@Nonnull String itemId, int quantity) {
            utilityItems.add(new LoadoutItem(itemId, quantity));
            return this;
        }

        /**
         * Adds a utility item at a specific slot.
         */
        @Nonnull
        public Builder utility(@Nonnull String itemId, int quantity, int slot) {
            utilityItems.add(new LoadoutItem(itemId, quantity, slot));
            return this;
        }

        /**
         * Adds metadata.
         */
        @Nonnull
        public Builder meta(@Nonnull String key, @Nonnull Object value) {
            metadata.put(key, value);
            return this;
        }

        /**
         * Builds the loadout.
         */
        @Nonnull
        public Loadout build() {
            return new Loadout(this);
        }
    }
}
