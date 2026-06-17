package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.GhastProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GhastProjectileModel extends GeoModel<GhastProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(GhastProjectileEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "geo/meteor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhastProjectileEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/meteor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhastProjectileEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "animations/meteor.animation.json");
    }
}
