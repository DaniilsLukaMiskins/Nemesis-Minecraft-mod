package com.sen2x.nemesisai.mixin;

import com.sen2x.nemesisai.parasite.ParasiteHostState;
import com.sen2x.nemesisai.parasite.ParasiteLifecycle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalParasiteMixin implements ParasiteHostState {
    @Unique private int nemesisAi$incubationTicks;

    @Override public boolean nemesisAi$isInfected() { return nemesisAi$incubationTicks > 0; }
    @Override public int nemesisAi$getIncubationTicks() { return nemesisAi$incubationTicks; }
    @Override public void nemesisAi$infect(int ticks) { nemesisAi$incubationTicks = ticks; }
    @Override public void nemesisAi$setIncubationTicks(int ticks) { nemesisAi$incubationTicks = ticks; }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void nemesisAi$tickInfection(CallbackInfo ci) {
        if (nemesisAi$incubationTicks > 0) ParasiteLifecycle.tick((Animal) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void nemesisAi$saveInfection(CompoundTag tag, CallbackInfo ci) {
        if (nemesisAi$incubationTicks > 0) tag.putInt("NemesisParasiteTicks", nemesisAi$incubationTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void nemesisAi$loadInfection(CompoundTag tag, CallbackInfo ci) {
        nemesisAi$incubationTicks = Math.max(0, tag.getInt("NemesisParasiteTicks"));
    }
}
