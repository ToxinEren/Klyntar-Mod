package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.entity.client.model.GhastProjectileModel;
import modKlyntar.entity.custom.GhastProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GhastProjectileRenderer extends GeoEntityRenderer<GhastProjectileEntity> {
    public GhastProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GhastProjectileModel());
    }

    @Override
    public void render(GhastProjectileEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferSource, int packedLight) {
        stack.pushPose();
        stack.scale(0.8f, 0.8f, 0.8f);
        super.render(entity, entityYaw, partialTicks, stack, bufferSource, packedLight);
        stack.popPose();
    }
}
