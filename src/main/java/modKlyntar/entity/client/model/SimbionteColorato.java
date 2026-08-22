package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SimbionteColoratoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Stesso corpo e stesse animazioni del simbionte comune; la pelle la sceglie l'entita'. */
public class SimbionteColorato<T extends SimbionteColoratoEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "animations/symbiote.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "geo/symbiote.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.texture();
    }
}
