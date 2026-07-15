package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.client.renderer.VenomLocomotionRenderer;
import modKlyntar.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomLocomotionClientController {
    private static final String FLIGHT_OBJECTIVE = "Venom.Flight";
    private static final String VENOM_SIZE_OBJECTIVE = "Klyntar.VenomSize";
    private static final double UPWARD_FORCE = 0.025D;
    private static final double VENOM_UPWARD_FORCE = 0.038D;
    private static final double MOVEMENT_SPEED = 0.05D;
    private static final double JUMP_FORCE = 0.1D;
    private static final double GROUND_CLEARANCE_THRESHOLD = 10.0D;
    private static final double VENOM_GROUND_CLEARANCE_THRESHOLD = 13.0D;
    private static final int MIN_ARMS_FOR_LOCOMOTION = 4;
    private static final int FLIGHT_LAUNCH_MAX_TICKS = 28;
    private static final double FLIGHT_LAUNCH_HEIGHT = 5.0D;
    private static final double FLIGHT_LAUNCH_SPEED = 0.72D;
    private static final double FLIGHT_TRANSITION_SPEED = 0.22D;
    private static final double FLIGHT_FIREWORK_TARGET_SPEED = 3.4D;
    private static final double FLIGHT_FIREWORK_SPRINT_TARGET_SPEED = 5.0D;
    private static final double FLIGHT_FIREWORK_DIRECT_THRUST = 0.45D;
    private static final double FLIGHT_FIREWORK_TARGET_PULL = 1.0D;
    private static final int FLIGHT_PROPELLER_TICKS = 35;
    private static final int FLIGHT_PROPELLER_RETRIGGER_TICKS = 8;
    private static long lastPacketGameTime;
    private static FlightState flightState;
    private static boolean serverFlightActive;

    private VenomLocomotionClientController() {
    }

    public static void setFlightActiveFromServer(boolean active) {
        serverFlightActive = active;
        if (!active) {
            flightState = null;
        }
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

        if (tickFlight(minecraft, player)) {
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

    private static boolean tickFlight(Minecraft minecraft, LocalPlayer player) {
        if (!serverFlightActive && !isScoreActive(player, FLIGHT_OBJECTIVE)) {
            if (flightState != null) {
                setFallFlyingFlag(player, false);
                flightState = null;
            }
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime == lastPacketGameTime) {
            return true;
        }
        lastPacketGameTime = gameTime;

        player.fallDistance = 0.0F;
        Vec3 look = player.getLookAngle().normalize();
        Vec3 current = player.getDeltaMovement();
        if (flightState == null) {
            flightState = new FlightState(player.getY());
            player.displayClientMessage(Component.literal("Venom Flight client active"), true);
        }

        if (flightState.launching) {
            setFallFlyingFlag(player, false);
            flightState.launchTicks++;
            double gainedHeight = player.getY() - flightState.launchStartY;
            if (gainedHeight >= FLIGHT_LAUNCH_HEIGHT || flightState.launchTicks >= FLIGHT_LAUNCH_MAX_TICKS) {
                flightState.launching = false;
            } else {
                double forwardBoost = flightState.launchTicks > 8 ? FLIGHT_TRANSITION_SPEED : FLIGHT_TRANSITION_SPEED * 0.35D;
                Vec3 next = new Vec3(
                        current.x * 0.35D + look.x * forwardBoost,
                        Math.max(current.y, FLIGHT_LAUNCH_SPEED),
                        current.z * 0.35D + look.z * forwardBoost
                );
                player.setDeltaMovement(next);
                ModNetwork.syncVenomLocomotionVelocity(next);
                return true;
            }
        }

        setFallFlyingFlag(player, true);
        boolean forwardPressed = minecraft.options.keyUp.isDown();
        if (forwardPressed) {
            flightState.propellerTicks = 2;
            if (!flightState.forwardPressedLastTick) {
                player.displayClientMessage(Component.literal("Venom propeller"), true);
            }
        }
        flightState.forwardPressedLastTick = forwardPressed;

        Vec3 next = player.getDeltaMovement();
        if (flightState.propellerTicks > 0) {
            double targetSpeed = minecraft.options.keySprint.isDown()
                    ? FLIGHT_FIREWORK_SPRINT_TARGET_SPEED
                    : FLIGHT_FIREWORK_TARGET_SPEED;
            next = applyPermanentFireworkBoost(next, look, targetSpeed);
            flightState.propellerTicks--;
        }
        if (minecraft.options.keyShift.isDown()) {
            next = next.add(0.0D, -0.12D, 0.0D);
        }
        player.setDeltaMovement(next);
        ModNetwork.syncVenomLocomotionVelocity(next);
        return true;
    }

    private static Vec3 applyPermanentFireworkBoost(Vec3 velocity, Vec3 look, double targetSpeed) {
        return velocity.add(
                look.x * FLIGHT_FIREWORK_DIRECT_THRUST + (look.x * targetSpeed - velocity.x) * FLIGHT_FIREWORK_TARGET_PULL,
                look.y * FLIGHT_FIREWORK_DIRECT_THRUST + (look.y * targetSpeed - velocity.y) * FLIGHT_FIREWORK_TARGET_PULL,
                look.z * FLIGHT_FIREWORK_DIRECT_THRUST + (look.z * targetSpeed - velocity.z) * FLIGHT_FIREWORK_TARGET_PULL
        );
    }

    private static boolean isScoreActive(LocalPlayer player, String objectiveName) {
        var objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return false;
        }
        return player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore() > 0;
    }

    private static void setFallFlyingFlag(Entity player, boolean value) {
        try {
            Method method = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
            method.setAccessible(true);
            method.invoke(player, 7, value);
        } catch (ReflectiveOperationException ignored) {
        }
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
        double clearanceThreshold = isScoreActive(player, VENOM_SIZE_OBJECTIVE)
                ? VENOM_GROUND_CLEARANCE_THRESHOLD
                : GROUND_CLEARANCE_THRESHOLD;
        double upwardForce = isScoreActive(player, VENOM_SIZE_OBJECTIVE) ? VENOM_UPWARD_FORCE : UPWARD_FORCE;
        double distanceToGround = getDistanceToGround(player, clearanceThreshold);
        if (distanceToGround < clearanceThreshold) {
            double closeness = 1.0D - distanceToGround / clearanceThreshold;
            force = upwardForce * closeness * closeness * clearanceThreshold;
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

    private static double getDistanceToGround(Entity entity, double maxDistance) {
        Level level = entity.level();
        BlockPos origin = entity.blockPosition();
        double closestDistance = maxDistance;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y < maxDistance; y++) {
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

    private static final class FlightState {
        private final double launchStartY;
        private int launchTicks;
        private int propellerTicks;
        private boolean forwardPressedLastTick;
        private boolean launching = true;

        private FlightState(double launchStartY) {
            this.launchStartY = launchStartY;
        }
    }
}
