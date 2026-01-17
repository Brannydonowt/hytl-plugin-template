package dev.branny.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import dev.branny.hytale.commands.ArenaCommands;
import dev.branny.hytale.commands.BrannyCommand;
import dev.branny.hytale.commands.ExampleCommand;
import dev.branny.hytale.commands.PoopCommand;
import dev.branny.hytale.utilities.WorldUtilities;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class BrannyPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /** Tracks players who have completed their initial join (to avoid re-routing on world changes) */
    private final Set<UUID> initialJoinHandled = ConcurrentHashMap.newKeySet();

    public BrannyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        this.getCommandRegistry().registerCommand(new BrannyCommand(this.getName(), this.getManifest().getVersion().toString()));
        this.getCommandRegistry().registerCommand(new PoopCommand());
        this.getCommandRegistry().registerCommand(new ArenaCommands());
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting plugin " + this.getName() + " - registering event listeners");
        
        // Register player ready event globally (across all worlds) to route players into the lobby
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> onPlayerReady(event));
        
        // Register disconnect event to clean up tracking state
        getEventRegistry().register(PlayerDisconnectEvent.class, event -> onPlayerDisconnect(event));
    }
    
    /**
     * Cleans up player tracking state on disconnect.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef != null) {
            initialJoinHandled.remove(playerRef.getUuid());
            LOGGER.atInfo().log("Player disconnected, cleared initial join state");
        }
    }

    /**
     * Handles player ready events (fired when player is fully loaded into a world).
     * On initial join, ensures the player is routed to the lobby world at the spawn point.
     */
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            LOGGER.atWarning().log("PlayerReadyEvent received with null player");
            return;
        }

        UUID playerUuid = player.getUuid();
        
        // Check if we've already handled this player's initial join
        if (!initialJoinHandled.add(playerUuid)) {
            // Already handled - this is a world change, not initial join
            LOGGER.atInfo().log("Player ready in world (not initial join), skipping lobby redirect");
            return;
        }

        LOGGER.atInfo().log("Player initial join detected, routing to lobby");

        PlayerRef playerRef = player.getPlayerRef();
        World currentWorld = WorldUtilities.getCurrentWorld(playerRef);

        if (currentWorld == null) {
            LOGGER.atWarning().log("PlayerReadyEvent: player has no current world");
            return;
        }

        // Check if player is already in lobby world
        if (WorldUtilities.LOBBY_WORLD_NAME.equals(currentWorld.getName())) {
            // Already in lobby - just teleport to spawn point on the world thread
            LOGGER.atInfo().log("Player already in lobby world, teleporting to spawn");
            teleportToLobbySpawn(currentWorld, player);
            return;
        }

        // Not in lobby - need to transfer worlds (with delay for client fade)
        LOGGER.atInfo().log("Player ready: scheduling lobby transfer");

        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
            // Re-check current world after delay (player state may have changed)
            World worldAfterDelay = WorldUtilities.getCurrentWorld(playerRef);
            if (worldAfterDelay == null) {
                LOGGER.atWarning().log("Player no longer in a world, skipping transfer");
                return;
            }

            // Check again if already in lobby (may have been transferred by other means)
            if (WorldUtilities.LOBBY_WORLD_NAME.equals(worldAfterDelay.getName())) {
                LOGGER.atInfo().log("Player now in lobby, teleporting to spawn");
                teleportToLobbySpawn(worldAfterDelay, player);
                return;
            }

            // Execute transfer on the current world's thread for proper synchronization
            worldAfterDelay.execute(() -> {
                LOGGER.atInfo().log("Transferring player to lobby");
                WorldUtilities.transferPlayerToWorld(playerRef, WorldUtilities.LOBBY_WORLD_NAME, false)
                    .whenComplete((transferredPlayer, error) -> {
                        if (error != null) {
                            LOGGER.atWarning().log("Failed to transfer to lobby: " + error.getMessage());
                        } else {
                            LOGGER.atInfo().log("Successfully transferred to lobby world");
                        }
                    });
            });
        });
    }

    /**
     * Teleports a player to the lobby spawn point on the world thread.
     */
    private void teleportToLobbySpawn(World world, Player player) {
        world.execute(() -> {
            Transform lobbySpawn = WorldUtilities.getWorldSpawn(WorldUtilities.LOBBY_WORLD_NAME);
            if (lobbySpawn != null) {
                player.getTransformComponent().teleportPosition(lobbySpawn.getPosition());
                LOGGER.atInfo().log("Teleported player to lobby spawn");
            }
        });
    }
}
