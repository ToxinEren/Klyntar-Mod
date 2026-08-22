package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.entity.client.model.SimbionteColorato;
import modKlyntar.entity.custom.SimbionteColoratoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Un solo renderer per tutte le varianti: cambia la texture, non il modello. */
public class SimbionteColoratoRenderer<T extends SimbionteColoratoEntity> extends GeoEntityRenderer<T> {
    public SimbionteColoratoRenderer(EntityRendererProvider.Context contesto) {
        super(contesto, new SimbionteColorato<>());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(T entita, float yaw, float partialTicks,
                       PoseStack stack, MultiBufferSource buffer, int luce) {
        stack.pushPose();
        stack.scale(0.8f, 0.8f, 0.8f);
        super.render(entita, yaw, partialTicks, stack, buffer, luce);
        stack.popPose();
    }
}
