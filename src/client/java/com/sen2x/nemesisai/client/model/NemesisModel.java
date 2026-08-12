package com.sen2x.nemesisai.client.model;

import com.sen2x.nemesisai.NemesisAiMod;
import com.sen2x.nemesisai.entity.NemesisEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NemesisModel extends GeoModel<NemesisEntity> {
    @Override
    public ResourceLocation getModelResource(NemesisEntity entity) {
        return NemesisAiMod.id("geo/nemesis.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NemesisEntity entity) {
        return NemesisAiMod.id("textures/entity/nemesis.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NemesisEntity entity) {
        return NemesisAiMod.id("animations/nemesis.animation.json");
    }
}
