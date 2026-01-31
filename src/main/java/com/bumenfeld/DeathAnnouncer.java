package com.bumenfeld;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        String uiPath = "death_notification.ui";
        deathAnnouncementSystem = new DeathAnnouncementSystem(bundle, config.areNotificationsEnabled(),
            config.areChatNotificationsEnabled(), uiPath, config.getHudDisplaySeconds(),
            config.areHudNotificationsEnabled());
        currentConfig = config;
        getEntityStoreRegistry().registerSystem(deathAnnouncementSystem);
        getCommandRegistry().registerCommand(new DeathNotificationCommand(this, deathAnnouncementSystem));
    }

    @Override
    protected void shutdown() {
        if (deathAnnouncementSystem != null) {
            deathAnnouncementSystem.shutdown();
        }
        super.shutdown();
    }

    private void ensureDefaultData(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create plugin data directory", e);
        }

        ensureResourceCopy(dataDirectory.resolve("config.yml"), "config/config.yml");
        ensureLocalizationData(dataDirectory);
    }

    private void ensureResourceCopy(Path target, String resourcePath) {
        if (Files.exists(target)) {
            return;
        }

        copyResource(target, resourcePath, false);
    }

    private void ensureLocalizationData(Path dataDirectory) {
        Path localizationDir = dataDirectory.resolve("localization");
        try {
            Files.createDirectories(localizationDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create localization directory", e);
        }

        String currentBuildId = readBundledBuildId();
        Path versionFile = dataDirectory.resolve(".plugin-version");
        String previousBuildId = readVersionFile(versionFile);
        boolean forceUpdate = currentBuildId != null && !currentBuildId.equals(previousBuildId);

        copyResource(localizationDir.resolve("en.json"), "localization/en.json", forceUpdate);
        copyResource(localizationDir.resolve("de.json"), "localization/de.json", forceUpdate);

        if (currentBuildId != null) {
            try {
                Files.writeString(versionFile, currentBuildId);
            } catch (IOException e) {
                LOGGER.warning("Unable to update plugin version file: " + e.getMessage());
            }
        }
    }

    private void copyResource(Path target, String resourcePath, boolean overwrite) {
        if (!overwrite && Files.exists(target)) {
            return;
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.warning(() -> "Missing bundled resource: " + resourcePath);
                return;
            }
            if (overwrite) {
                Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(stream, target);
            }
        } catch (IOException e) {
            LOGGER.warning("Unable to export " + resourcePath + ": " + e.getMessage());
        }
    }

    private String readBundledBuildId() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("manifest.json")) {
            if (stream == null) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(stream);
            JsonNode buildId = root.path("Build").path("Id");
            if (buildId.isTextual() && !buildId.asText().isBlank()) {
                return buildId.asText();
            }
            JsonNode version = root.path("Version");
            if (version.isTextual() && !version.asText().isBlank()) {
                return version.asText();
            }
        } catch (IOException ignored) {
            // Ignore and fall back to no version tracking.
        }
        return null;
    }

    private String readVersionFile(Path versionFile) {
        if (!Files.exists(versionFile)) {
            return null;
        }
        try {
            String text = Files.readString(versionFile).trim();
            return text.isBlank() ? null : text;
        } catch (IOException ignored) {
            return null;
        }
    }

    public void reloadConfiguration(CommandSender sender) {
        DeathAnnouncerConfig config = DeathAnnouncerConfig.load(getDataDirectory());
        LocalizationBundle bundle = localizationManager.load(config.getLanguage());
        deathAnnouncementSystem.updateLocalizationBundle(bundle);
        deathAnnouncementSystem.setNotificationsEnabled(config.areNotificationsEnabled());
        deathAnnouncementSystem.setChatNotificationsEnabled(config.areChatNotificationsEnabled());
        deathAnnouncementSystem.setHudNotificationsEnabled(config.areHudNotificationsEnabled());
        deathAnnouncementSystem.setHudDisplaySeconds(config.getHudDisplaySeconds());
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
