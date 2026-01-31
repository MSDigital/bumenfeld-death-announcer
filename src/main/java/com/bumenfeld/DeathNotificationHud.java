package com.bumenfeld;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Objects;

public final class DeathNotificationHud extends CustomUIHud {
    private final String title;
    private final String subtitle;
    private final String uiPath;
    private final String iconTexturePath;

    public DeathNotificationHud(PlayerRef playerRef, String title, String subtitle, String uiPath, String iconTexturePath) {
        super(playerRef);
        this.title = Objects.requireNonNullElse(title, "");
        this.subtitle = Objects.requireNonNullElse(subtitle, "");
        this.uiPath = Objects.requireNonNullElse(uiPath, "death_notification.ui");
        this.iconTexturePath = Objects.requireNonNullElse(iconTexturePath, "");
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append(uiPath);
        if (!iconTexturePath.isBlank()) {
            PatchStyle iconStyle = new PatchStyle().setTexturePath(Value.of(iconTexturePath));
            builder.setObject("#DeathIcon.Background", iconStyle);
        }
        builder.set("#DeathTitle.Text", title);
        builder.set("#DeathSubtitle.Text", subtitle);
    }
}
