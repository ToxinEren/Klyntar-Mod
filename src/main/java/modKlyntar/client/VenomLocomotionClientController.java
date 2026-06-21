package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.client.renderer.VenomLocomotionRenderer;
import modKlyntar.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomLocomotionClientController {
    private static final double UPWARD_FORCE = 0.025D;
    private static final double MOVEMENT_SPEED = 0.05D;
    private static final double JUMP_FORCE = 0.1D;
    private static final int MIN_ARMS_FOR_LOCOMOTION = 1;
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
        Vec3 next = new Vec3(
                current.x + movementForce.x * multiplier,
                current.y + verticalForce * multiplier,
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
        double verticalBase = 0.065D;
        double currentY = player.getDeltaMovement().y;
        double force;

        if (shift) {
            force = -0.01D;
        } else if (jump) {
            force = verticalBase;
        } else {
            force = verticalBase - currentY * 0.2D;
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
}