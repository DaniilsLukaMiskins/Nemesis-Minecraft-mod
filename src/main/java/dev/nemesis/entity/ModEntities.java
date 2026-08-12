package dev.nemesis.entity;

import dev.nemesis.NemesisMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModEntities {
    public static final EntityType<NemesisEntity> NEMESIS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("nemesis"),
            EntityType.Builder.of(NemesisEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(id("nemesis").toString())
    );

    public static final Item NEMESIS_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            id("nemesis_spawn_egg"),
            new SpawnEggItem(NEMESIS, 0x202020, 0x9B1C31, new Item.Properties())
    );

    private ModEntities() {}

    public static void register() {
        FabricDefaultAttributeRegistry.register(NEMESIS, NemesisEntity.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(NEMESIS_SPAWN_EGG));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NemesisMod.MOD_ID, path);
    }
}
