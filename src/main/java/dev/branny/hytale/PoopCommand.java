package dev.branny.hytale;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.TempAssetIdUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.branny.hytale.utilities.ItemUtilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class PoopCommand extends AbstractCommand {
    private static final String WHITE = "#FFFFFF";


    public PoopCommand() {
        super("poop", "Gives the player some poop", false);
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP

        addAliases("bc");
    }

    private static Message createHeader(String text) {
        FormattedMessage header = new FormattedMessage();
        header.rawText = text;
        header.color = WHITE;
        return new Message(header);
    }

    private static Message createSubtitle(String text) {
        FormattedMessage subtitle = new FormattedMessage();
        subtitle.rawText = text;
        subtitle.color = WHITE;
        return new Message(subtitle);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext commandContext) {
        Message title = createHeader("BY THE POWER OF POOP");
        Message subtitle = createSubtitle("May the Poop guide you well, young adventurer...");

        EventTitleUtil.showEventTitleToUniverse(title, subtitle, true, "Poop", 8.0f, 1.5f, 2.0f);

        // Grant the player some poop
        if (commandContext.isPlayer()){
            Player player = (Player) commandContext.sender();

            ItemUtilities.giveItemToHotbar(player, new ItemStack("Ingredient_Poop", 3));

            // Play pickup sound to the player
            SoundUtil.playSoundEvent2dToPlayer(player.getPlayerRef(), TempAssetIdUtil.getSoundEventIndex("SFX_Divine_Respawn"), SoundCategory.UI);
        }

        return CompletableFuture.completedFuture(null);
    }
}