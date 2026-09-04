package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import modKlyntar.MyMod;
import modKlyntar.block.entity.DioMortoBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Disegna il colosso d'oro steso, lungo sette blocchi, a partire dal suo blocco d'ancoraggio. */
public class DioMortoRenderer extends GeoBlockRenderer<DioMortoBlockEntity> {

    /**
     * Il modello parte dal centro del blocco d'ancoraggio e si estende in avanti lungo Z.
     *
     * <p>Le sue coordinate vanno da 0 a 112 unita' su Z, cioe' sette blocchi, ma partendo dal
     * centro dell'ancora finirebbe mezzo blocco piu' in la' del sedime: questo mezzo blocco
     * di arretramento lo rimette in squadra.</p>
     */
    private static final float ARRETRAMENTO_Z = -0.5F;

    public DioMortoRenderer(BlockEntityRendererProvider.Context contesto) {
        super(new Modello());
    }

    @Override
    public void preRender(PoseStack pila, DioMortoBlockEntity dio, BakedGeoModel modello,
                          MultiBufferSource buffer, VertexConsumer consumatore, boolean riDisegno,
                          float parziale, int luce, int sovrapposizione,
                          float r, float g, float b, float a) {
        pila.translate(0.0F, 0.0F, ARRETRAMENTO_Z);
        super.preRender(pila, dio, modello, buffer, consumatore, riDisegno, parziale, luce,
                sovrapposizione, r, g, b, a);
    }

    private static final class Modello extends GeoModel<DioMortoBlockEntity> {
        @Override
        public ResourceLocation getModelResource(DioMortoBlockEntity dio) {
            return new ResourceLocation(MyMod.MOD_ID, "geo/dead_god.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(DioMortoBlockEntity dio) {
            return new ResourceLocation(MyMod.MOD_ID, "textures/block/dead_god.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DioMortoBlockEntity dio) {
            return null;
        }
    }
}
