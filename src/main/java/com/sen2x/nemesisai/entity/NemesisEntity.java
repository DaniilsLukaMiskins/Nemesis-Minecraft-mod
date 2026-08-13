package com.sen2x.nemesisai.entity;

import com.sen2x.nemesisai.api.LearningResult;
import com.sen2x.nemesisai.api.NemesisFeedback;
import com.sen2x.nemesisai.api.NemesisMemoryStore;
import com.sen2x.nemesisai.api.Tactic;
import com.sen2x.nemesisai.parasite.ParasiteHostState;
import com.sen2x.nemesisai.parasite.ParasiteHosts;
import com.sen2x.nemesisai.parasite.ParasiteLifecycle;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NemesisEntity extends Zombie implements GeoEntity {
    private static final double COMBAT_RANGE_SQR = 50.0 * 50.0;
    private static final double AMBUSH_MIN_TARGET_DISTANCE_SQR = 50.0 * 50.0;
    private static final double AMBUSH_MAX_TARGET_DISTANCE_SQR = 100.0 * 100.0;
    private static final double AMBUSH_LEAD_DISTANCE = 20.0;
    private static final int AMBUSH_WAIT_DURATION = 600;
    private enum Habit {
        SHIELD(Tactic.DELAYED_ATTACK, "blocking with a shield"),
        RANGED(Tactic.ZIGZAG_APPROACH, "using ranged attacks"),
        FLEE(Tactic.FAST_CHASE, "retreating"),
        TOWER(Tactic.RANGED_ATTACK, "building out of reach");
        final Tactic counter;
        final String reason;
        Habit(Tactic counter, String reason) { this.counter = counter; this.reason = reason; }
    }
    private static final float HABIT_REWARD = 1.0F;
    private static final float HABIT_MAX = 10.0F;
    private static final float HABIT_DECAY = 0.97F;
    private static final float HABIT_THRESHOLD = 3.0F;
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
    private final Map<Long, Integer> routeVisits = new HashMap<>();
    private BlockPos lastRouteSample;
    private BlockPos ambushPoint;
    private int routeConfidence;
    private int ambushWaitTicks;
    private final float[] habitWeights = new float[Habit.values().length];
    private double lastTrackedDistance = -1.0;
    private int habitObservationTimer;
    private int parasiteCooldown;

    public NemesisEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ParasiteAnimalGoal());
        goalSelector.addGoal(1, new AmbushGoal());
        goalSelector.addGoal(1, new StalkRouteGoal());
        goalSelector.addGoal(2, new NormalMeleeGoal());
        goalSelector.addGoal(2, new DelayedAttackGoal());
        goalSelector.addGoal(2, new ZigzagGoal());
        goalSelector.addGoal(2, new RangedGoal());
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (tactic == Tactic.DELAYED_ATTACK && tacticCooldown > 0) {
            return false;
        }
        if (!level().isClientSide()) {
            if (target instanceof ServerPlayer player && player.isBlocking()) rewardHabit(Habit.SHIELD, player);
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
        if (parasiteCooldown > 0) parasiteCooldown--;
        if (level().isClientSide()) return;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        // Sample quickly enough to enter stalking mode before ordinary pursuit closes the gap.
        double targetDistanceSqr = distanceToSqr(target);
        if (target instanceof Player player && tickCount % 10 == 0
                && targetDistanceSqr > COMBAT_RANGE_SQR
                && targetDistanceSqr <= AMBUSH_MAX_TARGET_DISTANCE_SQR) {
            observeRoute(player);
        }
        if (--habitObservationTimer <= 0) {
            habitObservationTimer = 20;
            for (int i = 0; i < habitWeights.length; i++) habitWeights[i] *= HABIT_DECAY;
            if (target instanceof ServerPlayer player) {
                double distance = distanceTo(player);
                if (lastTrackedDistance >= 0 && distance - lastTrackedDistance > 1.5) rewardHabit(Habit.FLEE, player);
                lastTrackedDistance = distance;
                boolean tower = player.getY() - getY() > 3.0 && getNavigation().isDone() && !inMeleeRange(player);
                if (tower) rewardHabit(Habit.TOWER, player);
            }
        }
    }

    private void observeRoute(Player player) {
        BlockPos current = player.blockPosition();
        if (lastRouteSample != null && current.distSqr(lastRouteSample) < 16.0) return;
        BlockPos previous = lastRouteSample;
        lastRouteSample = current;

        // Eight-block cells tolerate small deviations while still representing a route.
        BlockPos cell = new BlockPos(Math.floorDiv(current.getX(), 8) * 8,
                current.getY(), Math.floorDiv(current.getZ(), 8) * 8);
        int visits = routeVisits.merge(cell.asLong(), 1, Integer::sum);
        routeConfidence = Math.max(routeConfidence, visits);
        double targetDistance = distanceToSqr(player);
        if (visits >= 3 && previous != null
                && targetDistance >= AMBUSH_MIN_TARGET_DISTANCE_SQR
                && targetDistance <= AMBUSH_MAX_TARGET_DISTANCE_SQR) {
            Vec3 direction = new Vec3(current.getX() - previous.getX(), 0,
                    current.getZ() - previous.getZ());
            if (direction.lengthSqr() > 0.25) {
                Vec3 ahead = Vec3.atCenterOf(current).add(direction.normalize().scale(AMBUSH_LEAD_DISTANCE));
                ambushPoint = BlockPos.containing(ahead.x, current.getY(), ahead.z);
                ambushWaitTicks = AMBUSH_WAIT_DURATION;
                getNavigation().stop();
            }
        }
        if (routeVisits.size() > 32) {
            routeVisits.entrySet().removeIf(entry -> entry.getValue() <= 1);
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

    private boolean hasTarget() {
        return getTarget() != null && getTarget().isAlive();
    }

    private boolean targetInCombatRange() {
        return hasTarget() && distanceToSqr(getTarget()) <= COMBAT_RANGE_SQR;
    }

    private boolean inMeleeRange(LivingEntity target) {
        double reach = getBbWidth() * 2.0F;
        return distanceToSqr(target) <= reach * reach + target.getBbWidth();
    }

    private final class ParasiteAnimalGoal extends Goal {
        private Animal host;
        private int repathTicks;

        private ParasiteAnimalGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (parasiteCooldown > 0) return false;
            host = level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(16.0),
                            animal -> animal.isAlive() && ParasiteHosts.isSupported(animal)
                                    && !((ParasiteHostState) animal).nemesisAi$isInfected())
                    .stream().min(Comparator.comparingDouble(NemesisEntity.this::distanceToSqr)).orElse(null);
            return host != null;
        }

        @Override public boolean canContinueToUse() {
            return host != null && host.isAlive() && parasiteCooldown == 0
                    && !((ParasiteHostState) host).nemesisAi$isInfected() && distanceToSqr(host) < 24.0 * 24.0;
        }

        @Override public void tick() {
            if (host == null) return;
            getLookControl().setLookAt(host, 30.0F, 30.0F);
            if (inMeleeRange(host)) {
                getNavigation().stop();
                if (ParasiteLifecycle.infect(host)) {
                    triggerAnim("actions", "bite");
                    parasiteCooldown = 20 * 20;
                }
            } else if (--repathTicks <= 0) {
                repathTicks = 10;
                getNavigation().moveTo(host, 1.0);
            }
        }

        @Override public void stop() { host = null; repathTicks = 0; }
    }

    private final class NormalMeleeGoal extends MeleeAttackGoal {
        private NormalMeleeGoal() { super(NemesisEntity.this, 1.0, false); }

        @Override public boolean canUse() {
            return targetInCombatRange()
                    && (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE) && super.canUse();
        }
        @Override public boolean canContinueToUse() {
            return targetInCombatRange()
                    && (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE) && super.canContinueToUse();
        }
    }

    private abstract class TacticGoal extends Goal {
        private final Tactic required;
        private TacticGoal(Tactic required) {
            this.required = required;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        @Override public boolean canUse() { return tactic == required && targetInCombatRange(); }
        @Override public boolean canContinueToUse() { return canUse(); }
    }

    private final class DelayedAttackGoal extends TacticGoal {
        private int windup;
        private DelayedAttackGoal() { super(Tactic.DELAYED_ATTACK); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30, 30);
            if (!inMeleeRange(target)) {
                windup = 0;
                getNavigation().moveTo(target, 1.0);
            } else {
                getNavigation().stop();
                if (windup == 0 && tacticCooldown == 0) windup = 8 + random.nextInt(7);
                if (windup > 0 && --windup == 0) doHurtTarget(target);
            }
        }
        @Override public void stop() { windup = 0; }
    }

    private final class ZigzagGoal extends TacticGoal {
        private int direction = 1;
        private int pathTicks;
        private int attackCooldown;
        private ZigzagGoal() { super(Tactic.ZIGZAG_APPROACH); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30, 30);
            if (inMeleeRange(target)) {
                getNavigation().stop();
                if (attackCooldown-- <= 0) {
                    doHurtTarget(target);
                    attackCooldown = 20;
                }
                return;
            }
            if (--pathTicks <= 0) {
                Vec3 forward = target.position().subtract(position()).normalize();
                Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(2.5 * direction);
                Vec3 waypoint = position().add(forward.scale(4.0)).add(side);
                getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 1.08);
                direction = -direction;
                pathTicks = 12;
            }
            if (attackCooldown > 0) attackCooldown--;
        }
    }

    private final class RangedGoal extends TacticGoal {
        private int shotCooldown;
        private RangedGoal() { super(Tactic.RANGED_ATTACK); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30, 30);
            double distance = distanceTo(target);
            if (distance < 6.0) {
                Vec3 away = position().subtract(target.position()).normalize();
                Vec3 retreat = position().add(away.scale(4));
                getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.0);
            } else if (distance > 12.0) {
                getNavigation().moveTo(target, 0.9);
            } else {
                getNavigation().stop();
            }
            if (shotCooldown-- <= 0 && getSensing().hasLineOfSight(target)) {
                BlueSlimeProjectile slime = new BlueSlimeProjectile(level(), NemesisEntity.this);
                slime.shoot(target.getX() - getX(), target.getEyeY() - slime.getY(),
                        target.getZ() - getZ(), 1.5F, 4.0F);
                level().addFreshEntity(slime);
                shotCooldown = 40;
            }
        }
        @Override public void stop() { shotCooldown = 0; }
    }

    private final class AmbushGoal extends Goal {
        private AmbushGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return ambushPoint != null && ambushWaitTicks > 0 && routeConfidence >= 3 && hasTarget();
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null || ambushPoint == null) return;
            ambushWaitTicks--;
            double pointDistance = distanceToSqr(Vec3.atCenterOf(ambushPoint));
            if (pointDistance > 4.0) {
                getNavigation().moveTo(ambushPoint.getX() + 0.5, ambushPoint.getY(),
                        ambushPoint.getZ() + 0.5, 1.25);
                setSprinting(true);
            } else {
                getNavigation().stop();
                setSprinting(false);
                getLookControl().setLookAt(target, 30, 30);
                if (distanceToSqr(target) < 12.25 && tacticCooldown == 0) {
                    doHurtTarget(target);
                    ambushPoint = null;
                    routeConfidence = 0;
                }
            }
            if (ambushWaitTicks <= 0) ambushPoint = null;
        }
        @Override public void stop() { setSprinting(tactic == Tactic.FAST_CHASE); }
    }

    /** Watches a partly learned route without rushing into melee and ruining the ambush. */
    private final class StalkRouteGoal extends Goal {
        private int repositionTicks;
        private StalkRouteGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            if (!hasTarget() || ambushPoint != null) return false;
            double distance = distanceToSqr(getTarget());
            return distance > COMBAT_RANGE_SQR && distance <= AMBUSH_MAX_TARGET_DISTANCE_SQR;
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 20, 20);
            double distance = distanceTo(target);
            if (--repositionTicks > 0) return;
            repositionTicks = 12;

            Vec3 fromTarget = position().subtract(target.position());
            if (fromTarget.lengthSqr() < 0.1) fromTarget = new Vec3(1, 0, 0);
            Vec3 radial = fromTarget.normalize();
            if (distance < 58.0) {
                Vec3 retreat = target.position().add(radial.scale(70.0));
                getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.15);
                setSprinting(true);
            } else if (distance > 85.0) {
                Vec3 watch = target.position().add(radial.scale(72.0));
                getNavigation().moveTo(watch.x, watch.y, watch.z, 1.0);
                setSprinting(false);
            } else {
                // Shift sideways so stalking is visible but keep a safe observation distance.
                Vec3 side = new Vec3(-radial.z, 0, radial.x).scale(random.nextBoolean() ? 5.0 : -5.0);
                Vec3 watch = position().add(side);
                getNavigation().moveTo(watch.x, watch.y, watch.z, 0.8);
                setSprinting(false);
            }
        }
        @Override public void stop() { setSprinting(tactic == Tactic.FAST_CHASE); }
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
                if (projectile && player instanceof ServerPlayer serverPlayer) rewardHabit(Habit.RANGED, serverPlayer);
                if (projectile) learnRanged(player); else learnMelee(player);
            }
        }
        return wasHurt;
    }

    private void rewardHabit(Habit habit, ServerPlayer player) {
        int index = habit.ordinal();
        habitWeights[index] = Math.min(HABIT_MAX, habitWeights[index] + HABIT_REWARD);
        Habit dominant = null;
        float best = HABIT_THRESHOLD;
        for (Habit candidate : Habit.values()) {
            float weight = habitWeights[candidate.ordinal()];
            if (weight > best) { best = weight; dominant = candidate; }
        }
        if (dominant == null || tactic == dominant.counter) return;
        setTactic(dominant.counter);
        float level = 0;
        for (float weight : habitWeights) level += Math.min(1F, weight / HABIT_THRESHOLD);
        LearningResult result = new LearningResult(dominant.counter,
                "Player habit: " + dominant.reason + " (" + String.format(Locale.ROOT, "%.1f", best) + ")",
                level / habitWeights.length);
        NemesisMemoryStore.record(player.getUUID(), result);
        NemesisFeedback.broadcastLearning(player, result);
    }

    public void resetLearning() {
        Arrays.fill(habitWeights, 0F);
        routeVisits.clear();
        routeConfidence = 0;
        ambushPoint = null;
        ambushWaitTicks = 0;
        meleeHits = rangedHits = 0;
        learnedMelee = learnedRanged = false;
        lastTrackedDistance = -1;
        setTactic(Tactic.NORMAL);
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
            setTactic(Tactic.DELAYED_ATTACK);
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
            setTactic(Tactic.ZIGZAG_APPROACH);
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
        tag.putInt("NemesisRouteConfidence", routeConfidence);
        tag.putInt("NemesisAmbushWait", ambushWaitTicks);
        tag.putInt("NemesisParasiteCooldown", parasiteCooldown);
        if (ambushPoint != null) tag.putLong("NemesisAmbushPoint", ambushPoint.asLong());
        long[] routeCells = new long[routeVisits.size()];
        int[] routeCounts = new int[routeVisits.size()];
        int routeIndex = 0;
        for (Map.Entry<Long, Integer> entry : routeVisits.entrySet()) {
            routeCells[routeIndex] = entry.getKey();
            routeCounts[routeIndex] = entry.getValue();
            routeIndex++;
        }
        tag.putLongArray("NemesisRouteCells", routeCells);
        tag.putIntArray("NemesisRouteCounts", routeCounts);
        for (Habit habit : Habit.values()) tag.putFloat("NemesisHabit" + habit.name(), habitWeights[habit.ordinal()]);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        meleeHits = tag.getInt("NemesisMeleeHits");
        rangedHits = tag.getInt("NemesisRangedHits");
        learnedMelee = tag.getBoolean("NemesisLearnedMelee");
        learnedRanged = tag.getBoolean("NemesisLearnedRanged");
        routeConfidence = tag.getInt("NemesisRouteConfidence");
        ambushWaitTicks = tag.getInt("NemesisAmbushWait");
        parasiteCooldown = tag.getInt("NemesisParasiteCooldown");
        if (tag.contains("NemesisAmbushPoint")) ambushPoint = BlockPos.of(tag.getLong("NemesisAmbushPoint"));
        long[] routeCells = tag.getLongArray("NemesisRouteCells");
        int[] routeCounts = tag.getIntArray("NemesisRouteCounts");
        routeVisits.clear();
        for (int i = 0; i < Math.min(routeCells.length, routeCounts.length); i++) {
            routeVisits.put(routeCells[i], routeCounts[i]);
        }
        for (Habit habit : Habit.values()) habitWeights[habit.ordinal()] = tag.getFloat("NemesisHabit" + habit.name());
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
    public int getRouteConfidence() { return routeConfidence; }
    public BlockPos getAmbushPoint() { return ambushPoint; }
}
