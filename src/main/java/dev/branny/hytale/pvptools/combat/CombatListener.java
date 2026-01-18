package dev.branny.hytale.pvptools.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.branny.hytale.servercore.player.PlayerSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Generic damage/death event listener for the PVP tools system.
 * Detects player deaths and records them through the KillTracker.
 * 
 * This system runs in the Inspect Damage Group (default for DamageEventSystem),
 * which means it runs AFTER damage has been applied to health.
 */
public class CombatListener extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        // Handle damage events for all entities
        return Query.any();
    }

    @Override
    public void handle(int index, 
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store, 
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        
        // Get the target entity (who received damage)
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        // Check if target is a player
        Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());
        if (targetPlayer == null) {
            return;
        }

        // Check if the damage was cancelled
        if (damage.isCancelled()) {
            return;
        }

        // Check if the player died from this damage
        DeathComponent deathComponent = store.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComponent == null) {
            // Player took damage but didn't die - nothing special to do
            return;
        }

        // Player died - process the death
        processPlayerDeath(targetPlayer, damage, store);
    }

    /**
     * Processes a player death event.
     */
    @SuppressWarnings("deprecation")
    private void processPlayerDeath(@Nonnull Player targetPlayer, 
                                    @Nonnull Damage damage,
                                    @Nonnull Store<EntityStore> store) {
        
        PlayerRef victimRef = targetPlayer.getPlayerRef();
        if (victimRef == null) {
            return;
        }

        // Extract killer information
        PlayerRef killerRef = extractKillerFromDamage(damage, store);
        String cause = determineCause(damage);

        // Get match context from player session
        PlayerSession session = PlayerSession.get(victimRef.getUuid());
        java.util.UUID matchId = session != null ? session.getCurrentMatchId() : null;
        String gamemodeId = session != null ? session.getCurrentGamemode() : null;

        LOGGER.atInfo().log("Player " + victimRef.getUsername() + " died. Cause: " + cause + 
            (killerRef != null ? ", Killer: " + killerRef.getUsername() : ""));

        // Record the kill through KillTracker
        KillTracker.recordKill(victimRef, killerRef, cause, matchId, gamemodeId);
    }

    /**
     * Extracts the killer PlayerRef from a damage event.
     *
     * @param damage the damage event
     * @param store the entity store for component lookups
     * @return the killer's PlayerRef, or null if not killed by a player
     */
    @SuppressWarnings("deprecation")
    @Nullable
    private PlayerRef extractKillerFromDamage(@Nonnull Damage damage, @Nonnull Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        
        // Check if damage came from an entity (player or mob)
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef != null && attackerRef.isValid()) {
                Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
                if (attackerPlayer != null) {
                    return attackerPlayer.getPlayerRef();
                }
            }
        }
        
        // Check if damage came from a projectile shot by a player
        if (source instanceof Damage.ProjectileSource projectileSource) {
            Ref<EntityStore> shooterRef = projectileSource.getRef();
            if (shooterRef != null && shooterRef.isValid()) {
                Player shooterPlayer = store.getComponent(shooterRef, Player.getComponentType());
                if (shooterPlayer != null) {
                    return shooterPlayer.getPlayerRef();
                }
            }
        }
        
        // Damage wasn't from a player
        return null;
    }

    /**
     * Determines the cause of death from the damage event.
     *
     * @param damage the damage event
     * @return a string describing the cause
     */
    @Nonnull
    private String determineCause(@Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        
        if (source instanceof Damage.EntitySource) {
            return "combat";
        }
        if (source instanceof Damage.ProjectileSource) {
            return "projectile";
        }
        if (source instanceof Damage.EnvironmentSource envSource) {
            return envSource.getType() != null ? envSource.getType() : "environment";
        }
        if (source instanceof Damage.CommandSource) {
            return "command";
        }
        
        return "unknown";
    }
}
