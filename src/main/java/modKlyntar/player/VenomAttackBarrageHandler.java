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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rende il moveset di attack barrage attivabile anche col clic sinistro: ogni clic manda in scena
 * una sola animazione del moveset, e i clic successivi avanzano lungo la catena.
 *
 * <p>Il barrage tenuto a tasto premuto e' un {@code repeating_animation_timer} da 55 tick chiamato
 * {@code attack}, e ogni animazione del moveset si accende in una finestra di quel timer. Qui non
 * si duplicano le animazioni: si ricopia solo il timer su {@link #TIMER_OBJECTIVE}, che in
 * venom.json affianca le stesse finestre. Un clic fa scorrere il contatore lungo un singolo battito
 * e poi lo azzera, cosi' parte una animazione sola invece dell'intera raffica.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomAttackBarrageHandler {
    /** ricalca il timer dell'abilita' "attack": venom.json legge le stesse finestre da qui */
    public static final String TIMER_OBJECTIVE = "Venom.Barrage.Timer";
    /** quale animazione del moveset e' stata lanciata: 1..N, 0 quando la catena e' ferma */
    public static final String COMBO_OBJECTIVE = "Venom.Barrage.Combo";
    /** 1 finche' l'animazione in corso non e' finita: serve a zittire idle, walk e run */
    public static final String PLAYING_OBJECTIVE = "Venom.Barrage.Playing";

    private static final String COMBAT_TOOL_OBJECTIVE = "Venom.CombatTool";
    /** 1 quando il giocatore e' fermo a terra, aggiornato da VenomSymbioteSystemsHandler */
    private static final String STANDING_OBJECTIVE = "Venom.Standing";
    private static final String SHIELD_OBJECTIVE = "Venom.Anim.Shield";
    private static final String LOCOMOTION_OBJECTIVE = "Venom.Locomotion";
    private static final String FLIGHT_OBJECTIVE = "Venom.Flight";
    private static final String[] CLIMB_OBJECTIVES = {
            "Venom.Anim.ClimbWall",
            "Venom.Anim.ClimbHang",
            "Venom.Anim.ClimbImpulse",
            "Venom.Anim.ClimbCeiling",
            "Venom.Anim.ClimbCeilingHold"
    };

    /**
     * Estremi (inclusi) del timer di "attack" occupati da ogni battito del moveset, ricavati dalle
     * finestre delle abilita' in venom.json. Ogni battito copre una animazione sola e si ferma
     * prima che si apra la finestra della successiva, cosi' un clic non ne fa partire due.
     *
     * <p>Il set default e' quello che va tagliato piu' fine, perche' le sue finestre si
     * sovrappongono a coppie: righttalon 1..6 e righttalon2 6..10 condividono il tick 6, quindi il
     * primo battito si chiude a 5. La catena si ferma ai quattro artigli: giravolta e slam restano
     * solo sul barrage a tasto tenuto.</p>
     */
    private static final int[][] BEATS_DEFAULT = {
            {1, 5},    // righttalon        -> animation.venom.attack.1
            {7, 10},   // righttalon2       -> animation.venom.attack.2
            {11, 15},  // lefttalon         -> animation.venom.attack.3
            {17, 19}   // lefttalon2        -> animation.venom.attack.4
    };
    private static final int[][] BEATS_WHIP = {{1, 8}, {12, 20}, {24, 32}, {38, 48}};
    private static final int[][] BEATS_AXE = {{1, 18}, {25, 42}};
    // symbiontmaceattack1..4 restano spente per la guardia CombatTool == 30 in venom.json:
    // tenerle in tabella darebbe tre clic muti, quindi la mazza si ferma a venomcrush
    private static final int[][] BEATS_MACE = {{1, 5}};

    /**
     * Quanto dura davvero ogni battito, in tick. Non coincide con la finestra: la finestra accende
     * l'abilita', ma l'animazione Bedrock che ne parte ha una durata sua e piu' lunga
     * (animation.venom.attack.1 sono 15 tick contro i 5 della finestra). Finche' non e' finita il
     * contatore non avanza, altrimenti il controller fonde due animazioni insieme.
     *
     * <p>Dove non c'e' animazione Bedrock il valore e' la finestra piu' il rientro della posa
     * KubeJS, che scala di un tick alla volta fino a zero.</p>
     */
    private static final int[] LOCKS_DEFAULT = {
            15,  // animation.venom.attack.1
            11,  // animation.venom.attack.2
            20,  // animation.venom.attack.3.java_safe
            40   // animation.venom.attack.4.java_safe
    };
    private static final int[] LOCKS_WHIP = {15, 15, 15, 15};
    private static final int[] LOCKS_AXE = {20, 20};
    private static final int[] LOCKS_MACE = {20};

    /**
     * Battiti che pretendono il giocatore fermo. Il requisito vero sta in venom.json sull'abilita'
     * che li accende, qui serve solo a saltarli: senza, un clic in corsa farebbe avanzare il
     * contatore su una animazione che poi non parte.
     */
    private static final boolean[] STANDING_DEFAULT = {false, false, false, true};
    private static final boolean[] STANDING_WHIP = {false, false, false, false};
    private static final boolean[] STANDING_AXE = {false, false};
    private static final boolean[] STANDING_MACE = {false};

    /** dopo questa pausa la catena riparte dal primo colpo invece di proseguire */
    private static final int COMBO_RESET_TICKS = 12;

    private static final Map<UUID, ComboState> STATES = new ConcurrentHashMap<>();

    private VenomAttackBarrageHandler() {
    }

    /** invocata dal pacchetto di clic sinistro del client */
    public static void onAttackClick(ServerPlayer player) {
        if (!canBarrage(player)) {
            return;
        }

        ComboState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ComboState());
        if (state.lock > 0) {
            // l'animazione in corso non va interrotta: il clic resta in coda e il contatore
            // avanzera' appena finisce
            state.queued = true;
            return;
        }
        advance(player, state);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        ComboState state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!canBarrage(player)) {
            stop(player, state);
            return;
        }

        if (state.lock <= 0) {
            // catena ferma: dopo una pausa il contatore torna a zero e il prossimo clic riparte da capo
            if (state.combo != 0 && ++state.idleTicks > COMBO_RESET_TICKS) {
                state.combo = 0;
                state.idleTicks = 0;
                setScore(player, COMBO_OBJECTIVE, 0);
            }
            return;
        }

        // la finestra accende l'abilita' solo per i suoi tick, ma il blocco dura fino a fine animazione
        if (state.timer > 0) {
            setScore(player, TIMER_OBJECTIVE, state.timer);
            state.timer = state.timer >= state.beatEnd ? 0 : state.timer + 1;
        } else {
            setScore(player, TIMER_OBJECTIVE, 0);
        }

        if (--state.lock <= 0) {
            state.idleTicks = 0;
            if (state.queued) {
                state.queued = false;
                advance(player, state);
            } else {
                // animazione finita: idle, walk e run possono riprendere subito, senza
                // aspettare che il contatore si azzeri
                setScore(player, PLAYING_OBJECTIVE, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // venom.json interroga questi obiettivi a ogni tick: meglio che esistano fin da subito
            setScore(player, TIMER_OBJECTIVE, 0);
            setScore(player, COMBO_OBJECTIVE, 0);
            setScore(player, PLAYING_OBJECTIVE, 0);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            setScore(player, TIMER_OBJECTIVE, 0);
            setScore(player, COMBO_OBJECTIVE, 0);
            setScore(player, PLAYING_OBJECTIVE, 0);
            STATES.remove(player.getUUID());
        }
    }

    /** fa avanzare il contatore di uno e apre la finestra dell'animazione corrispondente */
    private static void advance(ServerPlayer player, ComboState state) {
        int[][] beats = beatsFor(player);
        int[] locks = locksFor(player);
        boolean[] needsStanding = standingFor(player);
        int index = state.combo % beats.length;   // combo e' 1-based, l'indice riparte da capo a fine catena

        boolean standing = getScore(player, STANDING_OBJECTIVE) > 0;
        for (int skipped = 0; skipped < beats.length && needsStanding[index] && !standing; skipped++) {
            index = (index + 1) % beats.length;
        }

        state.combo = index + 1;
        state.timer = beats[index][0];
        state.beatEnd = beats[index][1];
        state.lock = locks[index];
        state.idleTicks = 0;
        setScore(player, COMBO_OBJECTIVE, state.combo);
        setScore(player, PLAYING_OBJECTIVE, 1);
        setScore(player, TIMER_OBJECTIVE, state.timer);
    }

    private static void stop(ServerPlayer player, ComboState state) {
        state.timer = 0;
        state.lock = 0;
        state.combo = 0;
        state.queued = false;
        state.idleTicks = 0;
        setScore(player, TIMER_OBJECTIVE, 0);
        setScore(player, COMBO_OBJECTIVE, 0);
        setScore(player, PLAYING_OBJECTIVE, 0);
    }

    /**
     * Arma da cui prendere le tabelle. Con la mazza in movimento si ricade sul set default: la sua
     * unica animazione, venomcrush, pianta il personaggio e in corsa non regge.
     */
    private static int effectiveTool(ServerPlayer player) {
        int tool = getScore(player, COMBAT_TOOL_OBJECTIVE);
        if (tool == 3 && getScore(player, STANDING_OBJECTIVE) <= 0) {
            return 0;
        }
        return tool;
    }

    /** ogni arma simbiotica ha le sue finestre lungo lo stesso timer da 55 tick */
    private static int[][] beatsFor(ServerPlayer player) {
        return switch (effectiveTool(player)) {
            case 1 -> BEATS_WHIP;
            case 2 -> BEATS_AXE;
            case 3 -> BEATS_MACE;
            default -> BEATS_DEFAULT;
        };
    }

    private static int[] locksFor(ServerPlayer player) {
        return switch (effectiveTool(player)) {
            case 1 -> LOCKS_WHIP;
            case 2 -> LOCKS_AXE;
            case 3 -> LOCKS_MACE;
            default -> LOCKS_DEFAULT;
        };
    }

    private static boolean[] standingFor(ServerPlayer player) {
        return switch (effectiveTool(player)) {
            case 1 -> STANDING_WHIP;
            case 2 -> STANDING_AXE;
            case 3 -> STANDING_MACE;
            default -> STANDING_DEFAULT;
        };
    }

    private static boolean canBarrage(ServerPlayer player) {
        if (getScore(player, VenomCameraHeightHandler.OBJECTIVE_NAME) <= 0) {
            return false;
        }
        if (player.isShiftKeyDown()) {
            // stessa esclusione dell'abilita' "attack", che non si sblocca da accovacciati
            return false;
        }
        if (getScore(player, SHIELD_OBJECTIVE) > 0
                || getScore(player, LOCOMOTION_OBJECTIVE) > 0
                || getScore(player, FLIGHT_OBJECTIVE) > 0) {
            return false;
        }
        for (String objectiveName : CLIMB_OBJECTIVES) {
            if (getScore(player, objectiveName) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int getScore(Player player, String objectiveName) {
        Objective objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        return player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
    }

    private static void setScore(Player player, String objectiveName, int value) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(value);
    }

    private static final class ComboState {
        /** valore corrente della finestra letta da venom.json, 0 quando la finestra e' chiusa */
        private int timer;
        private int beatEnd;
        /** indice dell'animazione lanciata, 1-based: e' il contatore rispecchiato sullo scoreboard */
        private int combo;
        /** tick che mancano alla fine dell'animazione in corso: finche' e' > 0 il contatore non avanza */
        private int lock;
        private int idleTicks;
        private boolean queued;
    }
}
