package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.entity.client.model.GrendelsFragment;
import modKlyntar.entity.custom.GrendelsFragmentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GrendelsFragmentRenderer extends GeoEntityRenderer<GrendelsFragmentEntity> {
    public GrendelsFragmentRenderer(EntityRendererProvider.Context contesto) {
        super(contesto, new GrendelsFragment());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(GrendelsFragmentEntity entita, float yaw, float partialTicks,
                       PoseStack stack, MultiBufferSource buffer, int luce) {
        stack.pushPose();
        stack.scale(0.8f, 0.8f, 0.8f);
        super.render(entita, yaw, partialTicks, stack, buffer, luce);
        stack.popPose();
    }
}
