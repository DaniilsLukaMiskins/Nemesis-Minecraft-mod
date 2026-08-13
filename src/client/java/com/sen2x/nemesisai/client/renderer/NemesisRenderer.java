package com.sen2x.nemesisai.client.renderer;

import com.sen2x.nemesisai.client.model.NemesisModel;
import com.sen2x.nemesisai.entity.NemesisEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NemesisRenderer extends GeoEntityRenderer<NemesisEntity> {
    public NemesisRenderer(EntityRendererProvider.Context context) {
        super(context, new NemesisModel());
        this.shadowRadius = 0.75F;
    }
}
