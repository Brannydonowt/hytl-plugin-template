package dev.branny.hytale.gamemodes.sniperwars;

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
 * Sniper Wars gamemode - Free-for-all sniper battles.
 * First player to reach the kill target wins!
 */
public class SniperWarsGamemode implements Gamemode {

    public static final String ID = "sniperwars";
    public static final String DISPLAY_NAME = "Sniper Wars";
    public static final String DESCRIPTION = "Free-for-all sniper battle! First to " + 
        SniperWarsConfig.KILLS_TO_WIN + " kills wins!";

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
        return SniperWarsConfig.MIN_PLAYERS;
    }

    @Override
    public int getMaxPlayers() {
        return SniperWarsConfig.MAX_PLAYERS;
    }

    @Override
    @Nonnull
    public String getWorldName() {
        return SniperWarsConfig.WORLD_NAME;
    }

    @Override
    @Nonnull
    public Match createMatch(@Nonnull List<PlayerRef> players) {
        return new SniperWarsMatch(players);
    }

    @Override
    @Nullable
    public Loadout getDefaultLoadout() {
        return LoadoutRegistry.get(SniperWarsConfig.LOADOUT_ID);
    }

    @Override
    public void registerCommands(@Nonnull CommandRegistry registry) {
        registry.registerCommand(new SniperWarsCommands());
    }

    @Override
    public void onRegister() {
        // Sniper wars loadout is registered in LoadoutRegistry.registerDefaults()
    }
}
