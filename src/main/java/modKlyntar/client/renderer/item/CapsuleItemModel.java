package modKlyntar.client.renderer.item;

import modKlyntar.item.CapsuleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CapsuleItemModel extends GeoModel<CapsuleItem> {

    @Override
    public ResourceLocation getModelResource(CapsuleItem animatable) {
        return animatable.getModelResource();
    }

    @Override
    public ResourceLocation getTextureResource(CapsuleItem animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(CapsuleItem animatable) {
        return animatable.getAnimationResource();
    }
}
