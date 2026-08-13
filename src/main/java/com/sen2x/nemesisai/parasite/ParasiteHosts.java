package com.sen2x.nemesisai.parasite;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;

public final class ParasiteHosts {
    private ParasiteHosts() {}

    public static boolean isSupported(LivingEntity entity) {
        return entity instanceof Cow || entity instanceof Horse || entity instanceof Pig
                || entity instanceof Sheep || entity instanceof Chicken;
    }
}
