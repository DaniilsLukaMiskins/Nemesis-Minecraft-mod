package com.sen2x.nemesisai;

import com.sen2x.nemesisai.entity.NemesisEntity;
import com.sen2x.nemesisai.entity.BlueSlimeProjectile;
import com.sen2x.nemesisai.entity.BabyNemesisEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    public static final EntityType<BlueSlimeProjectile> BLUE_SLIME_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            NemesisAiMod.id("blue_slime_projectile"),
            EntityType.Builder.<BlueSlimeProjectile>of(BlueSlimeProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(8).updateInterval(10)
                    .build("nemesis_ai:blue_slime_projectile")
    );

    public static final EntityType<BabyNemesisEntity> BABY_NEMESIS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            NemesisAiMod.id("baby_nemesis"),
            EntityType.Builder.of(BabyNemesisEntity::new, MobCategory.MONSTER)
                    .sized(0.55F, 0.8F).clientTrackingRange(8)
                    .build("nemesis_ai:baby_nemesis")
    );

    public static final Item BLUE_SLIME_GLOB = Registry.register(
            BuiltInRegistries.ITEM, NemesisAiMod.id("blue_slime_glob"), new Item(new Item.Properties())
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
                        .add(Attributes.MAX_HEALTH, 100.0)
                        .add(Attributes.FOLLOW_RANGE, 100.0)
        );
        FabricDefaultAttributeRegistry.register(
                BABY_NEMESIS,
                Zombie.createAttributes()
                        .add(Attributes.MAX_HEALTH, 14.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.34)
                        .add(Attributes.ATTACK_DAMAGE, 4.0)
                        .add(Attributes.FOLLOW_RANGE, 32.0)
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(NEMESIS_SPAWN_EGG));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(BLUE_SLIME_GLOB));

        NemesisAiMod.LOGGER.info("Registered Nemesis entity");
    }
}
