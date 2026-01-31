package com.bumenfeld;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DeathNotificationCommand extends AbstractCommand {
    private static final String ADMIN_PERMISSION = "com.bumenfeld.deathnotification.admin";
    private static final Set<String> CONFIG_OPTIONS = Set.of(
        "language",
        "notifications",
        "chat-notifications",
        "hud-display-seconds",
        "hud-notifications"
    );

    private final DeathAnnouncer announcer;
    private final DeathAnnouncementSystem announcementSystem;
    private final ScheduledExecutorService scheduler;

    public DeathNotificationCommand(DeathAnnouncer announcer, DeathAnnouncementSystem announcementSystem) {
        super("deathnotification", "Manage death notifications");
        requirePermission(ADMIN_PERMISSION);
        this.announcer = announcer;
        this.announcementSystem = announcementSystem;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "death-notify-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        addSubCommand(new ConfigSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new TestSubCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        context.sendMessage(Message.raw("Usage: /deathnotification <config|reload|test>"));
        return CompletableFuture.completedFuture(null);
    }

    private final class ConfigSubCommand extends AbstractCommand {
        private final RequiredArg<String> optionArg;
        private final RequiredArg<String> valueArg;

        private ConfigSubCommand() {
            super("config", "Adjust death notification configuration values");
            optionArg = withRequiredArg("option", "Config option (language/notifications/chat-notifications/hud-display-seconds/hud-notifications)",
                ArgTypes.STRING);
            valueArg = withRequiredArg("value", "New value for the option", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String option = context.get(optionArg).toLowerCase(Locale.ROOT);
            String value = context.get(valueArg);

            if (!CONFIG_OPTIONS.contains(option)) {
                context.sendMessage(Message.raw("Unknown option '" + option + "'. Available: " + CONFIG_OPTIONS));
                return CompletableFuture.completedFuture(null);
            }

            DeathAnnouncerConfig current = announcer.getCurrentConfig();
            if (current == null) {
                context.sendMessage(Message.raw("Configuration is not yet loaded."));
                return CompletableFuture.completedFuture(null);
            }

            DeathAnnouncerConfig updated = switch (option) {
                case "language" -> {
                    String trimmed = value.trim();
                    if (trimmed.isEmpty()) {
                        context.sendMessage(Message.raw("Language cannot be empty."));
                        yield null;
                    }
                    yield current.withLanguage(trimmed);
                }
                case "notifications" -> {
                    Boolean flag = parseBoolean(value);
                    if (flag == null) {
                        context.sendMessage(Message.raw("Expected true/false for notifications."));
                        yield null;
                    }
                    yield current.withNotifications(flag);
                }
                case "chat-notifications" -> {
                    Boolean flag = parseBoolean(value);
                    if (flag == null) {
                        context.sendMessage(Message.raw("Expected true/false for chat-notifications."));
                        yield null;
                    }
                    yield current.withChatNotifications(flag);
                }
                case "hud-display-seconds" -> {
                    Long seconds = parseLong(value);
                    if (seconds == null || seconds < 1) {
                        context.sendMessage(Message.raw("Expected a number >= 1 for hud-display-seconds."));
                        yield null;
                    }
                    yield current.withHudDisplaySeconds(seconds);
                }
                case "hud-notifications" -> {
                    Boolean flag = parseBoolean(value);
                    if (flag == null) {
                        context.sendMessage(Message.raw("Expected true/false for hud-notifications."));
                        yield null;
                    }
                    yield current.withHudNotifications(flag);
                }
                default -> null;
            };

            if (updated == null) {
                return CompletableFuture.completedFuture(null);
            }

            try {
                updated.save(announcer.getDataDirectory());
            } catch (IOException exception) {
                context.sendMessage(Message.raw("Failed to persist configuration: " + exception.getMessage()));
                return CompletableFuture.completedFuture(null);
            }

            announcer.reloadConfiguration(context.sender());
            context.sendMessage(Message.raw("Set " + option + " to '" + value + "'."));
            return CompletableFuture.completedFuture(null);
        }

        private Boolean parseBoolean(String value) {
            if (value == null) {
                return null;
            }

            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "true", "yes", "y", "1", "on" -> true;
                case "false", "no", "n", "0", "off" -> false;
                default -> null;
            };
        }

        private Long parseLong(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    private final class ReloadSubCommand extends AbstractCommand {
        private ReloadSubCommand() {
            super("reload", "Reload death notification configuration");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            announcer.reloadConfiguration(context.sender());
            context.sendMessage(Message.raw("Death announcer configuration reloaded."));
            return CompletableFuture.completedFuture(null);
        }
    }

    private final class TestSubCommand extends AbstractCommand {
        private final List<String> orderedCauses;

        private TestSubCommand() {
            super("test", "Simulate all death notifications");
            orderedCauses = new ArrayList<>();
            announcementSystem.getSupportedCauses().forEach(orderedCauses::add);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            CommandSender sender = context.sender();
            String playerName = sender != null ? sender.getDisplayName() : "DeathTester";
            context.sendMessage(Message.raw("Broadcasting all death notifications..."));

            for (int i = 0; i < orderedCauses.size(); i++) {
                String cause = orderedCauses.get(i);
                long delaySeconds = i * 2L;
                scheduler.schedule(() -> announcementSystem.triggerNotification(cause, playerName),
                    delaySeconds, TimeUnit.SECONDS);
            }

            return CompletableFuture.completedFuture(null);
        }
    }
}
