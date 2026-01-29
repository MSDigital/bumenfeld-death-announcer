package com.bumenfeld;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class DeathAnnouncer extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(DeathAnnouncer.class.getName());
    private DeathAnnouncementSystem deathAnnouncementSystem;
    private LocalizationManager localizationManager;
    private DeathAnnouncerConfig currentConfig;

    public DeathAnnouncer(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        Path dataDirectory = getDataDirectory();
        ensureDefaultData(dataDirectory);
        DeathAnnouncerConfig config = DeathAnnouncerConfig.load(dataDirectory);
        localizationManager = new LocalizationManager(dataDirectory);
        LocalizationBundle bundle = localizationManager.load(config.getLanguage());
        deathAnnouncementSystem = new DeathAnnouncementSystem(bundle, config.areNotificationsEnabled(),
            config.areChatNotificationsEnabled());
        currentConfig = config;
        getEntityStoreRegistry().registerSystem(deathAnnouncementSystem);
        getCommandRegistry().registerCommand(new DeathNotificationCommand(this, deathAnnouncementSystem));
    }

    private void ensureDefaultData(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create plugin data directory", e);
        }

        ensureResourceCopy(dataDirectory.resolve("config.yml"), "config/config.yml");
        Path localizationDir = dataDirectory.resolve("localization");
        try {
            Files.createDirectories(localizationDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create localization directory", e);
        }
        ensureResourceCopy(localizationDir.resolve("en.json"), "localization/en.json");
        ensureResourceCopy(localizationDir.resolve("de.json"), "localization/de.json");
    }

    private void ensureResourceCopy(Path target, String resourcePath) {
        if (Files.exists(target)) {
            return;
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.warning(() -> "Missing bundled resource: " + resourcePath);
                return;
            }
            Files.copy(stream, target);
        } catch (IOException e) {
            LOGGER.warning("Unable to export " + resourcePath + ": " + e.getMessage());
        }
    }

    public void reloadConfiguration(CommandSender sender) {
        DeathAnnouncerConfig config = DeathAnnouncerConfig.load(getDataDirectory());
        LocalizationBundle bundle = localizationManager.load(config.getLanguage());
        deathAnnouncementSystem.updateLocalizationBundle(bundle);
        deathAnnouncementSystem.setNotificationsEnabled(config.areNotificationsEnabled());
        deathAnnouncementSystem.setChatNotificationsEnabled(config.areChatNotificationsEnabled());
        currentConfig = config;
        String feedback = String.format("Death announcer configuration reloaded (language=%s)", config.getLanguage());
        Message message = Message.raw(feedback);
        if (sender != null) {
            sender.sendMessage(message);
        }
        LOGGER.info(feedback);
    }

    public DeathAnnouncerConfig getCurrentConfig() {
        return currentConfig;
    }
}
