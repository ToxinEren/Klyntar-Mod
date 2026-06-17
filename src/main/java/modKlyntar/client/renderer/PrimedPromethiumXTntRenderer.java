package modKlyntar.client.renderer;

import modKlyntar.entity.client.model.PrimedPromethiumXTntModel;
import modKlyntar.entity.custom.PrimedPromethiumXTnt;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PrimedPromethiumXTntRenderer extends GeoEntityRenderer<PrimedPromethiumXTnt> {
    public PrimedPromethiumXTntRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PrimedPromethiumXTntModel());
    }
}
