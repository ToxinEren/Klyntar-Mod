package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomLocomotionRenderer {
    private static final ResourceLocation ARM_TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/models/locomotion/venom_tentacle_segment.png");
    private static final int ARM_SEGMENTS = 18;
    private static final int VISIBLE_ARMS = 6;
    private static final int ARM_COLOR = 18;
    private static final float TENTACLE_THICKNESS_SCALE = 1.5F;
    private static final Map<Integer, AnchorState> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<Integer, GrabTargetState> GRAB_TARGETS = new ConcurrentHashMap<>();

    private VenomLocomotionRenderer() {
    }

    public static void updateAnchors(int entityId, List<Vec3> anchors) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (anchors == null || anchors.isEmpty()) {
            ANCHORS.remove(entityId);
            return;
        }
        ANCHORS.put(entityId, new AnchorState(List.copyOf(anchors), gameTime));
    }
    public static void updateGrabTarget(int entityId, Vec3 target) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (target == null) {
            GRAB_TARGETS.remove(entityId);
            return;
        }
        GRAB_TARGETS.put(entityId, new GrabTargetState(target, gameTime));
    }

    public static List<Vec3> getAnchors(int entityId) {
        AnchorState state = ANCHORS.get(entityId);
        if (state == null) {
            return Collections.emptyList();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.getGameTime() - state.gameTime > 8L) {
            return Collections.emptyList();
        }
        return state.anchors;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || (ANCHORS.isEmpty() && GRAB_TARGETS.isEmpty())) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        long gameTime = minecraft.level.getGameTime();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        List<Integer> staleEntries = new ArrayList<>();
        for (Map.Entry<Integer, AnchorState> entry : ANCHORS.entrySet()) {
            AnchorState state = entry.getValue();
            if (gameTime - state.gameTime > 8L) {
                staleEntries.add(entry.getKey());
                continue;
            }
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof Player player) {
                renderPlayerArms(player, state.anchors, event.getPartialTick(), poseStack, buffer, gameTime);
            }
        }
        List<Integer> staleGrabTargets = new ArrayList<>();
        for (Map.Entry<Integer, GrabTargetState> entry : GRAB_TARGETS.entrySet()) {
            GrabTargetState state = entry.getValue();
            if (gameTime - state.gameTime > 8L) {
                staleGrabTargets.add(entry.getKey());
                continue;
            }
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof Player player) {
                renderPlayerGrabTentacle(player, state.target, event.getPartialTick(), poseStack, buffer, gameTime);
            }
        }
        poseStack.popPose();
        staleEntries.forEach(ANCHORS::remove);
        staleGrabTargets.forEach(GRAB_TARGETS::remove);
        buffer.endBatch(RenderType.entityCutoutNoCull(ARM_TEXTURE));
    }

    private static void renderPlayerArms(Player player, List<Vec3> anchors, float partialTick, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        if (anchors.isEmpty()) {
            return;
        }

        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        Vec3 back = horizontalLook.scale(-0.46D);
        double[] sideOffsets = new double[] { -0.18D, 0.18D, -0.11D, 0.11D, -0.04D, 0.04D };
        double[] heightOffsets = new double[] { 1.12D, 1.12D, 1.42D, 1.42D, 1.72D, 1.72D };

        for (int i = 0; i < VISIBLE_ARMS; i++) {
            Vec3 start = playerPos.add(right.scale(sideOffsets[i])).add(back).add(0.0D, heightOffsets[i], 0.0D);
            Vec3 root = start.add(horizontalLook.scale(0.22D));
            Vec3 end = resolveTentacleEnd(playerPos, horizontalLook, right, anchors, i, gameTime);
            renderArmPath(root, end, i, poseStack, buffer, gameTime);
        }
    }

    private static void renderPlayerGrabTentacle(Player player, Vec3 target, float partialTick, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 back = horizontalLook.scale(-0.36D);
        Vec3 start = playerPos.add(back).add(0.0D, 1.45D, 0.0D).add(horizontalLook.scale(0.16D));
        renderArmPath(start, target, 7, poseStack, buffer, gameTime);
    }
    private static Vec3 resolveTentacleEnd(Vec3 playerPos, Vec3 horizontalLook, Vec3 right, List<Vec3> anchors, int index, long gameTime) {
        if (index < anchors.size()) {
            return anchors.get(index);
        }
        if (anchors.size() == 1 && index == 1) {
            return anchors.get(0).add(right.scale(0.35D)).add(0.0D, 0.08D, 0.0D);
        }

        double side = index % 2 == 0 ? -1.0D : 1.0D;
        double tier = index < 2 ? 0.0D : index < 4 ? 0.35D : 0.7D;
        double sway = Math.sin(gameTime * 0.12D + index) * 0.18D;
        return playerPos
                .add(horizontalLook.scale(-0.85D - tier * 0.55D))
                .add(right.scale(side * (0.75D + tier * 0.35D + sway)))
                .add(0.0D, 2.75D + tier * 0.18D, 0.0D);
    }

    private static void renderArmPath(Vec3 start, Vec3 end, int armIndex, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ARM_TEXTURE));
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 sideWave = safeNormalize(delta.cross(new Vec3(0.0D, 1.0D, 0.0D)), new Vec3(1.0D, 0.0D, 0.0D)).scale(0.06D);
        Vec3 previous = start;
        for (int segment = 1; segment <= ARM_SEGMENTS; segment++) {
            double progress = segment / (double) ARM_SEGMENTS;
            double previousProgress = (segment - 1) / (double) ARM_SEGMENTS;
            double pulse = Math.sin((gameTime * 0.35D) + progress * Math.PI * 2.0D + armIndex * 0.8D);
            double previousPulse = Math.sin((gameTime * 0.35D) + previousProgress * Math.PI * 2.0D + armIndex * 0.8D);
            double wave = pulse * 0.28D * (1.0D - Math.abs(progress - 0.5D));
            double previousWave = previousPulse * 0.28D * (1.0D - Math.abs(previousProgress - 0.5D));
            Vec3 current = start.add(delta.scale(progress)).add(sideWave.scale(wave));
            previous = start.add(delta.scale(previousProgress)).add(sideWave.scale(previousWave));
            float radius = (segment == ARM_SEGMENTS ? 0.11F : 0.085F) * TENTACLE_THICKNESS_SCALE;
            addOrientedSegment(vertexConsumer, poseStack, previous, current, radius, LightTexture.FULL_BRIGHT);
        }
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-4D) {
            return fallback;
        }
        return vector.normalize();
    }

    private static void addOrientedSegment(VertexConsumer consumer, PoseStack poseStack, Vec3 start, Vec3 end, float radius, int light) {
        Vec3 forward = end.subtract(start);
        if (forward.lengthSqr() < 1.0E-5D) {
            return;
        }
        forward = forward.normalize();
        Vec3 upHint = Math.abs(forward.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(upHint).normalize().scale(radius);
        Vec3 up = right.cross(forward).normalize().scale(radius);

        Vec3 a = start.add(right).add(up);
        Vec3 b = start.add(right).subtract(up);
        Vec3 c = start.subtract(right).subtract(up);
        Vec3 d = start.subtract(right).add(up);
        Vec3 e = end.add(right).add(up);
        Vec3 f = end.add(right).subtract(up);
        Vec3 g = end.subtract(right).subtract(up);
        Vec3 h = end.subtract(right).add(up);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        addQuad(consumer, matrix, normal, a, e, f, b, right.normalize(), light);
        addQuad(consumer, matrix, normal, d, c, g, h, right.normalize().scale(-1.0D), light);
        addQuad(consumer, matrix, normal, a, d, h, e, up.normalize(), light);
        addQuad(consumer, matrix, normal, b, f, g, c, up.normalize().scale(-1.0D), light);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normalVector, int light) {
        float nx = (float) normalVector.x;
        float ny = (float) normalVector.y;
        float nz = (float) normalVector.z;
        consumer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(ARM_COLOR, ARM_COLOR, ARM_COLOR, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(ARM_COLOR, ARM_COLOR, ARM_COLOR, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(ARM_COLOR, ARM_COLOR, ARM_COLOR, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(ARM_COLOR, ARM_COLOR, ARM_COLOR, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    private record AnchorState(List<Vec3> anchors, long gameTime) {
    }

    private record GrabTargetState(Vec3 target, long gameTime) {
    }
}
