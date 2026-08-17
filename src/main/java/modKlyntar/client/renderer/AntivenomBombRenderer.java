package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import modKlyntar.entity.client.model.AntivenomBombModel;
import modKlyntar.entity.custom.AntivenomBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.slf4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AntivenomBombRenderer extends GeoEntityRenderer<AntivenomBombEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int disegnate;
    public AntivenomBombRenderer(EntityRendererProvider.Context context) {
        super(context, new AntivenomBombModel());
    }

    @Override
    public void render(AntivenomBombEntity entity, float entityYaw, float partialTicks,
                       PoseStack stack, MultiBufferSource bufferSource, int packedLight) {
        if (disegnate++ % 20 == 0) {
            LOGGER.info("Bomba disegnata (chiamata {}) a {} {} {}", disegnate,
                    String.format("%.1f", entity.getX()), String.format("%.1f", entity.getY()),
                    String.format("%.1f", entity.getZ()));
        }
        stack.pushPose();
        // il modello del pack e' pensato in grande: rimpicciolito sta bene in volo
        stack.scale(0.6F, 0.6F, 0.6F);
        super.render(entity, entityYaw, partialTicks, stack, bufferSource, packedLight);
        stack.popPose();
    }
}
