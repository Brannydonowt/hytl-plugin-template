package dev.branny.hytale;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class BrannyCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;

    public BrannyCommand(String pluginName, String pluginVersion) {
        super("branny", "Test console command authored by Branny.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hello Branny, everything worked fine! You are the best! From the " + pluginName + " v" + pluginVersion + " plugin!"));
    }
}
