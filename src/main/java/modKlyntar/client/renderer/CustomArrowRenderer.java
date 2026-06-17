package modKlyntar.client.renderer;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.CustomArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CustomArrowRenderer extends ArrowRenderer<CustomArrowEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/entity/custom_arrow.png");

    public CustomArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CustomArrowEntity entity) {
        return TEXTURE;
    }

}
