package com.sen2x.nemesisai.client;

import com.sen2x.nemesisai.api.LearningResult;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the "how much has Nemesis adapted" bar and the last learned-tactic banner.
 * Pure presentation: all data comes from {@link NemesisHudState}.
 */
public final class NemesisHud {
	private static final int BAR_X = 10;
	private static final int BAR_Y = 10;
	private static final int BAR_WIDTH = 100;
	private static final int BAR_HEIGHT = 4;

	private NemesisHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(NemesisHud::render);
	}

	private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		LearningResult result = NemesisHudState.latestResult();
		if (result == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui) {
			return;
		}

		int filled = Math.round(BAR_WIDTH * result.adaptationLevel());
		guiGraphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, 0x66000000);
		guiGraphics.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_HEIGHT, 0xFFE84C3D);
		guiGraphics.drawString(client.font, "NEMESIS ADAPTATION", BAR_X, BAR_Y - 10, 0xFFE84C3D, true);

		if (NemesisHudState.hasFreshMessage() && NemesisHudState.latestMessage() != null) {
			guiGraphics.drawString(client.font, NemesisHudState.latestMessage(), BAR_X, BAR_Y + BAR_HEIGHT + 4, 0xFFFFFFFF, true);
		}
	}
}
