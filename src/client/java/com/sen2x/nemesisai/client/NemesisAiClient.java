package com.sen2x.nemesisai.client;

import com.sen2x.nemesisai.ModEntities;
import com.sen2x.nemesisai.client.renderer.NemesisRenderer;
import com.sen2x.nemesisai.client.renderer.BlueSlimeProjectileRenderer;
import com.sen2x.nemesisai.network.LearningResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class NemesisAiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.NEMESIS, NemesisRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLUE_SLIME_PROJECTILE, BlueSlimeProjectileRenderer::new);
        NemesisHud.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NemesisHudState.tick();
            // Windows/GLFW can occasionally leave the mouse grabbed after an F11
            // transition. Gameplay keeps it grabbed, but every menu must show it.
            if (client.screen != null && client.mouseHandler.isMouseGrabbed()) {
                client.mouseHandler.releaseMouse();
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(LearningResultPayload.TYPE, (payload, context) -> {
            String message = NemesisHudState.update(payload.toLearningResult());
            if (message != null && context.player() != null) {
                context.player().displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
            }
        });
    }
}
