package com.sen2x.nemesisai;

import com.sen2x.nemesisai.entity.NemesisEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.Registry;

public final class ModEntities {
    public static final EntityType<NemesisEntity> NEMESIS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(NemesisAiMod.MOD_ID, "nemesis"),
            EntityType.Builder.of(NemesisEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.75F)
                    .build("nemesis_ai:nemesis")
    );

    public static final Item NEMESIS_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            NemesisAiMod.id("nemesis_spawn_egg"),
            new SpawnEggItem(NEMESIS, 0x101A12, 0x76FF36, new Item.Properties())
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(
                NEMESIS,
                Zombie.createAttributes()
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(NEMESIS_SPAWN_EGG));

        NemesisAiMod.LOGGER.info("Registered Nemesis entity");
    }
}
