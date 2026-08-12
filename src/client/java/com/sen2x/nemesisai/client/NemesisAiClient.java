package com.sen2x.nemesisai.client;

import com.sen2x.nemesisai.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class NemesisAiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                ModEntities.NEMESIS,
                ZombieRenderer::new
        );
    }
}