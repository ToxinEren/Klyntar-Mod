package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.client.renderer.VenomLocomotionRenderer;
import modKlyntar.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomLocomotionClientController {
    private static final double UPWARD_FORCE = 0.025D;
    private static final double MOVEMENT_SPEED = 0.05D;
    private static final double JUMP_FORCE = 0.1D;
    private static final double GROUND_CLEARANCE_THRESHOLD = 10.0D;
    private static final int MIN_ARMS_FOR_LOCOMOTION = 4;
    private static long lastPacketGameTime;

    private VenomLocomotionClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.isPaused()) {
            return;
        }

        if (VenomLocomotionRenderer.getAnchors(player.getId()).size() < MIN_ARMS_FOR_LOCOMOTION) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime == lastPacketGameTime) {
            return;
        }
        lastPacketGameTime = gameTime;

        Vec3 movementForce = calculateMovementForce(
                player.getYRot(),
                minecraft.options.keyUp.isDown(),
                minecraft.options.keyDown.isDown(),
                minecraft.options.keyLeft.isDown(),
                minecraft.options.keyRight.isDown()
        );

        boolean jump = minecraft.options.keyJump.isDown();
        boolean shift = minecraft.options.keyShift.isDown();
        double verticalForce = calculateVerticalForce(player, jump, shift);
        double multiplier = getMovementMultiplier(player);

        Vec3 current = player.getDeltaMovement();
        double currentY = current.y;
        if (currentY < -0.9D) {
            currentY = -0.9D;
        }
        if (currentY < 0.0D) {
            currentY *= 0.7D;
        }
        Vec3 next = new Vec3(
                current.x + movementForce.x * multiplier,
                currentY * 0.95D + verticalForce * multiplier,
                current.z + movementForce.z * multiplier
        );

        player.setDeltaMovement(next);
        player.fallDistance = 0.0F;
        ModNetwork.syncVenomLocomotionVelocity(next);
    }

    private static Vec3 calculateMovementForce(float yaw, boolean forwardPressed, boolean backPressed, boolean leftPressed, boolean rightPressed) {
        if (!forwardPressed && !backPressed && !leftPressed && !rightPressed) {
            return Vec3.ZERO;
        }

        double radians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();
        Vec3 strafe = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 force = Vec3.ZERO;

        if (forwardPressed) {
            force = force.add(forward.scale(MOVEMENT_SPEED));
        }
        if (backPressed) {
            force = force.add(forward.scale(-MOVEMENT_SPEED * 0.5D));
        }
        if (leftPressed) {
            force = force.add(strafe.scale(-MOVEMENT_SPEED * 0.7D));
        }
        if (rightPressed) {
            force = force.add(strafe.scale(MOVEMENT_SPEED * 0.7D));
        }
        return force;
    }

    private static double calculateVerticalForce(LocalPlayer player, boolean jump, boolean shift) {
        double force = 0.0D;
        double distanceToGround = getDistanceToGround(player);
        if (distanceToGround < GROUND_CLEARANCE_THRESHOLD) {
            double closeness = 1.0D - distanceToGround / GROUND_CLEARANCE_THRESHOLD;
            force = UPWARD_FORCE * closeness * closeness * GROUND_CLEARANCE_THRESHOLD;
        }

        if (shift) {
            force = -0.1D;
        }

        if (jump && !shift) {
            force += JUMP_FORCE;
        }
        return force;
    }

    private static double getMovementMultiplier(LocalPlayer player) {
        if (player.isFallFlying()) {
            return 0.7D;
        }
        if (player.isInLava()) {
            return 1.5D;
        }
        if (player.isInWater()) {
            return 1.2D;
        }
        return 1.0D;
    }

    private static double getDistanceToGround(Entity entity) {
        Level level = entity.level();
        BlockPos origin = entity.blockPosition();
        double closestDistance = GROUND_CLEARANCE_THRESHOLD;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y < GROUND_CLEARANCE_THRESHOLD; y++) {
                    BlockPos pos = origin.offset(x, -y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() && state.getFluidState().isEmpty()) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(level, pos);
                    double surfaceY = pos.getY();
                    if (!shape.isEmpty()) {
                        surfaceY += shape.max(Direction.Axis.Y);
                    }

                    closestDistance = Math.min(closestDistance, entity.getY() - surfaceY);
                    break;
                }
            }
        }

        return closestDistance;
    }
}
