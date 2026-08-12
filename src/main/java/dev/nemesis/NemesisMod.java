package dev.nemesis;

import dev.nemesis.command.NemesisCommands;
import dev.nemesis.entity.ModEntities;
import net.fabricmc.api.ModInitializer;

public final class NemesisMod implements ModInitializer {
    public static final String MOD_ID = "nemesis";

    @Override
    public void onInitialize() {
        ModEntities.register();
        NemesisCommands.register();
    }
}
