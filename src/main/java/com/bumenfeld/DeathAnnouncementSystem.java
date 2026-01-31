package com.bumenfeld;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public final class DeathAnnouncementSystem extends DeathSystems.OnDeathSystem {
    private static final Logger LOGGER = Logger.getLogger(DeathAnnouncementSystem.class.getName());

    private static final Map<String, String> ICON_FILES = Map.ofEntries(
        Map.entry("physical", "physical.png"),
        Map.entry("projectile", "projectile.png"),
        Map.entry("command", "explosion.png"),
        Map.entry("drowning", "drowning.png"),
        Map.entry("drown", "drown.png"),
        Map.entry("environment", "environment.png"),
        Map.entry("fire", "fire.png"),
        Map.entry("lava", "lava.png"),
        Map.entry("explosion", "explosion.png"),
        Map.entry("poison", "poison.png"),
        Map.entry("freeze", "freeze.png"),
        Map.entry("fall", "fall.png"),
        Map.entry("out_of_world", "out_of_world.png"),
        Map.entry("void", "void.png"),
        Map.entry("suffocation", "suffocation.png")
    );
    private static final String HUD_ICON_PREFIX = "icons/";
    private static final String NOTIFICATION_ICON_PREFIX = "ui/custom/icons/";
    private static final String FALLBACK_NOTIFICATION_ITEM = "Weapon_Sword_Mithril";

    private volatile long hudDisplaySeconds;
    private volatile boolean hudNotificationsEnabled;

    private volatile LocalizationBundle localizationBundle;
    private volatile boolean notificationsEnabled;
    private volatile boolean chatNotificationsEnabled;
    private final ScheduledExecutorService hudResetScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "death-notification-hud-reset");
        thread.setDaemon(true);
        return thread;
    });
    private final String hudResourcePath;
    private final ConcurrentMap<PlayerRef, ScheduledFuture<?>> hudResetTasks = new ConcurrentHashMap<>();

    public DeathAnnouncementSystem(LocalizationBundle localizationBundle,
                                   boolean notificationsEnabled,
                                   boolean chatNotificationsEnabled,
                                   String hudResourcePath,
                                   long hudDisplaySeconds,
                                   boolean hudNotificationsEnabled) {
        this.localizationBundle = Objects.requireNonNull(localizationBundle, "localizationBundle");
        this.notificationsEnabled = notificationsEnabled;
        this.chatNotificationsEnabled = chatNotificationsEnabled;
        this.hudResourcePath = Objects.requireNonNull(hudResourcePath, "hudResourcePath");
        this.hudDisplaySeconds = Math.max(1L, hudDisplaySeconds);
        this.hudNotificationsEnabled = hudNotificationsEnabled;
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

        sendNotification(causeName, subtitle, title);
        displayDeathHudForAll(announcement.title(), announcement.subtitle(), resolveHudIconTexture(causeName));
        broadcastDeathChat(announcement.subtitle());
    }
    public CompletableFuture<Void> triggerNotification(String causeName, String playerName) {
        DeathAnnouncement announcement = pickAnnouncement(playerName, causeName);
        Message title = Message.raw(announcement.title());
        Message subtitle = Message.raw(announcement.subtitle());
        sendNotification(causeName, subtitle, title);
        displayDeathHudForAll(announcement.title(), announcement.subtitle(), resolveHudIconTexture(causeName));
        broadcastDeathChat(announcement.subtitle());
        return CompletableFuture.completedFuture(null);
    }

    public Iterable<String> getSupportedCauses() {
        return resolveRawDamageCauseIds();
    }

    private DeathAnnouncement pickAnnouncement(String playerName, String causeName) {
        String category = resolveCategory(causeName);
        List<String> pool = localizationBundle.getLines(category);
        String title = formatLine(pickLine(localizationBundle.getTitles()), playerName);
        String subtitle = formatLine(pickLine(pool), playerName);
        return new DeathAnnouncement(title, subtitle);
    }

    private static String resolveCategory(String causeName) {
        if (causeName.contains("projectile")) {
            return "projectile";
        }
        if (causeName.contains("fall")) {
            return "fall";
        }
        if (causeName.contains("drown")) {
            return "drowning";
        }
        if (causeName.contains("suffocat")) {
            return "suffocation";
        }
        if (causeName.contains("out_of_world") || causeName.contains("void")) {
            return "out_of_world";
        }
        if (causeName.contains("fire")) {
            return "fire";
        }
        if (causeName.contains("lava")) {
            return "lava";
        }
        if (causeName.contains("explosion")) {
            return "explosion";
        }
        if (causeName.contains("poison")) {
            return "poison";
        }
        if (causeName.contains("freeze")) {
            return "freeze";
        }
        if (causeName.contains("environment")) {
            return "environment";
        }
        if (causeName.contains("command")) {
            return "command";
        }
        if (causeName.contains("physical") || causeName.contains("melee") || causeName.contains("attack")) {
            return "physical";
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

    private static ItemWithAllMetadata resolveFallbackNotificationIcon() {
        ItemStack stack = new ItemStack(FALLBACK_NOTIFICATION_ITEM, 1);
        return stack.toPacket();
    }

    private static String resolveCauseName(DeathComponent component) {
        DamageCause cause = component.getDeathCause();
        if (cause != null) {
            return cause.getId().toLowerCase(Locale.ROOT);
        }
        return resolveCauseNameFromDamage(component.getDeathInfo());
    }

    private static List<String> resolveRawDamageCauseIds() {
        List<String> ids = new java.util.ArrayList<>();
        try {
            Object[] values = DamageCause.class.getEnumConstants();
            if (values != null) {
                for (Object value : values) {
                    String id = resolveDamageCauseId(value);
                    if (id != null && !id.isBlank()) {
                        ids.add(id.toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Fall back to reflection below.
        }

        if (ids.isEmpty()) {
            try {
                Object values = DamageCause.class.getMethod("values").invoke(null);
                if (values instanceof Object[] valueArray) {
                    for (Object value : valueArray) {
                        String id = resolveDamageCauseId(value);
                        if (id != null && !id.isBlank()) {
                            ids.add(id.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall back to categories below.
            }
        }

        if (ids.isEmpty()) {
            return List.copyOf(ICON_FILES.keySet());
        }

        return ids.stream()
            .distinct()
            .sorted()
            .toList();
    }

    private static String resolveDamageCauseId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Object idValue = value.getClass().getMethod("getId").invoke(value);
            if (idValue instanceof String text) {
                return text;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback to string representation.
        }
        return value.toString();
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

    private void sendNotification(String causeName, Message notificationTitle, Message notificationSubtitle) {
        if (!notificationsEnabled) {
            return;
        }

        String iconPath = resolveNotificationIconPath(causeName);
        if (trySendNotificationWithIconPath(notificationTitle, notificationSubtitle, iconPath)) {
            return;
        }

        NotificationUtil.sendNotificationToUniverse(
            notificationTitle,
            notificationSubtitle,
            null,
            resolveFallbackNotificationIcon(),
            NotificationStyle.Danger
        );
    }

    private void displayDeathHudForAll(String titleText, String subtitleText, String iconTexturePath) {
        if (!hudNotificationsEnabled) {
            return;
        }

        String safeTitle = titleText == null ? "" : titleText;
        String safeSubtitle = subtitleText == null ? "" : subtitleText;
        String safeIcon = iconTexturePath == null ? "" : iconTexturePath;

        Collection<World> worlds = Universe.get().getWorlds().values();
        for (World world : worlds) {
            if (world == null) {
                continue;
            }

            world.execute(() -> {
                for (PlayerRef playerRef : world.getPlayerRefs()) {
                    if (playerRef == null) {
                        continue;
                    }

                    Ref<EntityStore> playerEntityRef = playerRef.getReference();
                    if (playerEntityRef == null) {
                        continue;
                    }

                    Store<EntityStore> entityStore = playerEntityRef.getStore();
                    if (entityStore == null) {
                        continue;
                    }

                    Player spectator = entityStore.getComponent(playerEntityRef, Player.getComponentType());
                    if (spectator == null) {
                        continue;
                    }

                    HudManager hudManager = spectator.getHudManager();
                    if (hudManager == null) {
                        continue;
                    }

                    showDeathHud(world, hudManager, playerRef, safeTitle, safeSubtitle, safeIcon);
                }
            });
        }
    }

    private void showDeathHud(World world,
                              HudManager hudManager,
                              PlayerRef playerRef,
                              String titleText,
                              String subtitleText,
                              String iconTexturePath) {
        if (world == null || hudManager == null || playerRef == null) {
            return;
        }

        try {
            DeathNotificationHud hud = new DeathNotificationHud(playerRef, titleText, subtitleText, hudResourcePath, iconTexturePath);
            hudManager.setCustomHud(playerRef, hud);
            scheduleHudReset(world, hudManager, playerRef);
        } catch (RuntimeException ex) {
            String username = playerRef.getUsername();
            String identifier = username != null ? username : playerRef.getUuid().toString();
            LOGGER.log(Level.WARNING, String.format("Failed to display death HUD for %s; the HUD packet was not sent.",
                identifier), ex);
        }
    }

    private static String resolveHudIconTexture(String causeName) {
        return HUD_ICON_PREFIX + resolveIconFile(causeName);
    }

    private static String resolveNotificationIconPath(String causeName) {
        return NOTIFICATION_ICON_PREFIX + resolveIconFile(causeName);
    }

    private static String resolveIconFile(String causeName) {
        String key = causeName == null ? "" : causeName;
        String direct = ICON_FILES.get(key);
        if (direct != null) {
            return direct;
        }
        String category = resolveCategory(key);
        return ICON_FILES.getOrDefault(category, "physical.png");
    }

    private boolean trySendNotificationWithIconPath(Message notificationTitle,
                                                    Message notificationSubtitle,
                                                    String iconPath) {
        if (iconPath == null || iconPath.isBlank()) {
            return false;
        }

        try {
            var method = NotificationUtil.class.getMethod(
                "sendNotificationToUniverse",
                Message.class,
                Message.class,
                String.class,
                ItemWithAllMetadata.class,
                NotificationStyle.class
            );
            method.invoke(null, notificationTitle, notificationSubtitle, iconPath, null, NotificationStyle.Danger);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // Try a smaller overload below.
        }

        try {
            var method = NotificationUtil.class.getMethod(
                "sendNotificationToUniverse",
                Message.class,
                String.class,
                NotificationStyle.class
            );
            method.invoke(null, notificationTitle, iconPath, NotificationStyle.Danger);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void scheduleHudReset(World world, HudManager hudManager, PlayerRef playerRef) {
        if (world == null || hudManager == null || playerRef == null) {
            return;
        }

        ScheduledFuture<?> previous = hudResetTasks.remove(playerRef);
        if (previous != null) {
            previous.cancel(false);
        }

        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        long delaySeconds = Math.max(1L, hudDisplaySeconds);
        ScheduledFuture<?> future = hudResetScheduler.schedule(() -> {
            world.execute(() -> hudManager.setCustomHud(playerRef, new HudClearHud(playerRef)));
            hudResetTasks.remove(playerRef, futureRef.get());
        }, delaySeconds, TimeUnit.SECONDS);
        futureRef.set(future);
        hudResetTasks.put(playerRef, future);
    }

    public void shutdown() {
        hudResetScheduler.shutdownNow();
        hudResetTasks.clear();
    }

    public void updateLocalizationBundle(LocalizationBundle newBundle) {
        this.localizationBundle = Objects.requireNonNull(newBundle, "newBundle");
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    private void broadcastDeathChat(String text) {
        if (!chatNotificationsEnabled) {
            return;
        }

        Message header = Message
            .raw("[DEATH] ")
            .color(new Color(255, 60, 60))
            .bold(true);
        Message body = Message.raw(text);
        Universe.get().sendMessage(Message.empty().insertAll(header, body));
    }

    public void setChatNotificationsEnabled(boolean enabled) {
        this.chatNotificationsEnabled = enabled;
    }

    public void setHudNotificationsEnabled(boolean enabled) {
        this.hudNotificationsEnabled = enabled;
    }

    public void setHudDisplaySeconds(long seconds) {
        this.hudDisplaySeconds = Math.max(1L, seconds);
    }

    private record DeathAnnouncement(String title, String subtitle) {
    }
}
