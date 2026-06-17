package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import modKlyntar.MyMod;
import modKlyntar.entity.custom.SmokeTrailEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

public class SmokeTrailRenderer extends EntityRenderer<SmokeTrailEntity> {

    public SmokeTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SmokeTrailEntity entity) {
        return new ResourceLocation(MyMod.MOD_ID, "textures/particle/smoke.png"); // Percorso alla texture del fumo
    }

    @Override
    public void render(SmokeTrailEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLightIn) {
        matrixStack.pushPose();

        // Ottieni la posizione della telecamera
        Minecraft minecraft = Minecraft.getInstance();
        float cameraYaw = minecraft.player.yRotO + (minecraft.player.getYRot() - minecraft.player.yRotO) * partialTicks;
        float cameraPitch = minecraft.player.xRotO + (minecraft.player.getXRot() - minecraft.player.xRotO) * partialTicks;

        // Applica la rotazione inversa della telecamera
        matrixStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        matrixStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

        VertexConsumer buffer = bufferIn.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        PoseStack.Pose lastMatrix = matrixStack.last();
        Matrix4f matrix4f = lastMatrix.pose();
        Matrix3f matrix3f = lastMatrix.normal();

        float minU = 0.0f;
        float maxU = 1.0f;
        float minV = 0.0f;
        float maxV = 1.0f;

        float size = 2.0f; // Dimensione del fumo, modificata per essere due volte più grande

        buffer.vertex(matrix4f, -size, -size, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0f, 1.0f, 0.0f).endVertex();
        buffer.vertex(matrix4f, size, -size, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0f, 1.0f, 0.0f).endVertex();
        buffer.vertex(matrix4f, size, size, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0f, 1.0f, 0.0f).endVertex();
        buffer.vertex(matrix4f, -size, size, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0f, 1.0f, 0.0f).endVertex();

        matrixStack.popPose();
    }
}
