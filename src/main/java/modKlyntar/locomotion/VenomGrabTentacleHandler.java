package modKlyntar.locomotion;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomGrabTentacleHandler {
    private static final String OBJECTIVE_NAME = "Venom.GrabTentacle";
    private static final String DISTANCE_DELTA_OBJECTIVE = "Venom.GrabTentacle.DistanceDelta";
    private static final String RELEASE_CHARGE_OBJECTIVE = "Venom.GrabTentacle.ReleaseCharge";
    private static final double GRAB_RANGE = 10.0D;
    private static final int EXTEND_TICKS = 6;
    private static final double HOLD_DISTANCE_STEP = 0.35D;
    private static final double MIN_HOLD_DISTANCE_OFFSET = -1.75D;
    private static final double MAX_HOLD_DISTANCE_OFFSET = 6.0D;
    private static final int THROW_MIN_CHARGE_TICKS = 6;
    private static final int THROW_MAX_CHARGE_TICKS = 40;
    private static final double MIN_THROW_SPEED = 0.8D;
    private static final double MAX_THROW_SPEED_BONUS = 2.0D;
    private static final double MIN_THROW_UP = 0.12D;
    private static final double MAX_THROW_UP_BONUS = 0.55D;
    private static final Map<UUID, GrabState> ACTIVE_GRABS = new ConcurrentHashMap<>();

    private VenomGrabTentacleHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        int active = getScore(player, OBJECTIVE_NAME, false);
        if (active <= 0) {
            clearGrab(player);
            return;
        }

        tickGrab(player);
    }

    private static void tickGrab(ServerPlayer player) {
        UUID playerId = player.getUUID();
        GrabState state = ACTIVE_GRABS.get(playerId);
        Entity target = state == null ? null : player.level().getEntity(state.targetId());
        if (!canGrabEntity(target, player)) {
            target = getLookedAtEntity(player);
            if (target == null) {
                ACTIVE_GRABS.remove(playerId);
                ModNetwork.syncVenomGrabTentacle(player, null);
                return;
            }
            state = new GrabState(target.getId(), 0, 0.0D);
            ACTIVE_GRABS.put(playerId, state);
        }

        int releaseCharge = getScore(player, RELEASE_CHARGE_OBJECTIVE, false);
        if (releaseCharge > 0) {
            releaseGrabbedEntity(player, target, releaseCharge);
            return;
        }

        double holdDistanceOffset = updateHoldDistance(player, state.holdDistanceOffset());
        Vec3 targetCenter = getTargetCenter(target);
        if (state.age() >= EXTEND_TICKS) {
            Vec3 holdCenter = computeHoldPos(player, holdDistanceOffset);
            holdEntityAt(target, holdCenter);
            targetCenter = getTargetCenter(target);
        }

        ACTIVE_GRABS.put(playerId, new GrabState(target.getId(), state.age() + 1, holdDistanceOffset));
        ModNetwork.syncVenomGrabTentacle(player, targetCenter);
        player.fallDistance = 0.0F;
    }

    private static double updateHoldDistance(ServerPlayer player, double currentOffset) {
        int delta = getScore(player, DISTANCE_DELTA_OBJECTIVE, false);
        if (delta == 0) {
            return currentOffset;
        }
        setScore(player, DISTANCE_DELTA_OBJECTIVE, 0);
        return clampHoldDistanceOffset(currentOffset + delta * HOLD_DISTANCE_STEP);
    }

    private static void releaseGrabbedEntity(ServerPlayer player, Entity target, int chargeTicks) {
        boolean shouldThrow = chargeTicks >= THROW_MIN_CHARGE_TICKS;
        if (shouldThrow && target != null && target.isAlive()) {
            double charge = Mth.clamp(chargeTicks / (double) THROW_MAX_CHARGE_TICKS, 0.0D, 1.0D);
            Vec3 look = player.getLookAngle().normalize();
            double horizontalSpeed = MIN_THROW_SPEED + MAX_THROW_SPEED_BONUS * charge;
            double verticalSpeed = MIN_THROW_UP + MAX_THROW_UP_BONUS * charge;
            target.setDeltaMovement(look.x * horizontalSpeed, verticalSpeed, look.z * horizontalSpeed);
            target.hasImpulse = true;
            target.hurtMarked = true;
        } else if (target != null && target.isAlive()) {
            target.setDeltaMovement(Vec3.ZERO);
            target.hasImpulse = true;
        }

        setScore(player, RELEASE_CHARGE_OBJECTIVE, 0);
        setScore(player, OBJECTIVE_NAME, 0);
        clearGrab(player);
    }

    private static Entity getLookedAtEntity(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(GRAB_RANGE));
        AABB searchBox = player.getBoundingBox().expandTowards(player.getLookAngle().scale(GRAB_RANGE)).inflate(1.25D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, searchBox,
                entity -> canGrabEntity(resolveGrabEntityTarget(entity, player), player), GRAB_RANGE * GRAB_RANGE);
        if (hit == null) {
            return null;
        }
        return resolveGrabEntityTarget(hit.getEntity(), player);
    }

    private static Entity resolveGrabEntityTarget(Entity entity, LivingEntity holder) {
        if (entity == null) {
            return null;
        }
        Entity root = entity.getRootVehicle();
        if (root != entity && canGrabEntity(root, holder)) {
            return root;
        }
        return canGrabEntity(entity, holder) ? entity : null;
    }

    private static boolean canGrabEntity(Entity entity, LivingEntity holder) {
        if (entity == null || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity == holder || entity instanceof Player || entity instanceof ItemEntity) {
            return false;
        }
        if (entity.isPassenger()) {
            return false;
        }
        if (entity instanceof PrimedTnt) {
            return true;
        }
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return false;
        }
        if (entity instanceof LivingEntity living) {
            return !living.isSleeping();
        }
        return false;
    }

    private static Vec3 getTargetCenter(Entity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 computeHoldPos(ServerPlayer player, double holdDistanceOffset) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 forward = look.normalize();
        return player.getEyePosition().add(forward.scale(3.15D + holdDistanceOffset)).add(0.0D, -0.2D, 0.0D);
    }

    private static void holdEntityAt(Entity target, Vec3 center) {
        Vec3 currentCenter = getTargetCenter(target);
        Vec3 correction = center.subtract(currentCenter);
        if (correction.lengthSqr() > 16.0D) {
            target.teleportTo(center.x, center.y - target.getBbHeight() * 0.5D, center.z);
        } else {
            target.setDeltaMovement(correction.scale(0.45D));
        }
        target.fallDistance = 0.0F;
        target.hasImpulse = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static double clampHoldDistanceOffset(double value) {
        return Mth.clamp(value, MIN_HOLD_DISTANCE_OFFSET, MAX_HOLD_DISTANCE_OFFSET);
    }

    private static void clearGrab(ServerPlayer player) {
        ACTIVE_GRABS.remove(player.getUUID());
        ModNetwork.syncVenomGrabTentacle(player, null);
    }

    private static int getScore(ServerPlayer player, String objectiveName, boolean create) {
        Objective objective = getObjective(player, objectiveName, create);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static void setScore(ServerPlayer player, String objectiveName, int value) {
        Objective objective = getObjective(player, objectiveName, true);
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        score.setScore(value);
    }

    private static Objective getObjective(Player player, String objectiveName, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null && create) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }

    private record GrabState(int targetId, int age, double holdDistanceOffset) {
    }
}