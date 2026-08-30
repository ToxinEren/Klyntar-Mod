package modKlyntar.locomotion;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomLocomotionHandler {
    /** Letto anche dal berserk, che accende la locomozione per scavalcare un ostacolo. */
    public static final String OBJECTIVE_NAME = "Venom.Locomotion";
    private static final double GRAB_RANGE = 30.0D;
    private static final int MIN_ARMS_FOR_LOCOMOTION = 4;
    private static final int MAX_ARMS = 6;
    private static final double STATIONARY_ANCHOR_DISTANCE_SQR = 0.04D;
    private static final Map<UUID, AnchorMemory> ANCHOR_MEMORY = new ConcurrentHashMap<>();

    private VenomLocomotionHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        int ticks = getLocomotionTicks(player);
        if (ticks <= 0) {
            ANCHOR_MEMORY.remove(player.getUUID());
            ModNetwork.syncVenomLocomotion(player, Collections.emptyList(), false);
            return;
        }
        tickLocomotion(player);
    }

    private static void tickLocomotion(ServerPlayer player) {
        List<Vec3> anchors = getStableGrabSurfaces(player);
        ModNetwork.syncVenomLocomotion(player, anchors);
        if (anchors.size() < MIN_ARMS_FOR_LOCOMOTION) {
            player.fallDistance = 0.0F;
            return;
        }

        player.fallDistance = 0.0F;
    }


    /** Quanto ci si e' spostati sul piano, ignorando la quota. */
    private static double distanzaOrizzontaleSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static List<Vec3> getStableGrabSurfaces(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Vec3 currentPosition = player.position();
        AnchorMemory memory = ANCHOR_MEMORY.get(playerId);
        // il confronto guarda solo il piano orizzontale: la locomozione spinge in alto a
        // ogni tick, quindi la quota cambia sempre e con la distanza piena la memoria non
        // reggerebbe mai. Restando fermi e muovendo solo la telecamera gli appigli restano
        // quelli, invece di inseguire lo sguardo
        if (memory != null && distanzaOrizzontaleSqr(currentPosition, memory.playerPosition())
                < STATIONARY_ANCHOR_DISTANCE_SQR) {
            return memory.anchors();
        }

        List<Vec3> anchors = findGrabSurfaces(player, GRAB_RANGE);
        if (!anchors.isEmpty()) {
            ANCHOR_MEMORY.put(playerId, new AnchorMemory(List.copyOf(anchors), currentPosition));
        } else {
            ANCHOR_MEMORY.remove(playerId);
        }
        return anchors;
    }
    private static List<Vec3> findGrabSurfaces(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        Vec3 horizontalLook = safeNormalize(new Vec3(look.x, 0.0D, look.z), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 strafe = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);

        Vec3[] directions = new Vec3[] {
                safeNormalize(new Vec3(look.x, look.y + 0.3D, look.z), horizontalLook),
                safeNormalize(new Vec3(horizontalLook.x, 0.55D, horizontalLook.z), horizontalLook),
                safeNormalize(new Vec3(horizontalLook.x, -0.35D, horizontalLook.z), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(0.9D)).add(0.0D, 0.15D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-0.9D)).add(0.0D, 0.15D, 0.0D), horizontalLook),
                safeNormalize(new Vec3(velocity.x * 1.5D + horizontalLook.x, velocity.y + 0.2D, velocity.z * 1.5D + horizontalLook.z), horizontalLook),
                safeNormalize(strafe.add(0.0D, 0.25D, 0.0D), strafe),
                safeNormalize(strafe.scale(-1.0D).add(0.0D, 0.25D, 0.0D), strafe.scale(-1.0D)),
                safeNormalize(horizontalLook.add(strafe.scale(1.35D)).add(0.0D, 0.65D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-1.35D)).add(0.0D, 0.65D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(1.35D)).add(0.0D, -0.45D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-1.35D)).add(0.0D, -0.45D, 0.0D), horizontalLook)
        };

        List<Vec3> anchors = new ArrayList<>();
        for (Vec3 direction : directions) {
            if (anchors.size() >= MAX_ARMS) {
                break;
            }
            Vec3 anchor = tryRaycast(player, direction, range);
            if (anchor != null && !isTooCloseToExistingAnchor(anchors, anchor)) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

    private static Vec3 tryRaycast(ServerPlayer player, Vec3 direction, double range) {
        Vec3 start = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 end = start.add(direction.normalize().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }

        BlockState state = player.level().getBlockState(hit.getBlockPos());
        if (state.isAir() || state.getCollisionShape(player.level(), hit.getBlockPos()).isEmpty()) {
            return null;
        }
        return hit.getLocation();
    }

    private static boolean isTooCloseToExistingAnchor(List<Vec3> anchors, Vec3 candidate) {
        for (Vec3 anchor : anchors) {
            if (anchor.distanceToSqr(candidate) < 2.25D) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-4D) {
            return fallback;
        }
        return vector.normalize();
    }

    private static int getLocomotionTicks(ServerPlayer player) {
        Objective objective = getObjective(player, false);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static Objective getObjective(Player player, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null && create) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal(OBJECTIVE_NAME), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
    private record AnchorMemory(List<Vec3> anchors, Vec3 playerPosition) {
    }
}
