package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import modKlyntar.MyMod;
import modKlyntar.block.entity.AlienoFusoBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Disegna il cadavere dell'alieno con la corazza e la Necrospada sulla schiena. */
public class AlienoFusoRenderer extends GeoBlockRenderer<AlienoFusoBlockEntity> {

    public AlienoFusoRenderer(BlockEntityRendererProvider.Context contesto) {
        super(new Modello());
    }

    /**
     * I tentacoli si disegnano qui, non sovrascrivendo {@code render}.
     *
     * <p>Quel metodo GeckoLib lo dichiara con il proprio parametro di tipo, la cui cancellazione
     * e' {@code BlockEntity}: qualunque firma si provi a scrivere nella sottoclasse finisce per
     * collidere senza sovrascrivere. {@code postRender} invece e' un aggancio suo, tipizzato,
     * chiamato subito dopo il corpo e con la pila ancora nello spazio locale del blocco.</p>
     */
    @Override
    public void postRender(PoseStack pila, AlienoFusoBlockEntity alieno, BakedGeoModel modello,
                           MultiBufferSource buffer, VertexConsumer consumatore, boolean riDisegno,
                           float parziale, int luce, int sovrapposizione,
                           float r, float g, float b, float a) {
        super.postRender(pila, alieno, modello, buffer, consumatore, riDisegno, parziale, luce,
                sovrapposizione, r, g, b, a);
        if (!riDisegno && alieno.getLevel() != null) {
            TentacoliCadavere.disegna(alieno.getLevel(), alieno.getBlockPos(), parziale,
                    pila, buffer);
        }
    }

    private static final class Modello extends GeoModel<AlienoFusoBlockEntity> {
        @Override
        public ResourceLocation getModelResource(AlienoFusoBlockEntity alieno) {
            return new ResourceLocation(MyMod.MOD_ID, "geo/fused_alien.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(AlienoFusoBlockEntity alieno) {
            return new ResourceLocation(MyMod.MOD_ID, "textures/block/fused_alien.png");
        }

        @Override
        public ResourceLocation getAnimationResource(AlienoFusoBlockEntity alieno) {
            return null;
        }
    }
}
