package com.sen2x.nemesisai.client.model;

import com.sen2x.nemesisai.NemesisAiMod;
import com.sen2x.nemesisai.entity.BabyNemesisEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BabyNemesisModel extends GeoModel<BabyNemesisEntity> {
    @Override public ResourceLocation getModelResource(BabyNemesisEntity entity) {
        return NemesisAiMod.id("geo/nemesis.geo.json");
    }
    @Override public ResourceLocation getTextureResource(BabyNemesisEntity entity) {
        return NemesisAiMod.id("textures/entity/baby_nemesis.png");
    }
    @Override public ResourceLocation getAnimationResource(BabyNemesisEntity entity) {
        return NemesisAiMod.id("animations/nemesis.animation.json");
    }
}
