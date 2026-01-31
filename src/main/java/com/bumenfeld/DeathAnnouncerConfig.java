package com.bumenfeld;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DeathAnnouncerConfig {
    private static final String DEFAULT_LANGUAGE = "en";
    private static final boolean DEFAULT_NOTIFICATIONS = true;
    private static final boolean DEFAULT_CHAT_NOTIFICATIONS = true;
    private static final boolean DEFAULT_HUD_NOTIFICATIONS = true;
    private static final long DEFAULT_HUD_DISPLAY_SECONDS = 4;
    private final String language;
    private final boolean notificationsEnabled;
    private final boolean chatNotificationsEnabled;
    private final boolean hudNotificationsEnabled;
    private final long hudDisplaySeconds;

    private DeathAnnouncerConfig(String language,
                                 boolean notificationsEnabled,
                                 boolean chatNotificationsEnabled,
                                 boolean hudNotificationsEnabled,
                                 long hudDisplaySeconds) {
        this.language = language;
        this.notificationsEnabled = notificationsEnabled;
        this.chatNotificationsEnabled = chatNotificationsEnabled;
        this.hudNotificationsEnabled = hudNotificationsEnabled;
        this.hudDisplaySeconds = hudDisplaySeconds;
    }

    public String getLanguage() {
        return language;
    }

    public boolean areNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean areChatNotificationsEnabled() {
        return chatNotificationsEnabled;
    }

    public boolean areHudNotificationsEnabled() {
        return hudNotificationsEnabled;
    }

    public long getHudDisplaySeconds() {
        return hudDisplaySeconds;
    }

    public DeathAnnouncerConfig withLanguage(String newLanguage) {
        return new DeathAnnouncerConfig(newLanguage, notificationsEnabled, chatNotificationsEnabled,
            hudNotificationsEnabled, hudDisplaySeconds);
    }

    public DeathAnnouncerConfig withNotifications(boolean enabled) {
        return new DeathAnnouncerConfig(language, enabled, chatNotificationsEnabled, hudNotificationsEnabled,
            hudDisplaySeconds);
    }

    public DeathAnnouncerConfig withChatNotifications(boolean enabled) {
        return new DeathAnnouncerConfig(language, notificationsEnabled, enabled, hudNotificationsEnabled,
            hudDisplaySeconds);
    }

    public DeathAnnouncerConfig withHudNotifications(boolean enabled) {
        return new DeathAnnouncerConfig(language, notificationsEnabled, chatNotificationsEnabled, enabled,
            hudDisplaySeconds);
    }

    public DeathAnnouncerConfig withHudDisplaySeconds(long seconds) {
        return new DeathAnnouncerConfig(language, notificationsEnabled, chatNotificationsEnabled, hudNotificationsEnabled,
            seconds);
    }

    public void save(Path pluginDataDirectory) throws IOException {
        Path configFile = pluginDataDirectory.resolve("config.yml");
        Files.createDirectories(configFile.getParent());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("language", language);
        values.put("notifications", notificationsEnabled);
        values.put("chat-notifications", chatNotificationsEnabled);
        values.put("hud-notifications", hudNotificationsEnabled);
        values.put("hud-display-seconds", hudDisplaySeconds);
        Yaml yaml = new Yaml();
        try (var writer = Files.newBufferedWriter(configFile)) {
            yaml.dump(values, writer);
        }
    }

    public static DeathAnnouncerConfig load(Path pluginDataDirectory) {
        Path configFile = pluginDataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            return new DeathAnnouncerConfig(DEFAULT_LANGUAGE, DEFAULT_NOTIFICATIONS, DEFAULT_CHAT_NOTIFICATIONS,
                DEFAULT_HUD_NOTIFICATIONS, DEFAULT_HUD_DISPLAY_SECONDS);
        }

        try (var reader = Files.newBufferedReader(configFile)) {
            Yaml yaml = new Yaml();
            Object parsed = yaml.load(reader);
            if (parsed instanceof Map<?, ?> map) {
                String language = DEFAULT_LANGUAGE;
                Object languageValue = map.get("language");
                if (languageValue instanceof String value && !value.isBlank()) {
                    language = value.trim();
                }
                boolean notifications = DEFAULT_NOTIFICATIONS;
                Object notificationsValue = map.get("notifications");
                if (notificationsValue instanceof Boolean flag) {
                    notifications = flag;
                } else if (notificationsValue instanceof String text && !text.isBlank()) {
                    notifications = Boolean.parseBoolean(text.trim());
                }
                boolean chatNotifications = DEFAULT_CHAT_NOTIFICATIONS;
                Object chatNotificationsValue = map.get("chat-notifications");
                if (chatNotificationsValue instanceof Boolean flag) {
                    chatNotifications = flag;
                } else if (chatNotificationsValue instanceof String text && !text.isBlank()) {
                    chatNotifications = Boolean.parseBoolean(text.trim());
                }
                boolean hudNotifications = DEFAULT_HUD_NOTIFICATIONS;
                Object hudNotificationsValue = map.get("hud-notifications");
                if (hudNotificationsValue instanceof Boolean flag) {
                    hudNotifications = flag;
                } else if (hudNotificationsValue instanceof String text && !text.isBlank()) {
                    hudNotifications = Boolean.parseBoolean(text.trim());
                }
                long hudSeconds = DEFAULT_HUD_DISPLAY_SECONDS;
                Object hudSecondsValue = map.get("hud-display-seconds");
                if (hudSecondsValue instanceof Number number) {
                    hudSeconds = Math.max(1L, number.longValue());
                } else if (hudSecondsValue instanceof String text && !text.isBlank()) {
                    try {
                        hudSeconds = Math.max(1L, Long.parseLong(text.trim()));
                    } catch (NumberFormatException ignored) {
                        // Use default.
                    }
                }
                return new DeathAnnouncerConfig(language, notifications, chatNotifications, hudNotifications, hudSeconds);
            }
        } catch (IOException ignored) {
            // Fall back to defaults.
        }

        return new DeathAnnouncerConfig(DEFAULT_LANGUAGE, DEFAULT_NOTIFICATIONS, DEFAULT_CHAT_NOTIFICATIONS,
            DEFAULT_HUD_NOTIFICATIONS, DEFAULT_HUD_DISPLAY_SECONDS);
    }
}
