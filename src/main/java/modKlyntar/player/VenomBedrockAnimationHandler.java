package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomBedrockAnimationHandler {
    private static final String[] PULSE_OBJECTIVES = {
            "Venom.Anim.Idle",
            "Venom.Anim.Walk",
            "Venom.Anim.Run",
            "Venom.Anim.Sneak",
            "Venom.Anim.WalkSneak",
            "Venom.Anim.Falling",
            "Venom.Anim.FallingAttack",
            "Venom.Anim.FallingAttackEnd",
            "Venom.Anim.Water",
            "Venom.Anim.Swim",
            "Venom.Anim.Elytra"
    };
    private static final String FALLING_ATTACK_REQUEST_OBJECTIVE = "Venom.FallingAttack.Request";
    private static final String CLIMB_WALL_OBJECTIVE = "Venom.Anim.ClimbWall";
    private static final String CLIMB_HANG_OBJECTIVE = "Venom.Anim.ClimbHang";
    private static final String CLIMB_IMPULSE_OBJECTIVE = "Venom.Anim.ClimbImpulse";
    private static final String CLIMB_CEILING_OBJECTIVE = "Venom.Anim.ClimbCeiling";
    private static final String CLIMB_CEILING_HOLD_OBJECTIVE = "Venom.Anim.ClimbCeilingHold";
    private static final int FALLING_ATTACK_END_TICKS = 3;
    private static final int SPRINT_GRACE_TICKS = 6;
    private static final double MOVE_THRESHOLD_SQR = 1.0E-4D;
    private static final Map<UUID, AnimationMemory> MEMORY = new ConcurrentHashMap<>();

    private VenomBedrockAnimationHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!isVenomActive(player)) {
            clearPulseScores(player);
            MEMORY.remove(player.getUUID());
            return;
        }

        AnimationMemory memory = MEMORY.computeIfAbsent(player.getUUID(), ignored -> new AnimationMemory(player.position()));
        if (consumeFallingAttackRequest(player) && !player.onGround() && !player.isInWaterOrBubble()) {
            memory.fallingAttackActive = true;
            memory.fallingAttackEndTicks = 0;
        }

        if (memory.fallingAttackActive) {
            if (player.onGround() || player.isInWaterOrBubble()) {
                memory.fallingAttackActive = false;
                memory.fallingAttackEndTicks = FALLING_ATTACK_END_TICKS;
            } else {
                switchObjective(player, memory, "Venom.Anim.FallingAttack");
                memory.lastPosition = player.position();
                return;
            }
        }

        if (memory.fallingAttackEndTicks > 0) {
            switchObjective(player, memory, "Venom.Anim.FallingAttackEnd");
            memory.fallingAttackEndTicks--;
            memory.lastPosition = player.position();
            return;
        }

        if (isClimbAnimationActive(player)) {
            clearPulseScores(player);
            memory.objective = null;
            memory.lastPosition = player.position();
            return;
        }

        AnimationState state = getAnimationState(player, memory);
        if (!memory.initialized) {
            memory.initialized = true;
            memory.state = state;
            clearPulseScores(player);
            switchObjective(player, memory, state.objectiveName);
            memory.lastPosition = player.position();
        } else if (memory.state != state || !state.objectiveName.equals(memory.objective)) {
            memory.state = state;
            switchObjective(player, memory, state.objectiveName);
        } else if (memory.objective != null) {
            setScore(player, memory.objective, 1);
        }
        memory.lastPosition = player.position();
    }

    private static AnimationState getAnimationState(ServerPlayer player, AnimationMemory memory) {
        boolean inWater = player.isInWaterOrBubble();
        boolean moving = isMoving(player, memory);
        boolean sprinting = player.isSprinting();

        if (!moving || player.isShiftKeyDown()) {
            memory.sprintGraceTicks = 0;
        } else if (sprinting) {
            memory.sprintGraceTicks = SPRINT_GRACE_TICKS;
        } else if (memory.sprintGraceTicks > 0) {
            memory.sprintGraceTicks--;
        }

        if (player.isFallFlying()) {
            return AnimationState.ELYTRA;
        }
        if (inWater && sprinting) {
            return AnimationState.SWIM;
        }
        if (inWater) {
            return AnimationState.WATER;
        }
        if (!player.onGround()) {
            return AnimationState.FALLING;
        }
        if (player.isShiftKeyDown() && moving) {
            return AnimationState.WALK_SNEAK;
        }
        if (player.isShiftKeyDown()) {
            return AnimationState.SNEAK;
        }
        if (sprinting || memory.sprintGraceTicks > 0) {
            return AnimationState.RUN;
        }
        if (moving) {
            return AnimationState.WALK;
        }
        return AnimationState.IDLE;
    }

    private static boolean isMoving(ServerPlayer player, AnimationMemory memory) {
        Vec3 movement = player.getDeltaMovement();
        double velocitySqr = movement.x * movement.x + movement.z * movement.z;
        Vec3 positionDelta = player.position().subtract(memory.lastPosition);
        double positionDeltaSqr = positionDelta.x * positionDelta.x + positionDelta.z * positionDelta.z;
        return velocitySqr > MOVE_THRESHOLD_SQR || positionDeltaSqr > MOVE_THRESHOLD_SQR;
    }

    private static boolean isVenomActive(ServerPlayer player) {
        Objective objective = player.getScoreboard().getObjective(VenomCameraHeightHandler.OBJECTIVE_NAME);
        if (objective == null) {
            return false;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore() > 0;
    }

    private static boolean consumeFallingAttackRequest(ServerPlayer player) {
        Objective objective = player.getScoreboard().getObjective(FALLING_ATTACK_REQUEST_OBJECTIVE);
        if (objective == null) {
            return false;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        boolean requested = score.getScore() > 0;
        if (requested) {
            score.setScore(0);
        }
        return requested;
    }

    private static boolean isClimbAnimationActive(ServerPlayer player) {
        return getScore(player, CLIMB_WALL_OBJECTIVE) > 0
                || getScore(player, CLIMB_HANG_OBJECTIVE) > 0
                || getScore(player, CLIMB_IMPULSE_OBJECTIVE) > 0
                || getScore(player, CLIMB_CEILING_OBJECTIVE) > 0
                || getScore(player, CLIMB_CEILING_HOLD_OBJECTIVE) > 0;
    }

    private static int getScore(Player player, String objectiveName) {
        Objective objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static void clearPulseScores(ServerPlayer player) {
        for (String objectiveName : PULSE_OBJECTIVES) {
            setScore(player, objectiveName, 0);
        }
    }

    private static void switchObjective(ServerPlayer player, AnimationMemory memory, String objectiveName) {
        if (memory.objective != null && !memory.objective.equals(objectiveName)) {
            setScore(player, memory.objective, 0);
        }
        memory.objective = objectiveName;
        setScore(player, objectiveName, 1);
    }

    private static void setScore(Player player, String objectiveName, int value) {
        Objective objective = getObjective(player, objectiveName);
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        score.setScore(value);
    }

    private static Objective getObjective(Player player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }

    private enum AnimationState {
        IDLE("Venom.Anim.Idle"),
        WALK("Venom.Anim.Walk"),
        RUN("Venom.Anim.Run"),
        SNEAK("Venom.Anim.Sneak"),
        WALK_SNEAK("Venom.Anim.WalkSneak"),
        FALLING("Venom.Anim.Falling"),
        WATER("Venom.Anim.Water"),
        SWIM("Venom.Anim.Swim"),
        ELYTRA("Venom.Anim.Elytra");

        private final String objectiveName;

        AnimationState(String objectiveName) {
            this.objectiveName = objectiveName;
        }
    }

    private static final class AnimationMemory {
        private AnimationState state = AnimationState.IDLE;
        private String objective;
        private boolean initialized;
        private Vec3 lastPosition;
        private boolean fallingAttackActive;
        private int fallingAttackEndTicks;
        private int sprintGraceTicks;

        private AnimationMemory(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}
