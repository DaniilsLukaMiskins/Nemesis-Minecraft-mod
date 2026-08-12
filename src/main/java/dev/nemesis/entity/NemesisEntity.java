package dev.nemesis.entity;

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
    private Tactic tactic = Tactic.NORMAL;

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
