package com.sen2x.nemesisai.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sen2x.nemesisai.entity.BlueSlimeProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class BlueSlimeProjectileRenderer extends ThrownItemRenderer<BlueSlimeProjectile> {
    public BlueSlimeProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, 1.25F, true);
    }

    @Override
    public void render(BlueSlimeProjectile entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float pulse = 1.0F + 0.16F * (float)Math.sin((entity.tickCount + partialTick) * 0.55F);
        pose.scale(pulse, pulse, pulse);
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 24F));
        pose.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTick) * 13F));
        super.render(entity, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }
}
