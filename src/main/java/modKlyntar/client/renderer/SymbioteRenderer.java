package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.entity.client.model.Symbiote;
import modKlyntar.entity.custom.SymbioteEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SymbioteRenderer extends GeoEntityRenderer<SymbioteEntity> {
    public SymbioteRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Symbiote());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(SymbioteEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferSource, int packedLight) {
        stack.pushPose();
        stack.scale(0.8f, 0.8f, 0.8f);
        super.render(entity, entityYaw, partialTicks, stack, bufferSource, packedLight);
        stack.popPose();
    }
}
