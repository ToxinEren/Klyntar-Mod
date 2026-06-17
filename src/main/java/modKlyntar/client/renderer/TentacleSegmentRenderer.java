package modKlyntar.client.renderer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import modKlyntar.MyMod;
import modKlyntar.client.ClientSetup;
import modKlyntar.entity.client.model.TentacleSegmentModel;
import modKlyntar.entity.custom.TentacleSegmentEntity;

public class TentacleSegmentRenderer extends MobRenderer<TentacleSegmentEntity, TentacleSegmentModel<TentacleSegmentEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/entity/tentacle_segment.png");

    public TentacleSegmentRenderer(EntityRendererProvider.Context context) {
        super(context, new TentacleSegmentModel<>(context.bakeLayer(ClientSetup.TENTACLE_SEGMENT_LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TentacleSegmentEntity entity) {
        return TEXTURE;
    }
}
