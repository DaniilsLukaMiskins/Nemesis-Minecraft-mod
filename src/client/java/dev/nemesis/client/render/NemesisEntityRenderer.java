package dev.nemesis.client.render;

import dev.nemesis.entity.NemesisEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Uses Minecraft's zombie model and texture so no custom art is required. */
public final class NemesisEntityRenderer extends MobEntityRenderer<NemesisEntity, BipedEntityModel<NemesisEntity>> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/zombie/zombie.png");

    public NemesisEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public Identifier getTexture(NemesisEntity entity) {
        return TEXTURE;
    }
}
