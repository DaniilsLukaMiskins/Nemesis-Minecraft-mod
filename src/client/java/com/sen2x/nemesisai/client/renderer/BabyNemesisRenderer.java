package com.sen2x.nemesisai.client.renderer;

import com.sen2x.nemesisai.client.model.BabyNemesisModel;
import com.sen2x.nemesisai.entity.BabyNemesisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BabyNemesisRenderer extends GeoEntityRenderer<BabyNemesisEntity> {
    public BabyNemesisRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyNemesisModel());
        shadowRadius = 0.3F;
    }

    @Override public void render(BabyNemesisEntity entity, float yaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(0.42F, 0.42F, 0.42F);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
}
