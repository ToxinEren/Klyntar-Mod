package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convivenza con le mod ragno: mentre il corpo del simbionte e' fuori, la loro arrampicata
 * sta da parte.
 *
 * <p>Chi porta Venom insieme a un potere ragno di un'altra mod - il caso di Powerborne, che
 * la scorciatoia a venomspidey legge ma non toglie - si ritrova due arrampicate sullo stesso
 * corpo. Quella di Powerborne e' uno script che a ogni tick cerca un soffitto, ci si attacca
 * e da li' in poi impone lui il movimento: il nostro ceiling hold scrive lo stesso movimento
 * ma il loro arriva dopo e lo sovrascrive. Sul muro invece va bene, perche' il loro script si
 * ritira quando {@code onClimbable()} e' vero, e il nostro mixin lo rende vero solo li'.</p>
 *
 * <p>Powerborne espone un interruttore per il suo wall crawling, la proprieta' Palladium
 * {@code toggle_wall_crawling}, la stessa che il giocatore trova nella sua schermata dei
 * poteri. Qui la si spegne quando il corpo esce e la si riaccende quando rientra. Ci si
 * ricorda di averlo fatto noi, cosi' un interruttore spento a mano dal giocatore resta
 * spento: si riaccende solo quello che si e' spento.</p>
 *
 * <p>Compatibilita' morbida: la proprieta' si cerca per nome e, se non c'e' - Powerborne non
 * installato - non si fa niente. Nessun riferimento diretto all'altra mod.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SpiderCoexistenceHandler {

    /** L'interruttore del wall crawling di Powerborne, per nome. */
    private static final String INTERRUTTORE = "toggle_wall_crawling";
    /** Ricorda che l'interruttore l'abbiamo spento noi, e va riacceso. */
    private static final String CHIAVE_SPENTO_DA_NOI = "klyntar.wallcrawl_spento";
    /** Ogni quanto controllare: e' uno stato che cambia di rado. */
    private static final int INTERVALLO = 10;

    private static final Logger LOGGER = LogManager.getLogger("KlyntarConvivenza");

    private SpiderCoexistenceHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer giocatore)
                || giocatore.tickCount % INTERVALLO != 0) {
            return;
        }

        boolean corpoFuori = SymbioteMiningHandler.corpoAttivo(giocatore);
        boolean spentoDaNoi = giocatore.getPersistentData().getBoolean(CHIAVE_SPENTO_DA_NOI);
        if (corpoFuori == spentoDaNoi) {
            return;   // gia' nello stato giusto: niente da fare
        }

        EntityPropertyHandler.getHandler(giocatore).ifPresent(proprieta -> {
            PalladiumProperty<?> interruttore = proprieta.getPropertyByName(INTERRUTTORE);
            if (interruttore == null || !proprieta.isRegistered(interruttore)) {
                return;   // nessuna mod ragno con questo interruttore: non c'e' niente da conciliare
            }

            if (corpoFuori) {
                // si spegne solo se e' acceso: se il giocatore l'ha gia' spento a mano, non e'
                // affar nostro e non lo riaccenderemo noi
                if (acceso(proprieta.get(interruttore))) {
                    proprieta.setRaw(interruttore, 0);
                    giocatore.getPersistentData().putBoolean(CHIAVE_SPENTO_DA_NOI, true);
                    LOGGER.debug("Wall crawling of {} switched off: the symbiote is out",
                            giocatore.getGameProfile().getName());
                }
            } else {
                proprieta.setRaw(interruttore, 1);
                giocatore.getPersistentData().remove(CHIAVE_SPENTO_DA_NOI);
                LOGGER.debug("Wall crawling of {} switched back on: the symbiote is back inside",
                        giocatore.getGameProfile().getName());
            }
        });
    }

    private static boolean acceso(Object valore) {
        return valore instanceof Number numero && numero.intValue() != 0;
    }
}
