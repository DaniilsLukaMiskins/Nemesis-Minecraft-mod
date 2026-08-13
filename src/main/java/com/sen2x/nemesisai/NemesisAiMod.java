package com.sen2x.nemesisai;

import com.sen2x.nemesisai.command.NemesisCommands;
import com.sen2x.nemesisai.network.LearningResultPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NemesisAiMod implements ModInitializer {
    public static final String MOD_ID = "nemesis_ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(LearningResultPayload.TYPE, LearningResultPayload.STREAM_CODEC);
        ModEntities.register();
        NemesisCommands.register();
        LOGGER.info("Nemesis AI initialized!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
