package com.sen2x.nemesisai.parasite;

import com.sen2x.nemesisai.ModEntities;
import com.sen2x.nemesisai.entity.BabyNemesisEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;

public final class ParasiteLifecycle {
    public static final int INCUBATION_TICKS = 300;

    private ParasiteLifecycle() {}

    public static boolean infect(Animal host) {
        if (!(host.level() instanceof ServerLevel level) || !ParasiteHosts.isSupported(host)) return false;
        ParasiteHostState state = (ParasiteHostState) host;
        if (state.nemesisAi$isInfected()) return false;
        state.nemesisAi$infect(INCUBATION_TICKS);
        host.hurt(level.damageSources().generic(), 0.1F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, host.getX(), host.getY(0.6), host.getZ(),
                18, 0.45, 0.45, 0.45, 0.08);
        level.sendParticles(ParticleTypes.SMOKE, host.getX(), host.getY(0.6), host.getZ(),
                14, 0.35, 0.4, 0.35, 0.03);
        level.playSound(null, host.blockPosition(), SoundEvents.SPIDER_HURT,
                SoundSource.HOSTILE, 1.0F, 0.55F);
        return true;
    }

    public static void tick(Animal host) {
        if (!(host.level() instanceof ServerLevel level) || !ParasiteHosts.isSupported(host)) return;
        ParasiteHostState state = (ParasiteHostState) host;
        int remaining = state.nemesisAi$getIncubationTicks();
        if (remaining <= 0 || !host.isAlive()) return;
        state.nemesisAi$setIncubationTicks(--remaining);
        if (remaining > 0) {
            if (remaining % 20 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, host.getX(), host.getY(0.7), host.getZ(),
                        2, 0.25, 0.25, 0.25, 0.01);
            }
            return;
        }

        BabyNemesisEntity baby = ModEntities.BABY_NEMESIS.create(level);
        if (baby == null) return;
        baby.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), 0.0F);
        level.addFreshEntity(baby);
        level.sendParticles(ParticleTypes.EXPLOSION, host.getX(), host.getY(0.5), host.getZ(),
                4, 0.35, 0.35, 0.35, 0.02);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, host.getX(), host.getY(0.5), host.getZ(),
                30, 0.65, 0.5, 0.65, 0.15);
        level.playSound(null, host.blockPosition(), SoundEvents.WARDEN_EMERGE,
                SoundSource.HOSTILE, 1.2F, 1.35F);
        host.hurt(level.damageSources().mobAttack(baby), Float.MAX_VALUE);
    }
}
