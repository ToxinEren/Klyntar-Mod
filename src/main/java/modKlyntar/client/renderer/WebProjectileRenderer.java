package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import modKlyntar.MyMod;
import modKlyntar.entity.custom.WebProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WebProjectileRenderer extends EntityRenderer<WebProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/entity/web_projectile.png");

    public WebProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WebProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        poseStack.pushPose();
        Matrix4f matrix4f = poseStack.last().pose();
        float r = 1.0f, g = 0.0f, b = 0.0f; // Colore rosso brillante per debug

        // Punto di partenza (posizione del giocatore)
        double startX = entity.getX();
        double startY = entity.getY();
        double startZ = entity.getZ();

        // Punto di arrivo (posizione del target)
        double endX = entity.getTargetX();
        double endY = entity.getTargetY();
        double endZ = entity.getTargetZ();

        // Disegna il rettangolo
        float width = 0.1f; // Larghezza del rettangolo
        float height = (float) Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY) + (endZ - startZ) * (endZ - startZ));

        // Vertici del rettangolo
        vertexConsumer.vertex(matrix4f, (float) startX, (float) startY, (float) startZ).color(r, g, b, 1.0f).endVertex();
        vertexConsumer.vertex(matrix4f, (float) (startX + width), (float) startY, (float) startZ).color(r, g, b, 1.0f).endVertex();
        vertexConsumer.vertex(matrix4f, (float) endX, (float) endY, (float) endZ).color(r, g, b, 1.0f).endVertex();
        vertexConsumer.vertex(matrix4f, (float) (endX + width), (float) endY, (float) endZ).color(r, g, b, 1.0f).endVertex();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WebProjectileEntity entity) {
        return TEXTURE;
    }
}
