package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomPlayerSizeHandler {
    public static final String SIZE_OBJECTIVE = "Klyntar.VenomSize";
    private static final float VENOM_WIDTH = 0.6F;
    private static final float VENOM_HEIGHT = 4.0F;
    private static final float VENOM_EYE_HEIGHT = 3.55F;
    private static final float VENOM_SNEAKING_HEIGHT = 3.0F;
    private static final float VENOM_SNEAKING_EYE_HEIGHT = 2.62F;
    private static final Map<UUID, Integer> LAST_SIZE_STATE = new ConcurrentHashMap<>();

    private VenomPlayerSizeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        int currentState = getVenomSizeScore(player);
        if (currentState > 0 && player.isShiftKeyDown()) {
            currentState = 2;
        }

        Integer previousState = LAST_SIZE_STATE.put(player.getUUID(), currentState);
        if (previousState == null || previousState != currentState) {
            player.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player) || !isVenom(player)) {
            return;
        }

        boolean sneaking = player.isShiftKeyDown();
        event.setNewSize(EntityDimensions.scalable(VENOM_WIDTH, sneaking ? VENOM_SNEAKING_HEIGHT : VENOM_HEIGHT));
        event.setNewEyeHeight(sneaking ? VENOM_SNEAKING_EYE_HEIGHT : VENOM_EYE_HEIGHT);
    }

    public static boolean isVenom(Player player) {
        return getVenomSizeScore(player) > 0;
    }

    private static int getVenomSizeScore(Player player) {
        if (player.getGameProfile() == null || player.getGameProfile().getName() == null) {
            return 0;
        }

        var objective = player.getScoreboard().getObjective(SIZE_OBJECTIVE);
        return objective == null ? 0 : player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
    }
}
