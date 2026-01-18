package dev.branny.hytale.servercore.persistence;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for accessing and managing persistent player data.
 * 
 * Provides convenient static methods to access PlayerDataComponent
 * from PlayerRef, handling the complexity of Holder vs Ref access.
 * 
 * Note: Custom components are NOT automatically cloned to world EntityStores,
 * so we maintain a cache by player UUID for reliable access.
 */
public final class PlayerDataService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ComponentType<EntityStore, PlayerDataComponent> componentType;
    
    // Cache of PlayerDataComponent by player UUID - needed because custom components
    // exist on the Holder but are NOT cloned to world EntityStores
    private static final Map<UUID, PlayerDataComponent> componentCache = new ConcurrentHashMap<>();

    private PlayerDataService() {
        // Utility class
    }

    /**
     * Sets the component type after registration.
     * Called from BrannyPlugin.setup() after registering the component.
     */
    public static void setComponentType(@Nonnull ComponentType<EntityStore, PlayerDataComponent> type) {
        componentType = type;
        LOGGER.atInfo().log("PlayerDataService initialized with component type");
    }

    /**
     * Gets the registered component type.
     */
    @Nullable
    public static ComponentType<EntityStore, PlayerDataComponent> getComponentType() {
        return componentType;
    }

    // ==================== Data Access ====================

    /**
     * Gets the PlayerDataComponent for a player, creating it if needed.
     * Uses the cache for reliable access since custom components aren't cloned to world stores.
     *
     * @param playerRef the player reference
     * @return the player data component, or null if unable to access
     */
    @Nullable
    public static PlayerDataComponent get(@Nonnull PlayerRef playerRef) {
        if (componentType == null) {
            LOGGER.atWarning().log("PlayerDataService not initialized - componentType is null");
            return null;
        }

        UUID playerId = playerRef.getUuid();
        
        // Check cache first - this is the most reliable access method
        PlayerDataComponent cached = componentCache.get(playerId);
        if (cached != null) {
            return cached;
        }

        // Try to get from Holder (available during connect or between worlds)
        Holder<EntityStore> holder = playerRef.getHolder();
        if (holder != null) {
            PlayerDataComponent data = holder.ensureAndGetComponent(componentType);
            if (data != null) {
                componentCache.put(playerId, data);
                return data;
            }
        }

        // Try to get from Ref (player in world) - may work for some components
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            PlayerDataComponent data = store.getComponent(ref, componentType);
            if (data != null) {
                componentCache.put(playerId, data);
                return data;
            }
        }

        LOGGER.atWarning().log("[PlayerData] Cannot access data for " + playerRef.getUsername() + 
            " - not in cache, Holder=" + (playerRef.getHolder() != null) + 
            ", Ref=" + (playerRef.getReference() != null && playerRef.getReference().isValid()));
        return null;
    }

    /**
     * Gets the PlayerDataComponent if it exists, without creating it.
     * Checks the cache first for reliable access during world transfers.
     *
     * @param playerRef the player reference
     * @return the player data component, or null if not present
     */
    @Nullable
    public static PlayerDataComponent getIfPresent(@Nonnull PlayerRef playerRef) {
        if (componentType == null) {
            return null;
        }

        // Check cache first - most reliable during world transfers
        UUID playerId = playerRef.getUuid();
        PlayerDataComponent cached = componentCache.get(playerId);
        if (cached != null) {
            return cached;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            return ref.getStore().getComponent(ref, componentType);
        }

        Holder<EntityStore> holder = playerRef.getHolder();
        if (holder != null) {
            return holder.getComponent(componentType);
        }

        return null;
    }

    /**
     * Gets the PlayerDataComponent directly from cache by player UUID.
     * This is the most reliable method when you have the player's UUID.
     *
     * @param playerId the player's UUID
     * @return the player data component, or null if not cached
     */
    @Nullable
    public static PlayerDataComponent getByUuid(@Nonnull UUID playerId) {
        return componentCache.get(playerId);
    }

    /**
     * Gets the PlayerDataComponent from a captured Ref, falling back to cache.
     * Use this when you have a pre-validated Ref to avoid race conditions during world transfers.
     *
     * @param ref the pre-captured entity reference
     * @param playerId the player's UUID for cache fallback
     * @return the player data component, or null if not found
     */
    @Nullable
    public static PlayerDataComponent getFromRef(@Nonnull Ref<EntityStore> ref, @Nonnull UUID playerId) {
        // Check cache first - most reliable
        PlayerDataComponent cached = componentCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Try Ref's store as fallback
        if (componentType != null && ref.isValid()) {
            PlayerDataComponent data = ref.getStore().getComponent(ref, componentType);
            if (data != null) {
                componentCache.put(playerId, data);
                return data;
            }
        }
        return null;
    }

    // ==================== Convenience Methods ====================

    /**
     * Gets the global player data for a player.
     *
     * @param playerRef the player reference
     * @return the global data, or null if unable to access
     */
    @Nullable
    public static GlobalPlayerData getGlobalData(@Nonnull PlayerRef playerRef) {
        PlayerDataComponent component = get(playerRef);
        return component != null ? component.getGlobalData() : null;
    }

    /**
     * Gets the gamemode-specific data for a player.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode ID
     * @return the gamemode data (created if not present), or null if unable to access
     */
    @Nullable
    public static GamemodePlayerData getGamemodeData(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        PlayerDataComponent component = get(playerRef);
        return component != null ? component.getGamemodeData(gamemodeId) : null;
    }

    /**
     * Gets the gamemode-specific data if it exists, without creating it.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode ID
     * @return the gamemode data, or null if not present
     */
    @Nullable
    public static GamemodePlayerData getGamemodeDataIfPresent(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        PlayerDataComponent component = getIfPresent(playerRef);
        return component != null ? component.getGamemodeDataIfPresent(gamemodeId) : null;
    }

    /**
     * Checks if a player has data for a specific gamemode.
     *
     * @param playerRef the player reference
     * @param gamemodeId the gamemode ID
     * @return true if gamemode data exists
     */
    public static boolean hasGamemodeData(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        PlayerDataComponent component = getIfPresent(playerRef);
        return component != null && component.hasGamemodeData(gamemodeId);
    }

    // ==================== Session Lifecycle ====================

    /**
     * Initializes player data directly from a Holder (before player enters any world).
     * This is the earliest point to ensure the component exists and will persist across transfers.
     * Also caches the component for reliable access throughout the session.
     *
     * @param holder the player's entity holder
     * @param playerId the player's UUID
     * @param username the player's username (for logging)
     */
    public static void initFromHolder(@Nonnull Holder<EntityStore> holder, @Nonnull UUID playerId, @Nonnull String username) {
        if (componentType == null) {
            LOGGER.atSevere().log("[PlayerData] INIT FAILED for " + username + " - componentType not registered");
            return;
        }

        // ensureAndGetComponent creates the component if it doesn't exist
        try {
            PlayerDataComponent data = holder.ensureAndGetComponent(componentType);
            if (data != null) {
                // Cache for reliable access - custom components aren't cloned to world stores
                componentCache.put(playerId, data);
                
                GlobalPlayerData global = data.getGlobalData();
                global.recordFirstJoinIfNeeded();
                global.updateLastSeen();
                LOGGER.atInfo().log("[PlayerData] Initialized for " + username);
            } else {
                LOGGER.atSevere().log("[PlayerData] INIT FAILED for " + username + " - ensureAndGetComponent returned null");
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[PlayerData] INIT FAILED for " + username + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Called when a player disconnects to update their data.
     * Updates last seen timestamp and cleans up the cache.
     *
     * @param playerRef the player reference
     */
    public static void onPlayerDisconnect(@Nonnull PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        
        // Get from cache for final update
        PlayerDataComponent data = componentCache.get(playerId);
        if (data != null) {
            data.getGlobalData().updateLastSeen();
            data.markGamemodeDataChanged(); // Ensure any pending changes are serialized
        }
        
        // Clean up cache - the Holder will persist the data
        componentCache.remove(playerId);
    }
}
