package modKlyntar.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;

import modKlyntar.MyMod;
import modKlyntar.item.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CapsuleItemRenderer extends GeoItemRenderer<CapsuleItem> {
    public CapsuleItemRenderer() {
        super(new CapsuleItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // nell'inventario si usa la sprite piatta di sempre, il modello 3D solo nel mondo
        if (displayContext == ItemDisplayContext.GUI && stack.getItem() instanceof CapsuleItem capsule) {
            Minecraft mc = Minecraft.getInstance();
            BakedModel flat = mc.getModelManager()
                    .getModel(new ModelResourceLocation(MyMod.MOD_ID, capsule.getGuiModelName(), "inventory"));
            if (flat != null && flat != mc.getModelManager().getMissingModel()) {
                poseStack.pushPose();
                // ItemRenderer.render ha gia' centrato con translate(-0.5), e lo rifara' sul modello
                // piatto: senza questo l'icona finisce fuori asse
                poseStack.translate(0.5D, 0.5D, 0.5D);
                mc.getItemRenderer().render(stack, displayContext, false, poseStack,
                        bufferSource, packedLight, packedOverlay, flat);
                poseStack.popPose();
                return;
            }
        }
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
