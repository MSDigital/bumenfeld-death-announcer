package com.bumenfeld;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class DeathAnnouncerConfig {
    private static final String DEFAULT_LANGUAGE = "en";
    private final String language;

    private DeathAnnouncerConfig(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public static DeathAnnouncerConfig load(Path pluginDataDirectory) {
        Path configFile = pluginDataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            return new DeathAnnouncerConfig(DEFAULT_LANGUAGE);
        }

        try (var reader = Files.newBufferedReader(configFile)) {
            Yaml yaml = new Yaml();
            Object parsed = yaml.load(reader);
            if (parsed instanceof Map<?, ?> map) {
                Object languageValue = map.get("language");
                if (languageValue instanceof String language && !language.isBlank()) {
                    return new DeathAnnouncerConfig(language.trim());
                }
            }
        } catch (IOException ignored) {
            // Fall back to defaults.
        }

        return new DeathAnnouncerConfig(DEFAULT_LANGUAGE);
    }
}
