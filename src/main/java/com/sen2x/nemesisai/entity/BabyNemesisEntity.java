package com.sen2x.nemesisai.entity;

import com.sen2x.nemesisai.ModEntities;
import com.sen2x.nemesisai.parasite.ParasiteHosts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BabyNemesisEntity extends Zombie implements GeoEntity {
    public static final int FOOD_TO_GROW = 5;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nemesis.idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.nemesis.run");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.nemesis.bite");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int animalsEaten;

    public BabyNemesisEntity(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true,
                false, ParasiteHosts::isSupported));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide()) {
            triggerAnim("actions", "bite");
            level().playSound(null, blockPosition(), SoundEvents.SPIDER_HURT,
                    SoundSource.HOSTILE, 0.7F, 1.45F);
        }
        return super.doHurtTarget(target);
    }

    @Override public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (ParasiteHosts.isSupported(victim) && isAlive()) {
            animalsEaten++;
            if (animalsEaten >= FOOD_TO_GROW) growIntoAdult(level);
        }
        return result;
    }

    private void growIntoAdult(ServerLevel level) {
        NemesisEntity adult = ModEntities.NEMESIS.create(level);
        if (adult == null) return;
        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level.addFreshEntity(adult);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5), getZ(),
                5, 0.45, 0.5, 0.45, 0.03);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(0.5), getZ(),
                35, 0.65, 0.7, 0.65, 0.08);
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 1.3F, 0.8F);
        discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("NemesisAnimalsEaten", animalsEaten);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        animalsEaten = Math.max(0, tag.getInt("NemesisAnimalsEaten"));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 3, state ->
                state.setAndContinue(state.isMoving() ? RUN : IDLE)));
        controllers.add(new AnimationController<>(this, "actions", 1, state -> PlayState.STOP)
                .triggerableAnim("bite", BITE));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
    public int getAnimalsEaten() { return animalsEaten; }
}
