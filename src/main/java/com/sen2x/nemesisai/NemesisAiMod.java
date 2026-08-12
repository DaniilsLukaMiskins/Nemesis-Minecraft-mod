package com.sen2x.nemesisai;

import dev.nemesis.command.NemesisCommands;
import dev.nemesis.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NemesisAiMod implements ModInitializer {
    public static final String MOD_ID = "nemesis_ai";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        NemesisCommands.register();

        LOGGER.info("Nemesis AI initialized!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}