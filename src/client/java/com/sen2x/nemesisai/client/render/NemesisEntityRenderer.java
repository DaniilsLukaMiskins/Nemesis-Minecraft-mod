package com.sen2x.nemesisai.client.render;

import com.sen2x.nemesisai.entity.NemesisEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Uses Minecraft's zombie model and texture to keep this prototype asset-free. */
public final class NemesisEntityRenderer extends MobRenderer<NemesisEntity, HumanoidModel<NemesisEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public NemesisEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NemesisEntity entity) {
        return TEXTURE;
    }
}
