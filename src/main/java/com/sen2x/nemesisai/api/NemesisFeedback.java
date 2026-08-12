package com.sen2x.nemesisai.api;

import com.sen2x.nemesisai.network.LearningResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry point the AI module calls when Nemesis learns something. Pushes the event to the
 * player's client so the HUD module can display it; the HUD module never needs to know
 * how the learning decision was made.
 */
public final class NemesisFeedback {
	private NemesisFeedback() {
	}

	public static void broadcastLearning(ServerPlayer player, LearningResult result) {
		ServerPlayNetworking.send(player, new LearningResultPayload(result));
	}
}
