package dev.branny.hytale.servercore.lobby;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.gamemodes.GamemodeRegistry;
import dev.branny.hytale.pvptools.match.Match;
import dev.branny.hytale.pvptools.match.MatchManager;
import dev.branny.hytale.servercore.player.PlayerSession;
import dev.branny.hytale.servercore.world.WorldTransferService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages the lobby system, player queuing, and matchmaking.
 */
public final class LobbyManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Queues per gamemode (gamemodeId -> queue of players)
    private static final Map<String, Queue<QueueEntry>> queues = new ConcurrentHashMap<>();
    
    // Player to queue mapping
    private static final Map<UUID, QueueEntry> playerQueues = new ConcurrentHashMap<>();
    
    // Background queue processor
    private static ScheduledExecutorService queueProcessor;

    private LobbyManager() {
        // Utility class
    }

    // ==================== Initialization ====================

    /**
     * Initializes the lobby manager.
     * Starts the background queue processor.
     */
    public static void initialize() {
        // Initialize queues for all registered gamemodes
        for (String gamemodeId : GamemodeRegistry.getRegisteredIds()) {
            queues.put(gamemodeId, new ConcurrentLinkedQueue<>());
        }

        // Start queue processor
        queueProcessor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LobbyQueueProcessor");
            t.setDaemon(true);
            return t;
        });
        queueProcessor.scheduleAtFixedRate(
            LobbyManager::processQueues,
            LobbyConfig.QUEUE_CHECK_INTERVAL_MS,
            LobbyConfig.QUEUE_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        LOGGER.atInfo().log("LobbyManager initialized with " + queues.size() + " queue(s)");
    }

    /**
     * Shuts down the lobby manager.
     */
    public static void shutdown() {
        if (queueProcessor != null) {
            queueProcessor.shutdown();
            try {
                queueProcessor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Clear all queues
        queues.clear();
        playerQueues.clear();
        LOGGER.atInfo().log("LobbyManager shutdown");
    }

    // ==================== Queue Management ====================

    /**
     * Adds a player to a gamemode queue.
     *
     * @param playerRef the player to queue
     * @param gamemodeId the gamemode to queue for
     * @return true if successfully queued
     */
    public static boolean joinQueue(@Nonnull PlayerRef playerRef, @Nonnull String gamemodeId) {
        UUID playerId = playerRef.getUuid();

        // Check if already in queue
        if (playerQueues.containsKey(playerId)) {
            playerRef.sendMessage(Message.raw("You are already in a queue!"));
            return false;
        }

        // Check if in a match
        if (MatchManager.isInMatch(playerId)) {
            playerRef.sendMessage(Message.raw("You cannot queue while in a match!"));
            return false;
        }

        // Check if gamemode exists
        Gamemode gamemode = GamemodeRegistry.get(gamemodeId);
        if (gamemode == null) {
            playerRef.sendMessage(Message.raw("Unknown gamemode: " + gamemodeId));
            return false;
        }

        // Ensure queue exists
        queues.computeIfAbsent(gamemodeId, k -> new ConcurrentLinkedQueue<>());

        // Create queue entry
        QueueEntry entry = new QueueEntry(playerRef, gamemodeId);
        queues.get(gamemodeId).offer(entry);
        playerQueues.put(playerId, entry);

        // Update session
        PlayerSession session = PlayerSession.get(playerId);
        if (session != null) {
            session.joinQueue(gamemodeId);
        }

        LOGGER.atInfo().log(playerRef.getUsername() + " joined queue for " + gamemodeId);
        playerRef.sendMessage(Message.raw("Joined queue for " + gamemode.getDisplayName() + "!"));
        
        // Immediately try to match
        tryMatchPlayers(gamemodeId);
        
        return true;
    }

    /**
     * Removes a player from their current queue.
     *
     * @param playerId the player's UUID
     * @return true if was in queue and removed
     */
    public static boolean leaveQueue(@Nonnull UUID playerId) {
        QueueEntry entry = playerQueues.remove(playerId);
        if (entry == null) {
            return false;
        }

        Queue<QueueEntry> queue = queues.get(entry.getGamemodeId());
        if (queue != null) {
            queue.remove(entry);
        }

        // Update session
        PlayerSession session = PlayerSession.get(playerId);
        if (session != null) {
            session.leaveQueue();
        }

        entry.getPlayerRef().sendMessage(Message.raw("Left the queue."));
        LOGGER.atInfo().log(entry.getUsername() + " left queue for " + entry.getGamemodeId());
        return true;
    }

    /**
     * Gets the queue entry for a player.
     *
     * @param playerId the player's UUID
     * @return the queue entry, or null if not in queue
     */
    @Nullable
    public static QueueEntry getQueueEntry(@Nonnull UUID playerId) {
        return playerQueues.get(playerId);
    }

    /**
     * Checks if a player is in any queue.
     *
     * @param playerId the player's UUID
     * @return true if in queue
     */
    public static boolean isInQueue(@Nonnull UUID playerId) {
        return playerQueues.containsKey(playerId);
    }

    /**
     * Gets the queue size for a gamemode.
     *
     * @param gamemodeId the gamemode
     * @return queue size
     */
    public static int getQueueSize(@Nonnull String gamemodeId) {
        Queue<QueueEntry> queue = queues.get(gamemodeId);
        return queue != null ? queue.size() : 0;
    }

    // ==================== Queue Processing ====================

    /**
     * Processes all queues looking for matches.
     * Called periodically by the background processor.
     */
    private static void processQueues() {
        try {
            // Clean up disconnected players
            cleanupDisconnectedPlayers();

            // Try to form matches for each gamemode
            for (String gamemodeId : queues.keySet()) {
                tryMatchPlayers(gamemodeId);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error processing queues: " + e.getMessage());
        }
    }

    /**
     * Removes disconnected players from queues.
     */
    private static void cleanupDisconnectedPlayers() {
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, QueueEntry> entry : playerQueues.entrySet()) {
            if (!entry.getValue().isStillConnected()) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID playerId : toRemove) {
            QueueEntry entry = playerQueues.remove(playerId);
            if (entry != null) {
                Queue<QueueEntry> queue = queues.get(entry.getGamemodeId());
                if (queue != null) {
                    queue.remove(entry);
                }
                LOGGER.atInfo().log("Removed disconnected player from queue: " + entry.getUsername());
            }
        }
    }

    /**
     * Tries to form a match for a gamemode.
     *
     * @param gamemodeId the gamemode
     */
    private static void tryMatchPlayers(@Nonnull String gamemodeId) {
        Gamemode gamemode = GamemodeRegistry.get(gamemodeId);
        if (gamemode == null) {
            return;
        }

        Queue<QueueEntry> queue = queues.get(gamemodeId);
        if (queue == null || queue.size() < gamemode.getMinPlayers()) {
            return;
        }

        // Collect players for the match
        List<QueueEntry> matchPlayers = new ArrayList<>();
        int playersNeeded = Math.min(gamemode.getMaxPlayers(), queue.size());

        // Take players from the queue
        for (int i = 0; i < playersNeeded && !queue.isEmpty(); i++) {
            QueueEntry entry = queue.poll();
            if (entry != null && entry.isStillConnected()) {
                matchPlayers.add(entry);
            }
        }

        // Check if we have enough players
        if (matchPlayers.size() < gamemode.getMinPlayers()) {
            // Put players back in queue
            for (QueueEntry entry : matchPlayers) {
                queue.offer(entry);
            }
            return;
        }

        // Remove players from the player map
        for (QueueEntry entry : matchPlayers) {
            playerQueues.remove(entry.getPlayerId());
            PlayerSession session = PlayerSession.get(entry.getPlayerId());
            if (session != null) {
                session.leaveQueue();
            }
        }

        // Create the match
        List<PlayerRef> players = matchPlayers.stream()
            .map(QueueEntry::getPlayerRef)
            .toList();

        startMatch(gamemode, players);
    }

    /**
     * Starts a match with the given players.
     */
    private static void startMatch(@Nonnull Gamemode gamemode, @Nonnull List<PlayerRef> players) {
        LOGGER.atInfo().log("Starting " + gamemode.getId() + " match with " + players.size() + " players");

        try {
            // Create match through gamemode
            Match match = gamemode.createMatch(players);
            if (match == null) {
                LOGGER.atWarning().log("Gamemode returned null match");
                return;
            }

            // Register match
            MatchManager.registerMatch(match);

            // Notify players
            for (PlayerRef player : players) {
                player.sendMessage(Message.raw("Match found! Starting " + gamemode.getDisplayName() + "..."));
            }

            // Start the match
            match.start();

        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to start match: " + e.getMessage());
            // Return players to queue
            for (PlayerRef player : players) {
                joinQueue(player, gamemode.getId());
            }
        }
    }

    // ==================== Lobby Transfer ====================

    /**
     * Transfers a player to the lobby world.
     *
     * @param playerRef the player to transfer
     * @return CompletableFuture that completes when transfer is done
     */
    @Nonnull
    public static CompletableFuture<PlayerRef> transferToLobby(@Nonnull PlayerRef playerRef) {
        return WorldTransferService.transferWithTitle(
            playerRef,
            LobbyConfig.LOBBY_WORLD_NAME,
            LobbyConfig.getLobbySpawn(),
            "Lobby",
            "Welcome back!"
        );
    }

    /**
     * Called when a player returns to the lobby (e.g., after a match).
     *
     * @param playerRef the player
     */
    public static void onPlayerReturnToLobby(@Nonnull PlayerRef playerRef) {
        PlayerSession session = PlayerSession.get(playerRef.getUuid());
        if (session != null) {
            session.setInLobby(true);
            session.clearCurrentMatch();
        }
        LOGGER.atInfo().log(playerRef.getUsername() + " returned to lobby");
    }
}
