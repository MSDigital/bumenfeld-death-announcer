package com.bumenfeld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

final class ExtractedAssetPackManifestEnsurer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExtractedAssetPackManifestEnsurer() {
    }

    static void ensure(JavaPlugin plugin, Logger logger) {
        try {
            var pluginManifest = plugin.getManifest();
            if (pluginManifest == null || !pluginManifest.includesAssetPack()) {
                return;
            }

            Path dataDirectory = plugin.getDataDirectory();
            if (dataDirectory == null) {
                return;
            }

            Files.createDirectories(dataDirectory);
            Path extractedManifest = dataDirectory.resolve("manifest.json");
            String extractedPackName = extractedPackName(pluginManifest.getName());
            try (InputStream stream = plugin.getClass().getClassLoader().getResourceAsStream("manifest.json")) {
                if (stream == null) {
                    logger.warning("Missing bundled resource: manifest.json");
                    return;
                }
                JsonNode root = MAPPER.readTree(stream);
                if (!(root instanceof ObjectNode rootObject)) {
                    logger.warning("Bundled manifest.json is not a JSON object");
                    return;
                }
                if (!shouldWriteExtractedManifest(
                    extractedManifest,
                    pluginManifest.getGroup(),
                    pluginManifest.getName(),
                    extractedPackName,
                    rootObject
                )) {
                    return;
                }

                rootObject.put("Name", extractedPackName);
                rootObject.put("IncludesAssetPack", false);
                MAPPER.writerWithDefaultPrettyPrinter().writeValue(extractedManifest.toFile(), rootObject);
            }
        } catch (IOException e) {
            logger.warning("Unable to ensure extracted asset-pack manifest: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.warning("Unable to ensure extracted asset-pack manifest: " + e.getMessage());
        }
    }

    private static boolean shouldWriteExtractedManifest(
        Path path,
        String pluginGroup,
        String pluginName,
        String extractedPackName,
        JsonNode bundledManifest
    ) {
        if (!Files.exists(path)) {
            return true;
        }

        try {
            JsonNode root = MAPPER.readTree(path.toFile());
            if (!(root instanceof ObjectNode)) {
                return true;
            }
            String group = root.path("Group").asText("");
            String name = root.path("Name").asText("");

            if (group.equals(pluginGroup) && name.equals(extractedPackName)) {
                if (root.path("IncludesAssetPack").asBoolean(true)) {
                    return true;
                }

                String extractedBuildId = root.path("Build").path("Id").asText("");
                String bundledBuildId = bundledManifest.path("Build").path("Id").asText("");
                if (!bundledBuildId.isBlank() && !bundledBuildId.equals(extractedBuildId)) {
                    return true;
                }

                String extractedServerVersion = root.path("ServerVersion").asText("");
                String bundledServerVersion = bundledManifest.path("ServerVersion").asText("");
                if (!bundledServerVersion.isBlank() && !bundledServerVersion.equals(extractedServerVersion)) {
                    return true;
                }

                return false;
            }
            if (group.equals(pluginGroup) && name.equals(pluginName)) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private static String extractedPackName(String pluginName) {
        if (pluginName == null || pluginName.isBlank()) {
            return "Config_Plugin";
        }
        return "Config_" + pluginName;
    }
}
