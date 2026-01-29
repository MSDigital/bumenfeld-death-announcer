package com.bumenfeld;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public final class DeathAnnouncementSystem extends DeathSystems.OnDeathSystem {
    private static final Logger LOGGER = Logger.getLogger(DeathAnnouncementSystem.class.getName());

    private static final Map<String, String> ICONS = Map.ofEntries(
        Map.entry("fall", "Template_Glider"),
        Map.entry("fire", "Ingredient_Fire_Essence"),
        Map.entry("lava", "Fluid_Lava"),
        Map.entry("drown", "Fluid_Water"),
        Map.entry("void", "Ingredient_Void_Essence"),
        Map.entry("explosion", "Weapon_Grenade_Frag"),
        Map.entry("projectile", "Weapon_Arrow_Crude"),
        Map.entry("melee", "Weapon_Longsword_Iron"),
        Map.entry("poison", "Potion_Poison"),
        Map.entry("suffocation", "Soil_Sand"),
        Map.entry("freeze", "Ingredient_Ice_Essence"),
        Map.entry("starvation", "Food_Bread")
    );

    private volatile LocalizationBundle localizationBundle;

    public DeathAnnouncementSystem(LocalizationBundle localizationBundle) {
        this.localizationBundle = Objects.requireNonNull(localizationBundle, "localizationBundle");
    }

    @Override
    public Query getQuery() {
        return Query.and(Player.getComponentType(), DeathComponent.getComponentType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComponentAdded(Ref ref,
                                 DeathComponent component,
                                 Store store,
                                 CommandBuffer commandBuffer) {
        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        String displayName = player.getDisplayName();
        String causeName = resolveCauseName(component);
        DeathAnnouncement announcement = pickAnnouncement(displayName, causeName);
        Message title = Message.raw(announcement.title());
        Message subtitle = Message.raw(announcement.subtitle());

        logDeath(player, causeName);
        sendPlayerChat(player, causeName);

        NotificationUtil.sendNotificationToUniverse(
            title,
            subtitle,
            null,
            resolveNotificationIcon(causeName),
            NotificationStyle.Danger
        );
    }
    public CompletableFuture<Void> triggerNotification(String causeName, String playerName) {
        DeathAnnouncement announcement = pickAnnouncement(playerName, causeName);
        Message title = Message.raw(announcement.title());
        Message subtitle = Message.raw(announcement.subtitle());
        NotificationUtil.sendNotificationToUniverse(
            title,
            subtitle,
            null,
            resolveNotificationIcon(causeName),
            NotificationStyle.Danger
        );
        return CompletableFuture.completedFuture(null);
    }

    public Iterable<String> getSupportedCauses() {
        return ICONS.keySet();
    }

    private DeathAnnouncement pickAnnouncement(String playerName, String causeName) {
        String category = resolveCategory(causeName);
        List<String> pool = localizationBundle.getLines(category);
        String title = formatLine(pickLine(localizationBundle.getTitles()), playerName);
        String subtitle = formatLine(pickLine(pool), playerName);
        return new DeathAnnouncement(title, subtitle);
    }

    private static String resolveCategory(String causeName) {
        if (causeName.contains("fall")) {
            return "fall";
        }
        if (causeName.contains("lava")) {
            return "lava";
        }
        if (causeName.contains("fire") || causeName.contains("burn")) {
            return "fire";
        }
        if (causeName.contains("drown") || causeName.contains("water")) {
            return "drown";
        }
        if (causeName.contains("void")) {
            return "void";
        }
        if (causeName.contains("explosion") || causeName.contains("blast")) {
            return "explosion";
        }
        if (causeName.contains("projectile") || causeName.contains("arrow") || causeName.contains("bullet")) {
            return "projectile";
        }
        if (causeName.contains("melee") || causeName.contains("entity") || causeName.contains("attack")) {
            return "melee";
        }
        if (causeName.contains("poison") || causeName.contains("wither")) {
            return "poison";
        }
        if (causeName.contains("suffocat") || causeName.contains("crush")) {
            return "suffocation";
        }
        if (causeName.contains("freeze") || causeName.contains("frost")) {
            return "freeze";
        }
        if (causeName.contains("starve") || causeName.contains("hunger")) {
            return "starvation";
        }
        return "generic";
    }

    private static String pickLine(List<String> options) {
        if (options.isEmpty()) {
            return "{player} fell.";
        }
        int index = ThreadLocalRandom.current().nextInt(options.size());
        return options.get(index);
    }

    private static String formatLine(String template, String playerName) {
        return template.replace("{player}", playerName);
    }

    private static ItemWithAllMetadata resolveNotificationIcon(String causeName) {
        String identifier = ICONS.getOrDefault(resolveCategory(causeName), "Weapon_Sword_Mithril");
        ItemStack stack = new ItemStack(identifier, 1);
        return stack.toPacket();
    }

    private static String resolveCauseName(DeathComponent component) {
        DamageCause cause = component.getDeathCause();
        if (cause != null) {
            return cause.getId().toLowerCase(Locale.ROOT);
        }
        return resolveCauseNameFromDamage(component.getDeathInfo());
    }

    private static String resolveCauseNameFromDamage(Damage deathInfo) {
        if (deathInfo == null) {
            return "";
        }

        String[] probes = {
            "getCause",
            "getSource",
            "getDamageSource",
            "getDamageType",
            "getType"
        };

        for (String probe : probes) {
            try {
                Object value = deathInfo.getClass().getMethod(probe).invoke(deathInfo);
                if (value == null) {
                    continue;
                }
                if (value instanceof Enum<?> enumValue) {
                    return enumValue.name().toLowerCase(Locale.ROOT);
                }
                if (value instanceof String text) {
                    return text.toLowerCase(Locale.ROOT);
                }
                return value.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            } catch (ReflectiveOperationException ignored) {
                // Try the next probe.
            }
        }

        return deathInfo.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private static void logDeath(Player player, String causeName) {
        LOGGER.info(String.format("Player %s died (%s)", player.getDisplayName(), causeName));
    }

    private static void sendPlayerChat(Player player, String causeName) {
        String chatLine = String.format("[DeathLog] %s -> %s", player.getDisplayName(), causeName);
        player.sendMessage(Message.raw(chatLine));
    }

    public void updateLocalizationBundle(LocalizationBundle newBundle) {
        this.localizationBundle = Objects.requireNonNull(newBundle, "newBundle");
    }

    private record DeathAnnouncement(String title, String subtitle) {
    }
}
