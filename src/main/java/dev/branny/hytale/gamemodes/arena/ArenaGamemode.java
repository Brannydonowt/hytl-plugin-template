package dev.branny.hytale.gamemodes.arena;

import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import dev.branny.hytale.gamemodes.Gamemode;
import dev.branny.hytale.pvptools.loadout.Loadout;
import dev.branny.hytale.pvptools.loadout.LoadoutRegistry;
import dev.branny.hytale.pvptools.match.Match;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Arena gamemode - 1v1 PvP battles.
 * First player to eliminate their opponent wins.
 */
public class ArenaGamemode implements Gamemode {

    public static final String ID = "arena";
    public static final String DISPLAY_NAME = "1v1 Arena";
    public static final String DESCRIPTION = "Battle your opponent in a 1v1 fight to the death!";

    @Override
    @Nonnull
    public String getId() {
        return ID;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    @Nonnull
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public int getMinPlayers() {
        return ArenaConfig.MIN_PLAYERS;
    }

    @Override
    public int getMaxPlayers() {
        return ArenaConfig.MAX_PLAYERS;
    }

    @Override
    @Nonnull
    public String getWorldName() {
        return ArenaConfig.WORLD_NAME;
    }

    @Override
    @Nonnull
    public Match createMatch(@Nonnull List<PlayerRef> players) {
        return new ArenaMatch(players);
    }

    @Override
    @Nullable
    public Loadout getDefaultLoadout() {
        return LoadoutRegistry.get(ArenaConfig.LOADOUT_ID);
    }

    @Override
    public void registerCommands(@Nonnull CommandRegistry registry) {
        registry.registerCommand(new ArenaCommands());
    }

    @Override
    public void onRegister() {
        // Arena loadout is registered in LoadoutRegistry.registerDefaults()
    }
}
