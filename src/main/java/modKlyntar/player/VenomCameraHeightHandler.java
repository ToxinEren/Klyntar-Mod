package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomCameraHeightHandler {
    public static final String OBJECTIVE_NAME = "Venom.CameraHeight";
    private static final float VANILLA_SCALE = 1.0F;
    private static final float VENOM_BODY_SCALE = 0.7F;
    private static final float VENOM_DEFAULT_EYE_HEIGHT_SCALE = 1.2F;
    private static final int SCALE_TICK_DELAY = 0;
    private static final Map<UUID, Boolean> LAST_VENOM_STATE = new ConcurrentHashMap<>();

    private VenomCameraHeightHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        boolean active = isVenomCameraActive(player);
        Boolean previous = LAST_VENOM_STATE.put(player.getUUID(), active);
        boolean stateChanged = previous == null || previous.booleanValue() != active;
        if (applyPehkuiScale(player, active, stateChanged)) {
            player.refreshDimensions();
        }
    }

    private static boolean applyPehkuiScale(ServerPlayer player, boolean active, boolean stateChanged) {
        float bodyScale = active ? VENOM_BODY_SCALE : VANILLA_SCALE;

        boolean changed = false;
        changed |= setScale(ScaleTypes.WIDTH, player, bodyScale);
        changed |= setScale(ScaleTypes.HEIGHT, player, bodyScale);
        changed |= setScale(ScaleTypes.MODEL_WIDTH, player, bodyScale);
        changed |= setScale(ScaleTypes.MODEL_HEIGHT, player, bodyScale);
        if (stateChanged) {
            changed |= setScale(ScaleTypes.EYE_HEIGHT, player, active ? VENOM_DEFAULT_EYE_HEIGHT_SCALE : VANILLA_SCALE);
        }
        return changed;
    }

    private static boolean setScale(ScaleType type, ServerPlayer player, float targetScale) {
        ScaleData scaleData = type.getScaleData(player);
        if (Math.abs(scaleData.getTargetScale() - targetScale) < 0.001F
                && Math.abs(scaleData.getScale() - targetScale) < 0.001F) {
            return false;
        }

        scaleData.setPersistence(false);
        scaleData.setScaleTickDelay(SCALE_TICK_DELAY);
        scaleData.setTargetScale(targetScale);
        scaleData.markForSync(true);
        return true;
    }

    private static boolean isVenomCameraActive(Player player) {
        Objective objective = getObjective(player, false);
        if (objective == null) {
            return false;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore() > 0;
    }

    private static Objective getObjective(Player player, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null && create) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal(OBJECTIVE_NAME), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
}
