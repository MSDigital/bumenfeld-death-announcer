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
    private final String language;
    private final boolean notificationsEnabled;
    private final boolean chatNotificationsEnabled;

    private DeathAnnouncerConfig(String language, boolean notificationsEnabled, boolean chatNotificationsEnabled) {
        this.language = language;
        this.notificationsEnabled = notificationsEnabled;
        this.chatNotificationsEnabled = chatNotificationsEnabled;
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

    public DeathAnnouncerConfig withLanguage(String newLanguage) {
        return new DeathAnnouncerConfig(newLanguage, notificationsEnabled, chatNotificationsEnabled);
    }

    public DeathAnnouncerConfig withNotifications(boolean enabled) {
        return new DeathAnnouncerConfig(language, enabled, chatNotificationsEnabled);
    }

    public DeathAnnouncerConfig withChatNotifications(boolean enabled) {
        return new DeathAnnouncerConfig(language, notificationsEnabled, enabled);
    }

    public void save(Path pluginDataDirectory) throws IOException {
        Path configFile = pluginDataDirectory.resolve("config.yml");
        Files.createDirectories(configFile.getParent());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("language", language);
        values.put("notifications", notificationsEnabled);
        values.put("chat-notifications", chatNotificationsEnabled);
        Yaml yaml = new Yaml();
        try (var writer = Files.newBufferedWriter(configFile)) {
            yaml.dump(values, writer);
        }
    }

    public static DeathAnnouncerConfig load(Path pluginDataDirectory) {
        Path configFile = pluginDataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            return new DeathAnnouncerConfig(DEFAULT_LANGUAGE, DEFAULT_NOTIFICATIONS, DEFAULT_CHAT_NOTIFICATIONS);
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
                return new DeathAnnouncerConfig(language, notifications, chatNotifications);
            }
        } catch (IOException ignored) {
            // Fall back to defaults.
        }

        return new DeathAnnouncerConfig(DEFAULT_LANGUAGE, DEFAULT_NOTIFICATIONS, DEFAULT_CHAT_NOTIFICATIONS);
    }
}
