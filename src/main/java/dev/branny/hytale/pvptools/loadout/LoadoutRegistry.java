package dev.branny.hytale.pvptools.loadout;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for named loadouts.
 * Loadouts can be registered programmatically or loaded from configuration.
 */
public final class LoadoutRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<String, Loadout> loadouts = new ConcurrentHashMap<>();

    private LoadoutRegistry() {
        // Utility class
    }

    // ==================== Registration ====================

    /**
     * Registers a loadout.
     *
     * @param loadout the loadout to register
     */
    public static void register(@Nonnull Loadout loadout) {
        String id = loadout.getId().toLowerCase();
        if (loadouts.containsKey(id)) {
            LOGGER.atWarning().log("Overwriting existing loadout: " + id);
        }
        loadouts.put(id, loadout);
        LOGGER.atInfo().log("Registered loadout: " + id + " (" + loadout.getItemCount() + " items)");
    }

    /**
     * Unregisters a loadout by ID.
     *
     * @param id the loadout ID
     * @return the removed loadout, or null if not found
     */
    @Nullable
    public static Loadout unregister(@Nonnull String id) {
        return loadouts.remove(id.toLowerCase());
    }

    // ==================== Lookup ====================

    /**
     * Gets a loadout by ID.
     *
     * @param id the loadout ID
     * @return the loadout, or null if not found
     */
    @Nullable
    public static Loadout get(@Nonnull String id) {
        return loadouts.get(id.toLowerCase());
    }

    /**
     * Gets a loadout by ID, or returns a default if not found.
     *
     * @param id the loadout ID
     * @param defaultLoadout the default to return
     * @return the loadout, or the default
     */
    @Nonnull
    public static Loadout getOrDefault(@Nonnull String id, @Nonnull Loadout defaultLoadout) {
        Loadout loadout = get(id);
        return loadout != null ? loadout : defaultLoadout;
    }

    /**
     * Checks if a loadout is registered.
     *
     * @param id the loadout ID
     * @return true if registered
     */
    public static boolean exists(@Nonnull String id) {
        return loadouts.containsKey(id.toLowerCase());
    }

    /**
     * Gets all registered loadout IDs.
     *
     * @return collection of loadout IDs
     */
    @Nonnull
    public static Collection<String> getLoadoutIds() {
        return Collections.unmodifiableSet(loadouts.keySet());
    }

    /**
     * Gets all registered loadouts.
     *
     * @return collection of loadouts
     */
    @Nonnull
    public static Collection<Loadout> getAllLoadouts() {
        return Collections.unmodifiableCollection(loadouts.values());
    }

    /**
     * Gets the number of registered loadouts.
     *
     * @return count
     */
    public static int getLoadoutCount() {
        return loadouts.size();
    }

    /**
     * Clears all registered loadouts.
     */
    public static void clear() {
        loadouts.clear();
        LOGGER.atInfo().log("Cleared all loadouts");
    }

    // ==================== Default Loadouts ====================

    /**
     * Registers the default/built-in loadouts.
     * Called during plugin initialization.
     */
    public static void registerDefaults() {
        // Arena Default Loadout
        register(Loadout.builder("arena_default")
            .displayName("Arena Standard")
            .hotbar("Weapon_Club_Adamantite", 1)
            .hotbar("Weapon_Shield_Mithril", 1)
            .hotbar("Weapon_Bomb_Potion_Poison", 5)
            .hotbar("Weapon_Deployable_Healing_Totem", 1)
            .hotbar("Potion_Regen_Stamina_Large", 3)
            .hotbar("Potion_Signature_Greater", 3)
            .build());

        // Sniper Wars Loadout
        register(Loadout.builder("sniper_wars")
            .displayName("Sniper Wars")
            .hotbar("Weapon_Bow_Mythic", 1)
            .hotbar("Ammunition_Arrow", 64)
            .hotbar("Potion_Speed", 3)
            .build());

        // FFA Loadout
        register(Loadout.builder("ffa_default")
            .displayName("Free-For-All")
            .hotbar("Weapon_Sword_Iron", 1)
            .hotbar("Weapon_Bow_Wooden", 1)
            .hotbar("Ammunition_Arrow", 32)
            .hotbar("Food_Apple", 16)
            .armor("Armor_Chest_Leather")
            .armor("Armor_Legs_Leather")
            .build());

        // Empty Loadout (for clearing)
        register(Loadout.builder("empty")
            .displayName("Empty")
            .build());

        LOGGER.atInfo().log("Registered " + getLoadoutCount() + " default loadouts");
    }
}
