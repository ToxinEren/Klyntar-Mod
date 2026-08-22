package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * L'affinita' fra giocatore e simbionte: cresce col tempo passato insieme.
 *
 * <p>Ogni simbionte tiene il proprio conto, in un obiettivo suo che non viene mai azzerato:
 * il legame con Venom non si perde indossando Carnage. Il valore della forma indossata viene
 * ricopiato in {@code Klyntar.Affinity}, che e' quello che leggono i Symbiote Bond.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteAffinityHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarAffinity");

    /** un giorno di Minecraft */
    private static final int TICK_PER_GIORNO = 24000;
    /** quanta affinita' vale un giorno passato col simbionte addosso */
    private static final int PUNTI_PER_GIORNO = 2;
    /** oltre questo non si sale: e' la soglia del terzo bond */
    private static final int MASSIMO = 100;
    /** quanto costa aver lasciato morire di fame il simbionte */
    public static final int PENALITA_BERSERK = 20;

    private SymbioteAffinityHandler() {
    }

    /** L'obiettivo che tiene il conto di una singola forma. */
    private static String obiettivo(String forma) {
        return "Klyntar.Affinity." + forma;
    }

    /** L'obiettivo che tiene i tick maturati verso il prossimo giorno. */
    private static String obiettivoTick(String forma) {
        return "Klyntar.Affinity.Ticks." + forma;
    }

    /** L'affinita' accumulata con una forma, anche se ora il giocatore ne indossa un'altra. */
    public static int affinitaDi(ServerPlayer giocatore, String forma) {
        return SymbioteState.getScore(giocatore, obiettivo(forma));
    }

    /**
     * Toglie affinita' alla forma indossata: il simbionte lasciato a digiuno se lo ricorda.
     * Non scende sotto zero.
     */
    public static void penalizza(ServerPlayer giocatore, int punti) {
        String forma = SymbioteState.forma(giocatore);
        if (forma.isEmpty()) {
            return;
        }
        int prima = affinitaDi(giocatore, forma);
        int dopo = Math.max(0, prima - punti);
        if (dopo == prima) {
            return;
        }
        SymbioteState.setScore(giocatore, obiettivo(forma), dopo);
        SymbioteState.setScore(giocatore, SymbioteState.AFFINITY_OBJECTIVE, dopo);
        LOGGER.info("Affinita' di {} con {}: da {} a {}",
                giocatore.getGameProfile().getName(), forma, prima, dopo);
        giocatore.displayClientMessage(
                Component.literal("Il simbionte si fida meno di te (" + dopo + ")"), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        String forma = SymbioteState.forma(giocatore);
        if (forma.isEmpty()) {
            return;
        }

        // gli obiettivi di questa forma devono esistere da subito: uno mai scritto non e'
        // zero, semplicemente non c'e', e nemmeno i comandi riescono a interrogarlo
        SymbioteState.assicuraObiettivo(giocatore, obiettivo(forma));
        SymbioteState.assicuraObiettivo(giocatore, obiettivoTick(forma));

        int affinita = SymbioteState.getScore(giocatore, obiettivo(forma));

        if (affinita < MASSIMO) {
            int maturati = SymbioteState.getScore(giocatore, obiettivoTick(forma)) + 1;
            if (maturati >= TICK_PER_GIORNO) {
                maturati = 0;
                affinita = Math.min(MASSIMO, affinita + PUNTI_PER_GIORNO);
                SymbioteState.setScore(giocatore, obiettivo(forma), affinita);
                LOGGER.info("Affinita' di {} con {} salita a {}",
                        giocatore.getGameProfile().getName(), forma, affinita);
                giocatore.displayClientMessage(
                        Component.literal("Il legame col simbionte si rafforza (" + affinita + ")"), true);
            }
            SymbioteState.setScore(giocatore, obiettivoTick(forma), maturati);
        }

        // i Symbiote Bond leggono un obiettivo solo: ci si specchia la forma indossata ora
        if (SymbioteState.getScore(giocatore, SymbioteState.AFFINITY_OBJECTIVE) != affinita) {
            SymbioteState.setScore(giocatore, SymbioteState.AFFINITY_OBJECTIVE, affinita);
        }
    }
}
