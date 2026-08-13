package dev.nemesis.entity;

import com.sen2x.nemesisai.api.LearningResult;
import com.sen2x.nemesisai.api.NemesisFeedback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Objects;

public final class NemesisEntity extends Monster {
    private static final double NORMAL_MOVEMENT_SPEED = 0.25;
    private static final double FAST_MOVEMENT_SPEED = 0.38;

    // Learning: how many times a player behavior must repeat before Nemesis switches tactic.
    private static final int SHIELD_LEARN_THRESHOLD = 3;
    private static final int RANGED_LEARN_THRESHOLD = 3;
    private static final int FLEE_LEARN_THRESHOLD = 3;
    private static final int FLEE_CHECK_INTERVAL_TICKS = 20;
    private static final double FLEE_DISTANCE_GAIN = 1.5;

    private static final String TAG_TACTIC = "NemesisTactic";
    private static final String TAG_SHIELD_BLOCKS = "NemesisShieldBlocks";
    private static final String TAG_RANGED_HITS = "NemesisRangedHits";
    private static final String TAG_FLEE_SIGNALS = "NemesisFleeSignals";

    private Tactic tactic = Tactic.NORMAL;

    // Learned player-habit counters, persisted via addAdditionalSaveData/readAdditionalSaveData
    // so Nemesis remembers a player's habits across server restarts.
    private int shieldBlocks;
    private int rangedHits;
    private int fleeSignals;

    private double lastTrackedDistance = -1.0;
    private int fleeCheckTimer;

    public NemesisEntity(EntityType<? extends NemesisEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, NORMAL_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new TacticMeleeGoal());
        goalSelector.addGoal(2, new DelayedAttackGoal());
        goalSelector.addGoal(2, new ZigzagApproachGoal());
        goalSelector.addGoal(2, new RangedAttackGoal());
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setTactic(Tactic tactic) {
        this.tactic = Objects.requireNonNull(tactic, "tactic");
        var movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(tactic == Tactic.FAST_CHASE
                    ? FAST_MOVEMENT_SPEED : NORMAL_MOVEMENT_SPEED);
        }
        getNavigation().stop();
    }

    public Tactic getTactic() {
        return tactic;
    }

    /** Clears everything Nemesis has learned about the nearest player and returns to the default tactic. */
    public void resetLearning() {
        shieldBlocks = 0;
        rangedHits = 0;
        fleeSignals = 0;
        lastTrackedDistance = -1.0;
        setTactic(Tactic.NORMAL);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (!level().isClientSide() && wasHurt
                && source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getEntity() instanceof ServerPlayer player) {
            rangedHits++;
            tryLearn(player, Tactic.ZIGZAG_APPROACH,
                    "Player hit Nemesis with " + rangedHits + " ranged attacks",
                    RANGED_LEARN_THRESHOLD, rangedHits);
        }
        return wasHurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide() && target instanceof ServerPlayer player && player.isBlocking()) {
            shieldBlocks++;
            tryLearn(player, Tactic.DELAYED_ATTACK,
                    "Player blocked " + shieldBlocks + " of Nemesis's attacks with a shield",
                    SHIELD_LEARN_THRESHOLD, shieldBlocks);
        }
        return super.doHurtTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (--fleeCheckTimer > 0) {
            return;
        }
        fleeCheckTimer = FLEE_CHECK_INTERVAL_TICKS;

        if (!(getTarget() instanceof ServerPlayer player)) {
            lastTrackedDistance = -1.0;
            return;
        }

        double distance = distanceTo(player);
        if (lastTrackedDistance >= 0 && distance - lastTrackedDistance > FLEE_DISTANCE_GAIN) {
            fleeSignals++;
            tryLearn(player, Tactic.FAST_CHASE,
                    "Player retreated from Nemesis " + fleeSignals + " times",
                    FLEE_LEARN_THRESHOLD, fleeSignals);
        }
        lastTrackedDistance = distance;
    }

    /** Switches tactic and notifies the HUD once {@code progress} reaches {@code threshold}. */
    private void tryLearn(ServerPlayer player, Tactic proposedTactic, String reason, int threshold, int progress) {
        if (progress < threshold || tactic == proposedTactic) {
            return;
        }
        setTactic(proposedTactic);
        LearningResult result = new LearningResult(proposedTactic, reason, computeAdaptationLevel());
        NemesisFeedback.broadcastLearning(player, result);
    }

    private float computeAdaptationLevel() {
        float shieldProgress = Math.min(1f, shieldBlocks / (float) SHIELD_LEARN_THRESHOLD);
        float rangedProgress = Math.min(1f, rangedHits / (float) RANGED_LEARN_THRESHOLD);
        float fleeProgress = Math.min(1f, fleeSignals / (float) FLEE_LEARN_THRESHOLD);
        return (shieldProgress + rangedProgress + fleeProgress) / 3f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_TACTIC, tactic.name());
        tag.putInt(TAG_SHIELD_BLOCKS, shieldBlocks);
        tag.putInt(TAG_RANGED_HITS, rangedHits);
        tag.putInt(TAG_FLEE_SIGNALS, fleeSignals);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shieldBlocks = tag.getInt(TAG_SHIELD_BLOCKS);
        rangedHits = tag.getInt(TAG_RANGED_HITS);
        fleeSignals = tag.getInt(TAG_FLEE_SIGNALS);
        if (tag.contains(TAG_TACTIC)) {
            try {
                setTactic(Tactic.valueOf(tag.getString(TAG_TACTIC)));
            } catch (IllegalArgumentException ignored) {
                setTactic(Tactic.NORMAL);
            }
        }
    }

    private boolean hasLivingTarget() {
        return getTarget() != null && getTarget().isAlive();
    }

    private boolean isInMeleeRange(LivingEntity target) {
        double reach = getBbWidth() * 2.0F;
        return distanceToSqr(target) <= reach * reach + target.getBbWidth();
    }

    private final class TacticMeleeGoal extends MeleeAttackGoal {
        private TacticMeleeGoal() {
            super(NemesisEntity.this, 1.0, false);
        }

        @Override
        public boolean canUse() {
            return (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE)
                    && super.canContinueToUse();
        }
    }

    private abstract class TacticGoal extends Goal {
        private final Tactic requiredTactic;

        private TacticGoal(Tactic requiredTactic) {
            this.requiredTactic = requiredTactic;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return tactic == requiredTactic && hasLivingTarget();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }
    }

    private final class DelayedAttackGoal extends TacticGoal {
        private int attackDelay;

        private DelayedAttackGoal() {
            super(Tactic.DELAYED_ATTACK);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (!isInMeleeRange(target)) {
                attackDelay = 0;
                getNavigation().moveTo(target, 1.0);
            } else {
                getNavigation().stop();
                if (attackDelay == 0) attackDelay = 8 + getRandom().nextInt(7);
                if (--attackDelay == 0) doHurtTarget(target);
            }
        }

        @Override
        public void stop() {
            attackDelay = 0;
        }
    }

    private final class ZigzagApproachGoal extends TacticGoal {
        private int direction = 1;
        private int pathTicks;
        private int attackCooldown;

        private ZigzagApproachGoal() {
            super(Tactic.ZIGZAG_APPROACH);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (isInMeleeRange(target)) {
                getNavigation().stop();
                if (attackCooldown-- <= 0) {
                    doHurtTarget(target);
                    attackCooldown = 20;
                }
                return;
            }
            if (--pathTicks <= 0) {
                Vec3 toTarget = target.position().subtract(position()).normalize();
                Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x).scale(1.6 * direction);
                Vec3 waypoint = position().add(toTarget.scale(3.0)).add(side);
                getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 1.0);
                direction = -direction;
                pathTicks = 10;
            }
            if (attackCooldown > 0) attackCooldown--;
        }
    }

    private final class RangedAttackGoal extends TacticGoal {
        private int cooldown;

        private RangedAttackGoal() {
            super(Tactic.RANGED_ATTACK);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceTo(target) < 7.0F) getNavigation().stop();
            else getNavigation().moveTo(target, 0.8);
            if (cooldown-- <= 0 && getSensing().hasLineOfSight(target)) {
                Arrow projectile = new Arrow(level(), NemesisEntity.this,
                        new ItemStack(Items.ARROW), null);
                double dy = target.getY(0.5) - projectile.getY();
                projectile.shoot(target.getX() - getX(), dy, target.getZ() - getZ(), 1.5F, 4.0F);
                level().addFreshEntity(projectile);
                cooldown = 40;
            }
        }

        @Override
        public void stop() {
            cooldown = 0;
        }
    }
}
