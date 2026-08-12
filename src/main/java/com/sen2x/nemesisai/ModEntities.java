package com.sen2x.nemesisai;

import com.sen2x.nemesisai.entity.NemesisEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.core.Registry;

public final class ModEntities {
    public static final EntityType<NemesisEntity> NEMESIS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(NemesisAiMod.MOD_ID, "nemesis"),
            EntityType.Builder.of(NemesisEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build("nemesis_ai:nemesis")
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(
                NEMESIS,
                Zombie.createAttributes()
        );

        NemesisAiMod.LOGGER.info("Registered Nemesis entity");
    }
}