package dev.branny.hytale;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.MaybeBool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.branny.hytale.utilities.ItemUtilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TitleBroadcastCommand extends AbstractCommand {
    private static final Message TITLE = createTitle();
    private static final String WHITE = "#FFFFFF";

    private final String headerText;
    private final String subtitleText;

    @Nonnull
    private final RequiredArg<List<String>> messageArg = withListRequiredArg("message", "The message that gets sent", ArgTypes.STRING);

    public TitleBroadcastCommand(String header, String subtitle) {
        super("broadcast", "Sends a title broadcast", false);
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP

        this.headerText = header;
        this.subtitleText = subtitle;

        addAliases("bc");
    }

    private static Message createTitle() {
        FormattedMessage celestial = new FormattedMessage();
        celestial.rawText = "Celestial";
        celestial.color = "#87CEEB";

        FormattedMessage hytale = new FormattedMessage();
        hytale.rawText = "Hytale";
        hytale.color = "#00CED1";

        FormattedMessage message = new FormattedMessage();
        message.bold = MaybeBool.True;
        message.children = new FormattedMessage[] { celestial, hytale };

        return new Message(message);
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
        // Message header = createHeader(headerText);
        Message subtitle = createSubtitle(subtitleText);

        Universe.get().getPlayers().forEach(playerRef ->
                EventTitleUtil.showEventTitleToPlayer(playerRef, TITLE, subtitle, true)
        );

        // Grant the player some poop
        if (commandContext.isPlayer()){
            Player player = (Player) commandContext.sender();
            ItemUtilities.giveItemToHotbar(player, new ItemStack("Ingredient_Poop", 1));
        }

        return CompletableFuture.completedFuture(null);
    }
}