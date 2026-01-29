package com.bumenfeld;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;

import java.util.concurrent.CompletableFuture;

public final class DeathAnnouncerReloadCommand extends AbstractCommand {
    private final DeathAnnouncer announcer;

    public DeathAnnouncerReloadCommand(DeathAnnouncer announcer) {
        super("deathreload", "Reloads the death announcer configuration");
        setAllowsExtraArguments(false);
        this.announcer = announcer;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        CommandSender sender = context.sender();
        announcer.reloadConfiguration(sender);
        return CompletableFuture.completedFuture(null);
    }
}
