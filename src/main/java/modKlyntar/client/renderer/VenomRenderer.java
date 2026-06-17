package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.entity.client.model.VenomModel;
import modKlyntar.entity.custom.VenomEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VenomRenderer extends GeoEntityRenderer<VenomEntity> {
    public VenomRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VenomModel());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(VenomEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }
}
