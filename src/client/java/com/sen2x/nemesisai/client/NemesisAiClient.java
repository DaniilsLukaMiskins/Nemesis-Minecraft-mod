package com.sen2x.nemesisai.client;

import dev.nemesis.client.render.NemesisEntityRenderer;
import dev.nemesis.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class NemesisAiClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                ModEntities.NEMESIS,
                NemesisEntityRenderer::new
        );
    }
}