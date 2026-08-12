package com.sen2x.nemesisai.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class NemesisEntity extends Zombie {
    public NemesisEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }
}