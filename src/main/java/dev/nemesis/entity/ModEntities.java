package dev.nemesis.entity;

import dev.nemesis.NemesisMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final EntityType<NemesisEntity> NEMESIS = Registry.register(
            Registries.ENTITY_TYPE,
            id("nemesis"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, NemesisEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .trackRangeBlocks(8)
                    .build()
    );

    public static final Item NEMESIS_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("nemesis_spawn_egg"),
            new SpawnEggItem(NEMESIS, 0x202020, 0x9B1C31, new Item.Settings())
    );

    private ModEntities() {}

    public static void register() {
        FabricDefaultAttributeRegistry.register(NEMESIS, NemesisEntity.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(NEMESIS_SPAWN_EGG));
    }

    private static Identifier id(String path) {
        return Identifier.of(NemesisMod.MOD_ID, path);
    }
}
