package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomPlayerSizeHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarVenomSize");
    public static final String SIZE_OBJECTIVE = "Klyntar.VenomSize";
    private static final float VENOM_WIDTH = 0.6F;
    private static final float VENOM_HEIGHT = 4.0F;
    private static final float VENOM_EYE_HEIGHT = 3.55F;
    private static final float VENOM_SNEAKING_HEIGHT = 3.0F;
    private static final float VENOM_SNEAKING_EYE_HEIGHT = 2.62F;
    private static final float VANILLA_PLAYER_HEIGHT = 1.8F;
    private static final float VANILLA_PLAYER_EYE_HEIGHT = 1.62F;
    private static final float VENOM_CLIMB_HEIGHT = VANILLA_PLAYER_HEIGHT;
    private static final float VENOM_CLIMB_EYE_HEIGHT = VANILLA_PLAYER_EYE_HEIGHT;
    private static final int SIZE_GROW_COOLDOWN_TICKS = 10;
    private static final double SIZE_GROW_MARGIN_XZ = 0.04D;
    private static final double SIZE_GROW_MARGIN_Y = 0.08D;
    private static final Map<UUID, Integer> LAST_SIZE_STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SIZE_CHANGE_TICK = new ConcurrentHashMap<>();
    /**
     * Lo stato che il server ha comunicato, per id di entita'.
     *
     * <p>Klyntar.VenomSize e' un obiettivo fittizio e quelli non arrivano ai client: senza
     * questa mappa il client calcolerebbe sempre stato 0, terrebbe l'altezza vanilla e la
     * telecamera in prima persona resterebbe a 1.62 mentre il modello e' alto 4.</p>
     */
    private static final Map<Integer, Integer> STATO_DAL_SERVER = new ConcurrentHashMap<>();

    /** Chiamato dal pacchetto di sincronizzazione, solo lato client. */
    public static void aggiornaStatoDalServer(int idEntita, int stato) {
        if (stato <= 0) {
            STATO_DAL_SERVER.remove(idEntita);
        } else {
            STATO_DAL_SERVER.put(idEntita, stato);
        }
    }

    public static int statoDalServer(int idEntita) {
        return STATO_DAL_SERVER.getOrDefault(idEntita, 0);
    }

    private VenomPlayerSizeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }

        UUID playerId = player.getUUID();
        int previousState = LAST_SIZE_STATE.getOrDefault(playerId, 0);
        int currentState = getSizeState(player);

        if (currentState == 0 && getVenomSizeScore(player) <= 0) {
            LAST_SIZE_STATE.remove(playerId);
            LAST_SIZE_CHANGE_TICK.remove(playerId);
            if (previousState != 0) {
                player.refreshDimensions();
                annuncia(player, 0);
            }
            return;
        }

        LAST_SIZE_STATE.put(playerId, currentState);
        if (previousState != currentState) {
            LAST_SIZE_CHANGE_TICK.put(playerId, player.level().getGameTime());
            player.refreshDimensions();
            annuncia(player, currentState);
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        int sizeState;
        if (player.level() != null && player.level().isClientSide) {
            // il client non vede l'obiettivo fittizio: si fida di quello che gli e' stato detto
            sizeState = statoDalServer(player.getId());
            if (sizeState <= 0) {
                return;
            }
        } else {
            if (!isVenom(player)) {
                return;
            }
            sizeState = LAST_SIZE_STATE.getOrDefault(player.getUUID(), getSizeState(player));
        }
        float height = switch (sizeState) {
            case 1 -> VENOM_HEIGHT;
            case 2 -> VENOM_SNEAKING_HEIGHT;
            case 3 -> VENOM_CLIMB_HEIGHT;
            default -> VANILLA_PLAYER_HEIGHT;
        };
        float eyeHeight = switch (sizeState) {
            case 1 -> VENOM_EYE_HEIGHT;
            case 2 -> VENOM_SNEAKING_EYE_HEIGHT;
            case 3 -> VENOM_CLIMB_EYE_HEIGHT;
            default -> VANILLA_PLAYER_EYE_HEIGHT;
        };

        event.setNewSize(EntityDimensions.scalable(VENOM_WIDTH, height));
        event.setNewEyeHeight(eyeHeight);
    }

    /**
     * Al rientro nel mondo lo stato memorizzato non vale piu': il client e' nuovo e non sa
     * niente. Dimenticandolo, il primo tick lo ricalcola e lo riannuncia.
     */
    @SubscribeEvent
    public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        LAST_SIZE_STATE.remove(event.getEntity().getUUID());
        LAST_SIZE_CHANGE_TICK.remove(event.getEntity().getUUID());
    }

    /** Chi comincia a vedere un giocatore gia' trasformato deve saperne la taglia. */
    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Player bersaglio)
                || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer osservatore)) {
            return;
        }
        int stato = LAST_SIZE_STATE.getOrDefault(bersaglio.getUUID(), 0);
        if (stato > 0) {
            modKlyntar.network.ModNetwork.syncVenomSizeA(osservatore, bersaglio.getId(), stato);
        }
    }

    /** Manda la taglia a chi vede questo giocatore, se stesso compreso. */
    private static void annuncia(Player player, int stato) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            modKlyntar.network.ModNetwork.syncVenomSize(serverPlayer, stato);
        }
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

    private static int getSizeState(Player player) {
        if (getVenomSizeScore(player) <= 0) {
            return 0;
        }

        if (VenomSymbioteSystemsHandler.isClimbingForCollision(player)) {
            return 3;
        }

        if (player.isShiftKeyDown()) {
            return canUseDimensions(player, VENOM_WIDTH, VENOM_SNEAKING_HEIGHT, 0.0D, 0.0D) ? 2 : 0;
        }

        UUID playerId = player.getUUID();
        int previousState = LAST_SIZE_STATE.getOrDefault(playerId, 0);
        long gameTime = player.level() == null ? 0L : player.level().getGameTime();
        long lastChangeTick = LAST_SIZE_CHANGE_TICK.getOrDefault(playerId, Long.MIN_VALUE / 2L);
        boolean wasTall = previousState == 1;
        boolean canUseTall = canUseDimensions(
                player,
                VENOM_WIDTH,
                VENOM_HEIGHT,
                wasTall ? 0.0D : SIZE_GROW_MARGIN_XZ,
                wasTall ? 0.0D : SIZE_GROW_MARGIN_Y
        );

        if (wasTall && canUseTall) {
            return 1;
        }

        boolean canGrowAgain = player.onGround() && gameTime - lastChangeTick >= SIZE_GROW_COOLDOWN_TICKS;
        if (canUseTall && canGrowAgain) {
            return 1;
        }

        if (canUseDimensions(player, VENOM_WIDTH, VENOM_SNEAKING_HEIGHT, 0.0D, 0.0D)) {
            return 2;
        }

        return 0;
    }

    private static boolean canUseDimensions(Player player, float width, float height, double marginXZ, double marginY) {
        if (player.level() == null || player.isSpectator()) {
            return true;
        }

        double halfWidth = width / 2.0D;
        AABB targetBox = new AABB(
                player.getX() - halfWidth,
                player.getY(),
                player.getZ() - halfWidth,
                player.getX() + halfWidth,
                player.getY() + height + marginY,
                player.getZ() + halfWidth
        ).inflate(marginXZ, 0.0D, marginXZ).deflate(1.0E-6D);
        return player.level().noCollision(player, targetBox);
    }

    private static String stateName(int state) {
        return switch (state) {
            case 1 -> "tall";
            case 2 -> "compact";
            case 3 -> "climb";
            default -> "vanilla";
        };
    }
}
