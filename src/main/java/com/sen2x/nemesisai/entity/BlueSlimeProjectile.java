package com.sen2x.nemesisai.entity;

import com.sen2x.nemesisai.ModEntities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class BlueSlimeProjectile extends ThrowableItemProjectile {
    public BlueSlimeProjectile(EntityType<? extends BlueSlimeProjectile> type, Level level) {
        super(type, level);
    }

    public BlueSlimeProjectile(Level level, LivingEntity owner) {
        super(ModEntities.BLUE_SLIME_PROJECTILE, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModEntities.BLUE_SLIME_GLOB;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            hit.getEntity().hurt(damageSources().thrown(this, getOwner()), 5.0F);
            if (hit.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }
}
