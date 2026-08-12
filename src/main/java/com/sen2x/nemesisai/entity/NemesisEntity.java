package com.sen2x.nemesisai.entity;

import com.sen2x.nemesisai.api.LearningResult;
import com.sen2x.nemesisai.api.NemesisFeedback;
import com.sen2x.nemesisai.api.NemesisMemoryStore;
import com.sen2x.nemesisai.api.Tactic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NemesisEntity extends Zombie implements GeoEntity {
    private static final int LEARNING_THRESHOLD = 3;
    private static final float MELEE_DAMAGE_MULTIPLIER = 0.5F;
    private static final float RANGED_DAMAGE_MULTIPLIER = 0.65F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nemesis.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.nemesis.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.nemesis.run");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("animation.nemesis.claw_attack");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.nemesis.bite");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.nemesis.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.nemesis.death");
    private static final RawAnimation ADAPT_MELEE = RawAnimation.begin().thenPlay("animation.nemesis.adapt_melee");
    private static final RawAnimation ADAPT_RANGED = RawAnimation.begin().thenPlay("animation.nemesis.adapt_ranged");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int meleeHits;
    private int rangedHits;
    private boolean learnedMelee;
    private boolean learnedRanged;
    private Tactic tactic = Tactic.NORMAL;
    private int tacticCooldown;
    private int zigzagDirection = 1;

    public NemesisEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (tactic == Tactic.DELAYED_ATTACK && tacticCooldown > 0) {
            return false;
        }
        if (!level().isClientSide()) {
            triggerAnim("actions", random.nextBoolean() ? "claw" : "bite");
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK,
                    SoundSource.HOSTILE, 1.15F, 0.72F + random.nextFloat() * 0.12F);
        }
        tacticCooldown = tactic == Tactic.DELAYED_ATTACK ? 30 : 12;
        return super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (tacticCooldown > 0) tacticCooldown--;
        if (level().isClientSide()) return;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (tactic == Tactic.ZIGZAG_APPROACH && distanceToSqr(target) > 9.0) {
            if (tickCount % 12 == 0) zigzagDirection = -zigzagDirection;
            getMoveControl().strafe(0.9F, 0.75F * zigzagDirection);
        } else if (tactic == Tactic.RANGED_ATTACK && tacticCooldown <= 0
                && distanceToSqr(target) > 16.0 && getSensing().hasLineOfSight(target)) {
            Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
            double dy = target.getEyeY() - arrow.getY();
            arrow.shoot(target.getX() - getX(), dy, target.getZ() - getZ(), 1.5F, 4.0F);
            level().addFreshEntity(arrow);
            tacticCooldown = 40;
        }
    }

    public void setTactic(Tactic tactic) {
        this.tactic = tactic;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(tactic == Tactic.FAST_CHASE ? 0.38 : 0.25);
        setSprinting(tactic == Tactic.FAST_CHASE);
        getNavigation().stop();
    }

    public Tactic getTactic() {
        return tactic;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean playerAttack = source.getEntity() instanceof Player;
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        float adaptedDamage = amount;

        if (playerAttack && projectile && learnedRanged) {
            adaptedDamage *= RANGED_DAMAGE_MULTIPLIER;
        } else if (playerAttack && !projectile && learnedMelee) {
            adaptedDamage *= MELEE_DAMAGE_MULTIPLIER;
        }

        boolean wasHurt = super.hurt(source, adaptedDamage);
        if (!level().isClientSide() && wasHurt) {
            triggerAnim("actions", "hurt");
            if (source.getEntity() instanceof Player player) {
                if (projectile) learnRanged(player); else learnMelee(player);
            }
        }
        return wasHurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) triggerAnim("actions", "death");
        super.die(source);
    }

    private void learnMelee(Player player) {
        meleeHits++;
        if (meleeHits >= LEARNING_THRESHOLD && !learnedMelee) {
            learnedMelee = true;
            triggerAnim("actions", "adapt_melee");
            LearningResult result = new LearningResult(Tactic.DELAYED_ATTACK,
                    "Learned from repeated melee attacks", 0.5F);
            NemesisMemoryStore.record(player.getUUID(), result);
            if (player instanceof ServerPlayer serverPlayer) {
                NemesisFeedback.broadcastLearning(serverPlayer, result);
            }
            player.displayClientMessage(Component.literal("LEARNED: MELEE ATTACKS"), true);
            player.displayClientMessage(Component.literal("TACTIC CHANGED: MELEE RESISTANCE"), false);
        }
    }

    private void learnRanged(Player player) {
        rangedHits++;
        if (rangedHits >= LEARNING_THRESHOLD && !learnedRanged) {
            learnedRanged = true;
            triggerAnim("actions", "adapt_ranged");
            LearningResult result = new LearningResult(Tactic.ZIGZAG_APPROACH,
                    "Learned from repeated projectile attacks", 1.0F);
            NemesisMemoryStore.record(player.getUUID(), result);
            if (player instanceof ServerPlayer serverPlayer) {
                NemesisFeedback.broadcastLearning(serverPlayer, result);
            }
            player.displayClientMessage(Component.literal("LEARNED: RANGED ATTACKS"), true);
            player.displayClientMessage(Component.literal("TACTIC CHANGED: PROJECTILE RESISTANCE"), false);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 5, state -> {
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(isSprinting() ? RUN : WALK);
        }));
        // A stopped action controller leaves locomotion bones alone. Playing idle here
        // would continuously override the walk/run keyframes and make the mob slide.
        controllers.add(new AnimationController<>(this, "actions", 2, state -> PlayState.STOP)
                .triggerableAnim("claw", CLAW)
                .triggerableAnim("bite", BITE)
                .triggerableAnim("hurt", HURT)
                .triggerableAnim("death", DEATH)
                .triggerableAnim("adapt_melee", ADAPT_MELEE)
                .triggerableAnim("adapt_ranged", ADAPT_RANGED));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.65F, 0.72F + random.nextFloat() * 0.1F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("NemesisMeleeHits", meleeHits);
        tag.putInt("NemesisRangedHits", rangedHits);
        tag.putBoolean("NemesisLearnedMelee", learnedMelee);
        tag.putBoolean("NemesisLearnedRanged", learnedRanged);
        tag.putString("NemesisTactic", tactic.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        meleeHits = tag.getInt("NemesisMeleeHits");
        rangedHits = tag.getInt("NemesisRangedHits");
        learnedMelee = tag.getBoolean("NemesisLearnedMelee");
        learnedRanged = tag.getBoolean("NemesisLearnedRanged");
        if (tag.contains("NemesisTactic")) {
            try {
                setTactic(Tactic.valueOf(tag.getString("NemesisTactic")));
            } catch (IllegalArgumentException ignored) {
                setTactic(Tactic.NORMAL);
            }
        }
    }

    public int getMeleeHits() { return meleeHits; }
    public int getRangedHits() { return rangedHits; }
    public boolean hasLearnedMelee() { return learnedMelee; }
    public boolean hasLearnedRanged() { return learnedRanged; }
}
