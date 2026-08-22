package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.GrendelsFragmentEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Stesso corpo del simbionte comune, con la linea di Knull sulla schiena. */
public class GrendelsFragment extends GeoModel<GrendelsFragmentEntity> {
    @Override
    public ResourceLocation getAnimationResource(GrendelsFragmentEntity animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "animations/symbiote.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(GrendelsFragmentEntity animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "geo/symbiote.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GrendelsFragmentEntity animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/grendel/grendels_fragment.png");
    }
}
