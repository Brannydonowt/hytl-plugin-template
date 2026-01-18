package dev.branny.hytale.servercore.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.loadout.LoadoutItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-gamemode player data that persists between sessions.
 * Stores saved inventory, gamemode-specific stats, and play history.
 * 
 * Uses simple string serialization for inventory items due to Hytale codec limitations.
 * Format: "itemId:quantity:slot;itemId:quantity:slot;..."
 */
public class GamemodePlayerData {

    private static final String ITEM_DELIMITER = ";";
    private static final String FIELD_DELIMITER = ":";
    private static final String STAT_DELIMITER = ";";
    private static final String STAT_FIELD_DELIMITER = "=";

    public static final BuilderCodec<GamemodePlayerData> CODEC = BuilderCodec.builder(
            GamemodePlayerData.class,
            GamemodePlayerData::new
        )
        .addField(new KeyedCodec<>("HotbarItemsData", Codec.STRING),
            (data, value) -> data.hotbarItemsData = value,
            data -> data.hotbarItemsData)
        .addField(new KeyedCodec<>("ArmorItemsData", Codec.STRING),
            (data, value) -> data.armorItemsData = value,
            data -> data.armorItemsData)
        .addField(new KeyedCodec<>("UtilityItemsData", Codec.STRING),
            (data, value) -> data.utilityItemsData = value,
            data -> data.utilityItemsData)
        .addField(new KeyedCodec<>("StorageItemsData", Codec.STRING),
            (data, value) -> data.storageItemsData = value,
            data -> data.storageItemsData)
        .addField(new KeyedCodec<>("LastPlayedTimestamp", Codec.LONG),
            (data, value) -> data.lastPlayedTimestamp = value,
            data -> data.lastPlayedTimestamp)
        .addField(new KeyedCodec<>("TotalPlayTimeSeconds", Codec.LONG),
            (data, value) -> data.totalPlayTimeSeconds = value,
            data -> data.totalPlayTimeSeconds)
        .addField(new KeyedCodec<>("StatsData", Codec.STRING),
            (data, value) -> data.statsData = value,
            data -> data.statsData)
        .build();

    // Serialized inventory data (format: "itemId:quantity:slot;...")
    private String hotbarItemsData;
    private String armorItemsData;
    private String utilityItemsData;
    private String storageItemsData;

    // Serialized stats (format: "key=value;...")
    private String statsData;

    // Timestamps
    private long lastPlayedTimestamp;
    private long totalPlayTimeSeconds;

    public GamemodePlayerData() {
        this.hotbarItemsData = "";
        this.armorItemsData = "";
        this.utilityItemsData = "";
        this.storageItemsData = "";
        this.statsData = "";
        this.lastPlayedTimestamp = 0;
        this.totalPlayTimeSeconds = 0;
    }

    // ==================== Inventory Management ====================

    /**
     * Checks if there is a saved inventory.
     */
    public boolean hasInventory() {
        return !hotbarItemsData.isEmpty() || !armorItemsData.isEmpty() || 
               !utilityItemsData.isEmpty() || !storageItemsData.isEmpty();
    }

    /**
     * Saves inventory data from a Loadout.
     */
    public void saveInventory(@Nonnull Loadout loadout) {
        this.hotbarItemsData = serializeItems(loadout.getHotbarItems());
        this.armorItemsData = serializeItems(loadout.getArmorItems());
        this.utilityItemsData = serializeItems(loadout.getUtilityItems());
    }

    /**
     * Saves inventory with explicit storage items.
     */
    public void saveInventory(@Nonnull Loadout loadout, @Nonnull List<LoadoutItem> storage) {
        saveInventory(loadout);
        this.storageItemsData = serializeItems(storage);
    }

    /**
     * Creates a Loadout from the saved inventory data.
     */
    @Nullable
    public Loadout toLoadout(@Nonnull String loadoutId) {
        if (!hasInventory()) {
            return null;
        }

        Loadout.Builder builder = Loadout.builder(loadoutId);

        for (LoadoutItem item : getHotbarItems()) {
            if (item.isValid()) {
                builder.hotbar(item.itemId(), item.quantity(), item.slot());
            }
        }

        for (LoadoutItem item : getArmorItems()) {
            if (item.isValid()) {
                builder.armor(item.itemId(), item.slot());
            }
        }

        for (LoadoutItem item : getUtilityItems()) {
            if (item.isValid()) {
                builder.utility(item.itemId(), item.quantity(), item.slot());
            }
        }

        return builder.build();
    }

    /**
     * Clears the saved inventory.
     */
    public void clearInventory() {
        this.hotbarItemsData = "";
        this.armorItemsData = "";
        this.utilityItemsData = "";
        this.storageItemsData = "";
    }

    @Nonnull
    public List<LoadoutItem> getHotbarItems() {
        return deserializeItems(hotbarItemsData);
    }

    @Nonnull
    public List<LoadoutItem> getArmorItems() {
        return deserializeItems(armorItemsData);
    }

    @Nonnull
    public List<LoadoutItem> getUtilityItems() {
        return deserializeItems(utilityItemsData);
    }

    @Nonnull
    public List<LoadoutItem> getStorageItems() {
        return deserializeItems(storageItemsData);
    }

    public void setStorageItems(@Nonnull List<LoadoutItem> storageItems) {
        this.storageItemsData = serializeItems(storageItems);
    }

    // ==================== Statistics ====================

    @Nonnull
    public Map<String, Integer> getStats() {
        return deserializeStats(statsData);
    }

    public int getStat(@Nonnull String key) {
        return getStats().getOrDefault(key, 0);
    }

    public void setStat(@Nonnull String key, int value) {
        Map<String, Integer> stats = getStats();
        stats.put(key, value);
        this.statsData = serializeStats(stats);
    }

    public void incrementStat(@Nonnull String key) {
        incrementStat(key, 1);
    }

    public void incrementStat(@Nonnull String key, int amount) {
        Map<String, Integer> stats = getStats();
        stats.merge(key, amount, Integer::sum);
        this.statsData = serializeStats(stats);
    }

    // ==================== Timestamps ====================

    public long getLastPlayedTimestamp() {
        return lastPlayedTimestamp;
    }

    public void setLastPlayedTimestamp(long lastPlayedTimestamp) {
        this.lastPlayedTimestamp = lastPlayedTimestamp;
    }

    public void updateLastPlayed() {
        this.lastPlayedTimestamp = System.currentTimeMillis();
    }

    public long getTotalPlayTimeSeconds() {
        return totalPlayTimeSeconds;
    }

    public void setTotalPlayTimeSeconds(long totalPlayTimeSeconds) {
        this.totalPlayTimeSeconds = totalPlayTimeSeconds;
    }

    public void addPlayTime(long seconds) {
        this.totalPlayTimeSeconds += seconds;
    }

    // ==================== Serialization Helpers ====================

    /**
     * Serializes a list of LoadoutItems to a string.
     * Format: "itemId:quantity:slot;itemId:quantity:slot;..."
     */
    @Nonnull
    private static String serializeItems(@Nonnull List<LoadoutItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            LoadoutItem item = items.get(i);
            if (i > 0) sb.append(ITEM_DELIMITER);
            sb.append(item.itemId())
              .append(FIELD_DELIMITER)
              .append(item.quantity())
              .append(FIELD_DELIMITER)
              .append(item.slot());
        }
        return sb.toString();
    }

    /**
     * Deserializes a string to a list of LoadoutItems.
     */
    @Nonnull
    private static List<LoadoutItem> deserializeItems(@Nonnull String data) {
        List<LoadoutItem> items = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return items;
        }
        String[] entries = data.split(ITEM_DELIMITER);
        for (String entry : entries) {
            String[] fields = entry.split(FIELD_DELIMITER);
            if (fields.length >= 3) {
                try {
                    String itemId = fields[0];
                    int quantity = Integer.parseInt(fields[1]);
                    int slot = Integer.parseInt(fields[2]);
                    items.add(new LoadoutItem(itemId, quantity, slot));
                } catch (NumberFormatException ignored) {
                    // Skip invalid entries
                }
            }
        }
        return items;
    }

    /**
     * Serializes stats map to a string.
     * Format: "key=value;key=value;..."
     */
    @Nonnull
    private static String serializeStats(@Nonnull Map<String, Integer> stats) {
        if (stats.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (!first) sb.append(STAT_DELIMITER);
            first = false;
            sb.append(entry.getKey())
              .append(STAT_FIELD_DELIMITER)
              .append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Deserializes a string to a stats map.
     */
    @Nonnull
    private static Map<String, Integer> deserializeStats(@Nonnull String data) {
        Map<String, Integer> stats = new HashMap<>();
        if (data == null || data.isEmpty()) {
            return stats;
        }
        String[] entries = data.split(STAT_DELIMITER);
        for (String entry : entries) {
            String[] parts = entry.split(STAT_FIELD_DELIMITER);
            if (parts.length == 2) {
                try {
                    stats.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                    // Skip invalid entries
                }
            }
        }
        return stats;
    }

    @Override
    @Nonnull
    public String toString() {
        return "GamemodePlayerData{" +
                "inventory=" + (hasInventory() ? "present" : "empty") +
                ", stats=" + getStats().size() +
                ", lastPlayed=" + lastPlayedTimestamp +
                ", playTime=" + totalPlayTimeSeconds + "s" +
                '}';
    }
}
