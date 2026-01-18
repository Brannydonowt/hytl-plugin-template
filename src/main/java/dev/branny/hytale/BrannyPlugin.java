package dev.branny.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.gamemodes.GamemodeRegistry;
import dev.branny.hytale.gamemodes.arena.ArenaGamemode;
import dev.branny.hytale.gamemodes.sniperwars.SniperWarsGamemode;
import dev.branny.hytale.gamemodes.survival.SurvivalGamemode;
import dev.branny.hytale.pvptools.combat.CombatListener;
import dev.branny.hytale.pvptools.loadout.LoadoutRegistry;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.servercore.ServerCore;
import dev.branny.hytale.servercore.lobby.LobbyCommands;
import dev.branny.hytale.servercore.lobby.LobbyConfig;
import dev.branny.hytale.servercore.lobby.LobbyManager;
import dev.branny.hytale.servercore.persistence.GamemodeInventoryManager;
import dev.branny.hytale.servercore.persistence.PlayerDataComponent;
import dev.branny.hytale.servercore.persistence.PlayerDataService;
import dev.branny.hytale.servercore.player.PlayerSession;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin entry point for the Hytale Minigame Server.
 * 
 * This plugin provides a modular minigame framework with:
 * - Lobby system with queue management
 * - PVP tools (loadouts, combat tracking, match lifecycle)
 * - Multiple gamemodes (Arena, Sniper Wars, etc.)
 */
public class BrannyPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /** Tracks players who have completed their initial join */
    private final Set<UUID> initialJoinHandled = ConcurrentHashMap.newKeySet();

    public BrannyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initializing " + this.getName() + " version " + 
            this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up " + this.getName());
        
        // ==================== Core Systems ====================
        
        // Initialize server core
        ServerCore.initialize();
        
        // Register player data component for persistence
        var playerDataComponentType = getEntityStoreRegistry().registerComponent(
            PlayerDataComponent.class,
            PlayerDataComponent::new
        );
        PlayerDataService.setComponentType(playerDataComponentType);
        LOGGER.atInfo().log("Registered PlayerDataComponent for persistence");
        
        // Register default loadouts
        LoadoutRegistry.registerDefaults();
        LOGGER.atInfo().log("Registered " + LoadoutRegistry.getLoadoutCount() + " loadouts");
        
        // Register combat listener for kill tracking
        getEntityStoreRegistry().registerSystem(new CombatListener());
        LOGGER.atInfo().log("Registered CombatListener system");
        
        // Initialize match manager
        MatchManager.initialize();
        
        // ==================== Gamemodes ====================
        
        // Register gamemodes
        GamemodeRegistry.register(new ArenaGamemode(), getCommandRegistry());
        GamemodeRegistry.register(new SniperWarsGamemode(), getCommandRegistry());
        GamemodeRegistry.register(new SurvivalGamemode(), getCommandRegistry());
        LOGGER.atInfo().log("Registered " + GamemodeRegistry.getCount() + " gamemodes");
        
        // ==================== Global Commands ====================
        
        // Register lobby commands
        getCommandRegistry().registerCommand(new LobbyCommands());
        
        LOGGER.atInfo().log("Plugin setup complete");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting " + this.getName());
        
        // Initialize lobby manager (starts queue processor)
        LobbyManager.initialize();
        
        // Register event listeners
        // PlayerConnectEvent fires before player enters any world - use Holder to init data
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        
        LOGGER.atInfo().log("Plugin started successfully");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down " + this.getName());
        
        // Shutdown systems in reverse order
        LobbyManager.shutdown();
        MatchManager.shutdown();
        GamemodeRegistry.shutdown();
        ServerCore.shutdown();
        
        LOGGER.atInfo().log("Plugin shutdown complete");
    }

    // ==================== Event Handlers ====================

    /**
     * Handles player connect events (before entering any world).
     * Initializes persistent data using the Holder so it survives world transfers.
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        // Initialize PlayerDataComponent via Holder - this is the earliest reliable point
        // and ensures the component exists before any world transfers
        // Also caches it by UUID for reliable access when player is in a world
        PlayerDataService.initFromHolder(event.getHolder(), playerRef.getUuid(), playerRef.getUsername());
    }

    /**
     * Handles player ready events.
     * On initial join, routes the player to the lobby.
     */
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            LOGGER.atWarning().log("PlayerReadyEvent received with null player");
            return;
        }

        UUID playerUuid = player.getUuid();
        
        // Create session
        @SuppressWarnings("deprecation")
        PlayerRef playerRef = player.getPlayerRef();
        PlayerSession.getOrCreate(playerRef);
        
        // Initialize persistent player data
        PlayerDataService.onPlayerConnect(playerRef);

        // Check if we've already handled this player's initial join
        if (!initialJoinHandled.add(playerUuid)) {
            // Already handled - this is a world change, not initial join
            return;
        }

        LOGGER.atInfo().log("Player " + playerRef.getUsername() + " initial join - routing to lobby");

        World currentWorld = WorldTransferService.getCurrentWorld(playerRef);
        if (currentWorld == null) {
            LOGGER.atWarning().log("Player has no current world");
            return;
        }

        // If already in lobby, just teleport to spawn
        if (LobbyConfig.LOBBY_WORLD_NAME.equals(currentWorld.getName())) {
            WorldTransferService.teleport(playerRef, LobbyConfig.getLobbySpawn());
            return;
        }

        // Transfer to lobby with short delay for client fade
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
            World worldAfterDelay = WorldTransferService.getCurrentWorld(playerRef);
            if (worldAfterDelay == null) {
                return;
            }

            if (LobbyConfig.LOBBY_WORLD_NAME.equals(worldAfterDelay.getName())) {
                WorldTransferService.teleport(playerRef, LobbyConfig.getLobbySpawn());
                return;
            }

            worldAfterDelay.execute(() -> {
                WorldTransferService.transferWithTitle(
                    playerRef,
                    LobbyConfig.LOBBY_WORLD_NAME,
                    LobbyConfig.getLobbySpawn(),
                    "Welcome!",
                    "Use /lobby games to see available modes!"
                ).whenComplete((transferred, error) -> {
                    if (error != null) {
                        LOGGER.atWarning().log("Failed to transfer to lobby: " + error.getMessage());
                    }
                });
            });
        });
    }

    /**
     * Handles player disconnect events.
     * Cleans up session and queue state, saves persistent inventory.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        
        // Save inventory if in a persistent gamemode (belt-and-suspenders with transition save)
        PlayerSession session = PlayerSession.get(playerId);
        if (session != null) {
            String currentGamemodeId = session.getCurrentGamemode();
            if (currentGamemodeId != null) {
                Gamemode gamemode = GamemodeRegistry.get(currentGamemodeId);
                if (gamemode != null && gamemode.hasPersistentInventory()) {
                    GamemodeInventoryManager.saveCurrentInventory(playerRef, currentGamemodeId);
                    LOGGER.atInfo().log("Saved persistent inventory on disconnect for " + playerRef.getUsername());
                }
            }
        }
        
        // Clean up tracking state
        initialJoinHandled.remove(playerId);
        
        // Remove from queue if queued
        LobbyManager.leaveQueue(playerId);
        
        // Remove from match if in one
        MatchManager.removePlayerFromMatch(playerId);
        
        // Update persistent player data
        PlayerDataService.onPlayerDisconnect(playerRef);
        
        // Clean up session
        ServerCore.onPlayerDisconnect(playerId);
        
        LOGGER.atInfo().log("Player " + playerRef.getUsername() + " disconnected - cleaned up state");
    }
}
