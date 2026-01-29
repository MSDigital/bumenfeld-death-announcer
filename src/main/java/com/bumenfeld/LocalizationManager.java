package com.bumenfeld;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LocalizationManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Path pluginDataDirectory;
    private final LocalizationBundle fallbackBundle;

    public LocalizationManager(Path pluginDataDirectory) {
        this.pluginDataDirectory = pluginDataDirectory;
        this.fallbackBundle = loadBundleFromResource("localization/en.json");
    }

    public LocalizationBundle load(String language) {
        if (language != null) {
            Path override = pluginDataDirectory.resolve("localization").resolve(language + ".json");
            if (Files.exists(override)) {
                LocalizationBundle bundle = loadBundleFromPath(override);
                if (bundle != null) {
                    return bundle;
                }
            }
        }
        return fallbackBundle;
    }

    private LocalizationBundle loadBundleFromResource(String resource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return new LocalizationBundle(List.of(), Map.of("generic", List.of("{player} has fallen.")));
            }
            return loadBundle(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load built-in localization", e);
        }
    }

    private LocalizationBundle loadBundleFromPath(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return loadBundle(stream);
        } catch (IOException ignored) {
            return null;
        }
    }

    private LocalizationBundle loadBundle(InputStream stream) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(stream);
        JsonNode deathAnnouncer = root.path("death-announcer");
        List<String> titles = readList(deathAnnouncer.get("titles"));
        Map<String, List<String>> categories = new HashMap<>();
        deathAnnouncer.fields().forEachRemaining(entry -> {
            List<String> lines = readList(entry.getValue());
            if (!lines.isEmpty()) {
                if (!"titles".equals(entry.getKey())) {
                    categories.put(entry.getKey(), List.copyOf(lines));
                }
            }
        });
        if (!categories.containsKey("generic")) {
            categories.put("generic", List.of("{player} has fallen."));
        }
        return new LocalizationBundle(titles, categories);
    }

    private List<String> readList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }
}
