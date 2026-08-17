package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.AntivenomBombEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AntivenomBombModel extends GeoModel<AntivenomBombEntity> {
    @Override
    public ResourceLocation getModelResource(AntivenomBombEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "geo/antivenom_bomb.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AntivenomBombEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/antivenom_bomb.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AntivenomBombEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "animations/antivenom_bomb.animation.json");
    }
}
