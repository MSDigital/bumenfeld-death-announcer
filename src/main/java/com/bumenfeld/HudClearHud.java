package com.bumenfeld;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class HudClearHud extends CustomUIHud {
    public HudClearHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(com.hypixel.hytale.server.core.ui.builder.UICommandBuilder builder) {
        // Empty build removes the custom HUD per official UI guide.
    }
}
