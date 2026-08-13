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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;

public final class NemesisEntity extends Monster {
    private static final double NORMAL_MOVEMENT_SPEED = 0.25;
    private static final double FAST_MOVEMENT_SPEED = 0.38;

    /** A player habit Nemesis can pick up on, and the tactic it counters with. */
    private enum Habit {
        SHIELD(Tactic.DELAYED_ATTACK, "blocking with a shield"),
        RANGED(Tactic.ZIGZAG_APPROACH, "using ranged attacks"),
        FLEE(Tactic.FAST_CHASE, "retreating"),
        TOWER(Tactic.RANGED_ATTACK, "building up out of reach");

        private final Tactic counterTactic;
        private final String description;

        Habit(Tactic counterTactic, String description) {
            this.counterTactic = counterTactic;
            this.description = description;
        }
    }

    // Habit-profile tuning: each observed behavior adds REWARD to its habit's weight (capped at
    // MAX_WEIGHT); weights decay every OBSERVATION_INTERVAL_TICKS so unused habits fade instead
    // of sticking forever. Whichever habit is dominant above LEARN_THRESHOLD wins the tactic.
    private static final float REWARD = 1.0f;
    private static final float MAX_WEIGHT = 10.0f;
    private static final float DECAY_FACTOR = 0.97f;
    private static final float LEARN_THRESHOLD = 3.0f;
    private static final int OBSERVATION_INTERVAL_TICKS = 20;
    private static final double FLEE_DISTANCE_GAIN = 1.5;
    private static final double TOWER_HEIGHT_GAP = 3.0;

    private static final String TAG_TACTIC = "NemesisTactic";
    private static final String TAG_SHIELD_WEIGHT = "NemesisShieldWeight";
    private static final String TAG_RANGED_WEIGHT = "NemesisRangedWeight";
    private static final String TAG_FLEE_WEIGHT = "NemesisFleeWeight";
    private static final String TAG_TOWER_WEIGHT = "NemesisTowerWeight";

    private Tactic tactic = Tactic.NORMAL;

    // Learned player-habit weights, persisted via addAdditionalSaveData/readAdditionalSaveData
    // so Nemesis remembers a player's habits across server restarts.
    private final float[] habitWeights = new float[Habit.values().length];

    private double lastTrackedDistance = -1.0;
    private int observationTimer;

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
        Arrays.fill(habitWeights, 0f);
        lastTrackedDistance = -1.0;
        setTactic(Tactic.NORMAL);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (!level().isClientSide() && wasHurt
                && source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getEntity() instanceof ServerPlayer player) {
            reward(Habit.RANGED, player);
        }
        return wasHurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide() && target instanceof ServerPlayer player && player.isBlocking()) {
            reward(Habit.SHIELD, player);
        }
        return super.doHurtTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (--observationTimer > 0) {
            return;
        }
        observationTimer = OBSERVATION_INTERVAL_TICKS;

        for (int i = 0; i < habitWeights.length; i++) {
            habitWeights[i] *= DECAY_FACTOR;
        }

        if (!(getTarget() instanceof ServerPlayer player)) {
            lastTrackedDistance = -1.0;
            return;
        }

        double distance = distanceTo(player);
        if (lastTrackedDistance >= 0 && distance - lastTrackedDistance > FLEE_DISTANCE_GAIN) {
            reward(Habit.FLEE, player);
        }
        lastTrackedDistance = distance;

        boolean heightGap = player.getY() - getY() > TOWER_HEIGHT_GAP;
        boolean pathUnreachable = getNavigation().isDone() && !isInMeleeRange(player);
        if (heightGap && pathUnreachable) {
            reward(Habit.TOWER, player);
        }
    }

    /** Reinforces a habit's weight and re-evaluates which tactic Nemesis should be using. */
    private void reward(Habit habit, ServerPlayer player) {
        int index = habit.ordinal();
        habitWeights[index] = Math.min(MAX_WEIGHT, habitWeights[index] + REWARD);
        evaluateTactic(player);
    }

    /** Picks the dominant habit (if any exceeds the learn threshold) and switches to its counter-tactic. */
    private void evaluateTactic(ServerPlayer player) {
        Habit dominant = null;
        float best = LEARN_THRESHOLD;
        for (Habit habit : Habit.values()) {
            float weight = habitWeights[habit.ordinal()];
            if (weight > best) {
                best = weight;
                dominant = habit;
            }
        }

        if (dominant == null || tactic == dominant.counterTactic) {
            return;
        }

        setTactic(dominant.counterTactic);
        String reason = "Player's habit of " + dominant.description + " reached weight "
                + String.format(Locale.ROOT, "%.1f", best);
        NemesisFeedback.broadcastLearning(player, new LearningResult(dominant.counterTactic, reason, computeAdaptationLevel()));
    }

    private float computeAdaptationLevel() {
        float sum = 0f;
        for (float weight : habitWeights) {
            sum += Math.min(1f, weight / LEARN_THRESHOLD);
        }
        return sum / habitWeights.length;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_TACTIC, tactic.name());
        tag.putFloat(TAG_SHIELD_WEIGHT, habitWeights[Habit.SHIELD.ordinal()]);
        tag.putFloat(TAG_RANGED_WEIGHT, habitWeights[Habit.RANGED.ordinal()]);
        tag.putFloat(TAG_FLEE_WEIGHT, habitWeights[Habit.FLEE.ordinal()]);
        tag.putFloat(TAG_TOWER_WEIGHT, habitWeights[Habit.TOWER.ordinal()]);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        habitWeights[Habit.SHIELD.ordinal()] = tag.getFloat(TAG_SHIELD_WEIGHT);
        habitWeights[Habit.RANGED.ordinal()] = tag.getFloat(TAG_RANGED_WEIGHT);
        habitWeights[Habit.FLEE.ordinal()] = tag.getFloat(TAG_FLEE_WEIGHT);
        habitWeights[Habit.TOWER.ordinal()] = tag.getFloat(TAG_TOWER_WEIGHT);
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
