package com.bumenfeld;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DeathNotificationCommand extends AbstractCommand {
    private final DeathAnnouncementSystem announcementSystem;
    private final ScheduledExecutorService scheduler;

    public DeathNotificationCommand(DeathAnnouncementSystem announcementSystem) {
        super("deathnotify", "Simulate all death notifications once");
        this.announcementSystem = announcementSystem;
        setAllowsExtraArguments(true);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "death-notify-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        CommandSender sender = context.sender();
        String playerName = sender != null ? sender.getDisplayName() : "DeathTester";
        context.sendMessage(Message.raw("Broadcasting all death notifications..."));
        List<String> orderedCauses = new java.util.ArrayList<>();
        announcementSystem.getSupportedCauses().forEach(orderedCauses::add);
        orderedCauses = List.copyOf(orderedCauses);
        for (int i = 0; i < orderedCauses.size(); i++) {
            String cause = orderedCauses.get(i);
            long delaySeconds = i * 5L;
            scheduler.schedule(() -> announcementSystem.triggerNotification(cause, playerName),
                delaySeconds, TimeUnit.SECONDS);
        }
        return CompletableFuture.completedFuture(null);
    }
}
