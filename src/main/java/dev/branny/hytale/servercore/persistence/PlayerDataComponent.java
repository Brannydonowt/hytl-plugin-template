package dev.branny.hytale.servercore.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * ECS component for persistent player data.
 * 
 * This component is registered with the EntityStoreRegistry and Hytale
 * automatically handles serialization/deserialization using the CODEC.
 * 
 * Contains:
 * - Global player data (currency, total play time, timestamps)
 * - Per-gamemode data map (inventories, stats per gamemode) - serialized as string
 */
public class PlayerDataComponent implements Component<EntityStore> {

    // Delimiter for separating gamemodes in serialized data
    private static final String GAMEMODE_DELIMITER = "|||";
    private static final String GAMEMODE_DATA_DELIMITER = ":::";

    public static final BuilderCodec<PlayerDataComponent> CODEC = BuilderCodec.builder(
            PlayerDataComponent.class,
            PlayerDataComponent::new
        )
        .addField(new KeyedCodec<>("GlobalData", GlobalPlayerData.CODEC),
            (component, value) -> component.globalData = value,
            component -> component.globalData)
        .addField(new KeyedCodec<>("GamemodeDataSerialized", Codec.STRING),
            (component, value) -> component.gamemodeDataSerialized = value,
            component -> component.gamemodeDataSerialized)
        .build();

    private GlobalPlayerData globalData;
    private String gamemodeDataSerialized;
    
    // Runtime cache - not serialized directly
    private transient Map<String, GamemodePlayerData> gamemodeDataCache;

    public PlayerDataComponent() {
        this.globalData = new GlobalPlayerData();
        this.gamemodeDataSerialized = "";
        this.gamemodeDataCache = null;
    }

    // ==================== Global Data ====================

    /**
     * Gets the global player data.
     */
    @Nonnull
    public GlobalPlayerData getGlobalData() {
        return globalData;
    }

    /**
     * Sets the global player data.
     */
    public void setGlobalData(@Nonnull GlobalPlayerData globalData) {
        this.globalData = globalData;
    }

    // ==================== Gamemode Data ====================

    /**
     * Gets the gamemode data map, lazily deserializing from string.
     */
    @Nonnull
    private Map<String, GamemodePlayerData> getGamemodeDataMap() {
        if (gamemodeDataCache == null) {
            gamemodeDataCache = deserializeGamemodeData(gamemodeDataSerialized);
        }
        return gamemodeDataCache;
    }

    /**
     * Marks the cache as dirty and serializes to string.
     */
    private void syncToSerialized() {
        if (gamemodeDataCache != null) {
            gamemodeDataSerialized = serializeGamemodeData(gamemodeDataCache);
        }
    }

    /**
     * Gets data for a specific gamemode, creating it if it doesn't exist.
     *
     * @param gamemodeId the gamemode ID
     * @return the gamemode data (never null)
     */
    @Nonnull
    public GamemodePlayerData getGamemodeData(@Nonnull String gamemodeId) {
        Map<String, GamemodePlayerData> map = getGamemodeDataMap();
        GamemodePlayerData data = map.get(gamemodeId.toLowerCase());
        if (data == null) {
            data = new GamemodePlayerData();
            map.put(gamemodeId.toLowerCase(), data);
            syncToSerialized();
        }
        return data;
    }

    /**
     * Gets data for a specific gamemode if it exists.
     *
     * @param gamemodeId the gamemode ID
     * @return the gamemode data, or null if not present
     */
    public GamemodePlayerData getGamemodeDataIfPresent(@Nonnull String gamemodeId) {
        return getGamemodeDataMap().get(gamemodeId.toLowerCase());
    }

    /**
     * Checks if data exists for a specific gamemode.
     *
     * @param gamemodeId the gamemode ID
     * @return true if data exists
     */
    public boolean hasGamemodeData(@Nonnull String gamemodeId) {
        return getGamemodeDataMap().containsKey(gamemodeId.toLowerCase());
    }

    /**
     * Gets all gamemode data.
     *
     * @return map of gamemode IDs to data
     */
    @Nonnull
    public Map<String, GamemodePlayerData> getAllGamemodeData() {
        return getGamemodeDataMap();
    }

    /**
     * Removes data for a specific gamemode.
     *
     * @param gamemodeId the gamemode ID
     * @return the removed data, or null if not present
     */
    public GamemodePlayerData removeGamemodeData(@Nonnull String gamemodeId) {
        GamemodePlayerData removed = getGamemodeDataMap().remove(gamemodeId.toLowerCase());
        if (removed != null) {
            syncToSerialized();
        }
        return removed;
    }

    /**
     * Call this after modifying gamemode data to ensure serialization.
     */
    public void markGamemodeDataChanged() {
        syncToSerialized();
    }

    // ==================== Serialization Helpers ====================

    /**
     * Serializes gamemode data map to a string.
     * Format: "gamemodeId:::fieldData|||gamemodeId:::fieldData..."
     * Where fieldData is: "hotbar|armor|utility|storage|lastPlayed|totalPlay|stats"
     */
    @Nonnull
    private static String serializeGamemodeData(@Nonnull Map<String, GamemodePlayerData> data) {
        if (data.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, GamemodePlayerData> entry : data.entrySet()) {
            if (!first) sb.append(GAMEMODE_DELIMITER);
            first = false;
            
            GamemodePlayerData gd = entry.getValue();
            sb.append(entry.getKey())
              .append(GAMEMODE_DATA_DELIMITER)
              .append(serializeGamemodeFields(gd));
        }
        return sb.toString();
    }

    /**
     * Serializes individual gamemode fields.
     * Format: "hotbar|armor|utility|storage|lastPlayed|totalPlay|stats"
     */
    @Nonnull
    private static String serializeGamemodeFields(@Nonnull GamemodePlayerData gd) {
        // Access internal serialized data through reflection-free approach
        // We store: hotbar|armor|utility|storage|lastPlayed|totalPlay|stats (using | as field separator)
        return String.join("|",
            serializeItemList(gd.getHotbarItems()),
            serializeItemList(gd.getArmorItems()),
            serializeItemList(gd.getUtilityItems()),
            serializeItemList(gd.getStorageItems()),
            String.valueOf(gd.getLastPlayedTimestamp()),
            String.valueOf(gd.getTotalPlayTimeSeconds()),
            serializeStats(gd.getStats())
        );
    }

    @Nonnull
    private static String serializeItemList(@Nonnull java.util.List<dev.branny.hytale.pvptools.loadout.LoadoutItem> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            var item = items.get(i);
            sb.append(item.itemId()).append(":").append(item.quantity()).append(":").append(item.slot());
        }
        return sb.toString();
    }

    @Nonnull
    private static String serializeStats(@Nonnull Map<String, Integer> stats) {
        if (stats.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var entry : stats.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Deserializes gamemode data map from a string.
     */
    @Nonnull
    private static Map<String, GamemodePlayerData> deserializeGamemodeData(@Nonnull String data) {
        Map<String, GamemodePlayerData> result = new HashMap<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        
        String[] gamemodes = data.split("\\|\\|\\|");
        for (String gamemodeEntry : gamemodes) {
            String[] parts = gamemodeEntry.split(":::");
            if (parts.length == 2) {
                String gamemodeId = parts[0];
                GamemodePlayerData gd = deserializeGamemodeFields(parts[1]);
                result.put(gamemodeId, gd);
            }
        }
        return result;
    }

    /**
     * Deserializes individual gamemode fields.
     */
    @Nonnull
    private static GamemodePlayerData deserializeGamemodeFields(@Nonnull String data) {
        GamemodePlayerData gd = new GamemodePlayerData();
        String[] fields = data.split("\\|", -1); // -1 to keep empty strings
        
        if (fields.length >= 7) {
            // Reconstruct through the public API by setting storage items and stats
            var hotbar = deserializeItemList(fields[0]);
            var armor = deserializeItemList(fields[1]);
            var utility = deserializeItemList(fields[2]);
            var storage = deserializeItemList(fields[3]);
            
            // Create a temporary loadout to save
            if (!hotbar.isEmpty() || !armor.isEmpty() || !utility.isEmpty()) {
                var builder = dev.branny.hytale.pvptools.loadout.Loadout.builder("temp");
                for (var item : hotbar) {
                    builder.hotbar(item.itemId(), item.quantity(), item.slot());
                }
                for (var item : armor) {
                    builder.armor(item.itemId(), item.slot());
                }
                for (var item : utility) {
                    builder.utility(item.itemId(), item.quantity(), item.slot());
                }
                gd.saveInventory(builder.build(), storage);
            } else if (!storage.isEmpty()) {
                gd.setStorageItems(storage);
            }
            
            try {
                gd.setLastPlayedTimestamp(Long.parseLong(fields[4]));
                gd.setTotalPlayTimeSeconds(Long.parseLong(fields[5]));
            } catch (NumberFormatException ignored) {}
            
            // Restore stats
            var stats = deserializeStatsMap(fields[6]);
            for (var entry : stats.entrySet()) {
                gd.setStat(entry.getKey(), entry.getValue());
            }
        }
        return gd;
    }

    @Nonnull
    private static java.util.List<dev.branny.hytale.pvptools.loadout.LoadoutItem> deserializeItemList(@Nonnull String data) {
        java.util.List<dev.branny.hytale.pvptools.loadout.LoadoutItem> items = new java.util.ArrayList<>();
        if (data == null || data.isEmpty()) return items;
        
        String[] entries = data.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                try {
                    items.add(new dev.branny.hytale.pvptools.loadout.LoadoutItem(
                        parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                } catch (NumberFormatException ignored) {}
            }
        }
        return items;
    }

    @Nonnull
    private static Map<String, Integer> deserializeStatsMap(@Nonnull String data) {
        Map<String, Integer> stats = new HashMap<>();
        if (data == null || data.isEmpty()) return stats;
        
        String[] entries = data.split(",");
        for (String entry : entries) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    stats.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return stats;
    }

    // ==================== Component Interface ====================

    @Override
    public Component<EntityStore> clone() {
        PlayerDataComponent copy = new PlayerDataComponent();
        
        // Deep copy global data
        copy.globalData = new GlobalPlayerData();
        copy.globalData.setServerCurrency(this.globalData.getServerCurrency());
        copy.globalData.setTotalPlayTimeSeconds(this.globalData.getTotalPlayTimeSeconds());
        copy.globalData.setFirstJoinTimestamp(this.globalData.getFirstJoinTimestamp());
        copy.globalData.setLastSeenTimestamp(this.globalData.getLastSeenTimestamp());
        
        // Copy serialized gamemode data
        copy.gamemodeDataSerialized = this.gamemodeDataSerialized;
        copy.gamemodeDataCache = null; // Will be lazily deserialized
        
        return copy;
    }

    @Override
    @Nonnull
    public String toString() {
        return "PlayerDataComponent{" +
                "globalData=" + globalData +
                ", gamemodes=" + getGamemodeDataMap().keySet() +
                '}';
    }
}
