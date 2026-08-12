package com.sen2x.nemesisai.entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NemesisEntity extends Zombie {
    private static final int LEARNING_THRESHOLD = 3;
    private static final float MELEE_DAMAGE_MULTIPLIER = 0.5F;
    private static final float RANGED_DAMAGE_MULTIPLIER = 0.65F;

    private int meleeHits;
    private int rangedHits;

    private boolean learnedMelee;
    private boolean learnedRanged;

    public NemesisEntity(
            EntityType<? extends Zombie> entityType,
            Level level
    ) {
        super(entityType, level);
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

        if (!level().isClientSide()
                && wasHurt
                && source.getEntity() instanceof Player player) {

            if (projectile) {
                learnRanged(player);
            } else {
                learnMelee(player);
            }
        }

        return wasHurt;
    }

    private void learnMelee(Player player) {
        meleeHits++;

        if (meleeHits >= LEARNING_THRESHOLD && !learnedMelee) {
            learnedMelee = true;

            player.displayClientMessage(
                    Component.literal("LEARNED: MELEE ATTACKS"),
                    true
            );

            player.displayClientMessage(
                    Component.literal("TACTIC CHANGED: MELEE RESISTANCE"),
                    false
            );
        }
    }

    private void learnRanged(Player player) {
        rangedHits++;

        if (rangedHits >= LEARNING_THRESHOLD && !learnedRanged) {
            learnedRanged = true;

            player.displayClientMessage(
                    Component.literal("LEARNED: RANGED ATTACKS"),
                    true
            );

            player.displayClientMessage(
                    Component.literal("TACTIC CHANGED: PROJECTILE RESISTANCE"),
                    false
            );
        }
    }
@Override
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);

    tag.putInt("NemesisMeleeHits", meleeHits);
    tag.putInt("NemesisRangedHits", rangedHits);
    tag.putBoolean("NemesisLearnedMelee", learnedMelee);
    tag.putBoolean("NemesisLearnedRanged", learnedRanged);
}

@Override
public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);

    meleeHits = tag.getInt("NemesisMeleeHits");
    rangedHits = tag.getInt("NemesisRangedHits");
    learnedMelee = tag.getBoolean("NemesisLearnedMelee");
    learnedRanged = tag.getBoolean("NemesisLearnedRanged");
}
    public int getMeleeHits() {
        return meleeHits;
    }

    public int getRangedHits() {
        return rangedHits;
    }

    public boolean hasLearnedMelee() {
        return learnedMelee;
    }

    public boolean hasLearnedRanged() {
        return learnedRanged;
    }
}