package modKlyntar.client.renderer;

import modKlyntar.entity.client.model.VenomPlayerModel;
import modKlyntar.entity.custom.VenomPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VenomPlayerRenderer extends GeoEntityRenderer<VenomPlayerEntity> {
    public VenomPlayerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VenomPlayerModel());
        this.shadowRadius = 0.3f;
    }
}
