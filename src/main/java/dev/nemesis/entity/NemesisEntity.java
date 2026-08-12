package dev.nemesis.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/** A simple hostile mob whose behavior is intentionally composed from vanilla goals. */
public final class NemesisEntity extends HostileEntity {
    private static final double NORMAL_MOVEMENT_SPEED = 0.25;
    private static final double FAST_MOVEMENT_SPEED = 0.38;
    private Tactic tactic = Tactic.NORMAL;

    public NemesisEntity(EntityType<? extends NemesisEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, NORMAL_MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new TacticMeleeGoal());
        goalSelector.add(2, new DelayedAttackGoal());
        goalSelector.add(2, new ZigzagApproachGoal());
        goalSelector.add(2, new RangedAttackGoal());
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public void setTactic(Tactic tactic) {
        this.tactic = java.util.Objects.requireNonNull(tactic, "tactic");
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(tactic == Tactic.FAST_CHASE ? FAST_MOVEMENT_SPEED : NORMAL_MOVEMENT_SPEED);
        getNavigation().stop();
    }

    public Tactic getTactic() {
        return tactic;
    }

    private boolean hasLivingTarget() {
        return getTarget() != null && getTarget().isAlive();
    }

    private boolean isInMeleeRange(LivingEntity target) {
        double reach = getWidth() * 2.0F;
        return squaredDistanceTo(target) <= reach * reach + target.getWidth();
    }

    private final class TacticMeleeGoal extends MeleeAttackGoal {
        private TacticMeleeGoal() {
            super(NemesisEntity.this, 1.0, false);
        }

        @Override
        public boolean canStart() {
            return (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE) && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return (tactic == Tactic.NORMAL || tactic == Tactic.FAST_CHASE) && super.shouldContinue();
        }
    }

    private abstract class TacticGoal extends Goal {
        private final Tactic requiredTactic;

        private TacticGoal(Tactic requiredTactic) {
            this.requiredTactic = requiredTactic;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return tactic == requiredTactic && hasLivingTarget();
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
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
            getLookControl().lookAt(target, 30.0F, 30.0F);
            if (!isInMeleeRange(target)) {
                attackDelay = 0;
                getNavigation().startMovingTo(target, 1.0);
            } else {
                getNavigation().stop();
                if (attackDelay == 0) attackDelay = 8 + getRandom().nextInt(7);
                if (--attackDelay == 0) tryAttack(target);
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
            getLookControl().lookAt(target, 30.0F, 30.0F);
            if (isInMeleeRange(target)) {
                getNavigation().stop();
                if (attackCooldown-- <= 0) {
                    tryAttack(target);
                    attackCooldown = 20;
                }
                return;
            }
            if (--pathTicks <= 0) {
                Vec3d toTarget = target.getPos().subtract(getPos()).normalize();
                Vec3d side = new Vec3d(-toTarget.z, 0.0, toTarget.x).multiply(1.6 * direction);
                Vec3d waypoint = getPos().add(toTarget.multiply(3.0)).add(side);
                getNavigation().startMovingTo(waypoint.x, waypoint.y, waypoint.z, 1.0);
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
            getLookControl().lookAt(target, 30.0F, 30.0F);
            double distance = Math.sqrt(squaredDistanceTo(target));
            if (distance < 7.0) getNavigation().stop();
            else getNavigation().startMovingTo(target, 0.8);
            if (cooldown-- <= 0 && canSee(target)) {
                ArrowEntity projectile = new ArrowEntity(getWorld(), NemesisEntity.this,
                        new ItemStack(Items.ARROW), null);
                double dy = target.getBodyY(0.5) - projectile.getY();
                projectile.setVelocity(target.getX() - getX(), dy,
                        target.getZ() - getZ(), 1.5F, 4.0F);
                getWorld().spawnEntity(projectile);
                cooldown = 40;
            }
        }

        @Override
        public void stop() {
            cooldown = 0;
        }
    }
}
