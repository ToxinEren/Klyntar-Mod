package modKlyntar.player;

import modKlyntar.MeteorCommand;
import modKlyntar.MyMod;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * La pioggia di meteore, una volta sola per giocatore e in un giorno imprevedibile.
 *
 * <p>Al primo ingresso nel mondo viene sorteggiato un giorno fra il decimo e il trentesimo; da
 * quel giorno in poi, la prima volta che il giocatore e' collegato, il server fa cadere le
 * meteore per conto suo e non ci riprova mai piu'.</p>
 *
 * <p>Il giorno sorteggiato e' contato a partire da quando il giocatore entra la prima volta, non
 * dall'inizio del mondo: chi si collega a una partita gia' avviata avrebbe altrimenti il
 * bersaglio gia' alle spalle e si vedrebbe arrivare le meteore all'istante. Su un mondo nuovo le
 * due cose coincidono.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class MeteorScheduleHandler {

    /** Il giorno sorteggiato, zero finche' non e' stato scelto. */
    public static final String OBIETTIVO_GIORNO = "Klyntar.MeteorDay";
    /** Uno quando le meteore sono gia' cadute per questo giocatore. */
    public static final String OBIETTIVO_FATTO = "Klyntar.MeteorDone";
    /**
     * Il giorno del mondo, tenuto aggiornato per poterlo guardare in gioco.
     *
     * <p>Si mostra a schermo con
     * {@code /scoreboard objectives setdisplay sidebar Klyntar.Day}.</p>
     */
    public static final String OBIETTIVO_GIORNO_CORRENTE = "Klyntar.Day";

    private static final int GIORNO_MINIMO = 10;
    private static final int GIORNO_MASSIMO = 30;
    private static final int TICK_PER_GIORNO = 24000;
    /** Ogni quanto controllare: al giorno bastano pochi controlli, non uno per tick. */
    private static final int INTERVALLO_CONTROLLO = 100;

    private static final Random SORTE = new Random();
    private static final Logger LOGGER = LogManager.getLogger("KlyntarMeteor");

    private MeteorScheduleHandler() {
    }

    /** Il giorno del mondo, contato come fa il comando /time query day. */
    private static long giornoCorrente(ServerPlayer giocatore) {
        return giocatore.serverLevel().getDayTime() / TICK_PER_GIORNO;
    }

    @SubscribeEvent
    public static void onIngresso(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer giocatore)) {
            return;
        }
        // gli obiettivi devono esistere davvero: uno mai scritto non e' zero, non c'e'
        SymbioteState.assicuraObiettivo(giocatore, OBIETTIVO_GIORNO);
        SymbioteState.assicuraObiettivo(giocatore, OBIETTIVO_FATTO);
        SymbioteState.assicuraObiettivo(giocatore, OBIETTIVO_GIORNO_CORRENTE);

        if (SymbioteState.getScore(giocatore, OBIETTIVO_GIORNO) > 0) {
            return;   // il sorteggio e' gia' stato fatto in un ingresso precedente
        }

        int attesa = GIORNO_MINIMO + SORTE.nextInt(GIORNO_MASSIMO - GIORNO_MINIMO + 1);
        long bersaglio = giornoCorrente(giocatore) + attesa;
        SymbioteState.setScore(giocatore, OBIETTIVO_GIORNO, (int) bersaglio);
        LOGGER.info("Meteore programmate per {} al giorno {} (fra {} giorni)",
                giocatore.getGameProfile().getName(), bersaglio, attesa);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer giocatore)
                || giocatore.tickCount % INTERVALLO_CONTROLLO != 0) {
            return;
        }
        // il contatore si aggiorna comunque, anche dopo che le meteore sono cadute
        long oggi = giornoCorrente(giocatore);
        if (SymbioteState.getScore(giocatore, OBIETTIVO_GIORNO_CORRENTE) != (int) oggi) {
            SymbioteState.setScore(giocatore, OBIETTIVO_GIORNO_CORRENTE, (int) oggi);
        }

        if (SymbioteState.getScore(giocatore, OBIETTIVO_FATTO) >= 1) {
            return;
        }
        int bersaglio = SymbioteState.getScore(giocatore, OBIETTIVO_GIORNO);
        if (bersaglio <= 0 || oggi < bersaglio) {
            return;
        }

        // segnato prima di lanciare: se qualcosa va storto nella caduta, non si riprova
        // all'infinito a ogni controllo
        SymbioteState.setScore(giocatore, OBIETTIVO_FATTO, 1);
        MeteorCommand.lanciaMeteore(giocatore);
        LOGGER.info("Meteore cadute su {} al giorno {}",
                giocatore.getGameProfile().getName(), bersaglio);
    }
}
