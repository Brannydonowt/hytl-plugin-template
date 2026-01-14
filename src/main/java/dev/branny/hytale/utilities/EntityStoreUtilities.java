package dev.branny.hytale.utilities;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Utility class for obtaining EntityStore and ComponentAccessor from a World.
 * Provides a clean abstraction for ECS interactions.
 */
public final class EntityStoreUtilities {

    private EntityStoreUtilities() {
        // Utility class; do not instantiate.
    }

    /**
     * Gets the EntityStore from the given World.
     *
     * @param world the world to get the EntityStore from
     * @return the EntityStore for the world
     */
    @Nonnull
    public static EntityStore getEntityStore(@Nonnull World world) {
        return world.getEntityStore();
    }

    /**
     * Gets a ComponentAccessor for the given World's EntityStore.
     * Useful for ECS operations that require a ComponentAccessor (e.g., SoundUtil methods).
     *
     * @param world the world to get the accessor from
     * @return a ComponentAccessor for the world's EntityStore
     */
    @Nonnull
    public static ComponentAccessor<EntityStore> getAccessor(@Nonnull World world) {
        return world.getEntityStore().getStore();
    }
}
